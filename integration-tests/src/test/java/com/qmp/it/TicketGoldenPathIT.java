package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 门票黄金路径端到端集成测试（ADR-010 / 09 文档八）。
 *
 * <p>黑盒驱动已通过 {@code docker compose up} 启动的整套服务，覆盖：
 * 创建订单 → 预占库存 → 发起支付 → 支付成功回调 → 确认预占 + 出票 → 核验 → 申请退票 → 释放库存。</p>
 *
 * <p>订单含两条明细（同 sku、不同场次、各 1 张），分别走「核验」与「退票」两条分支：
 * itemA 核验成功（库存保持已售），itemB 申请退票（退款成功后释放库存）。</p>
 *
 * <p>默认跳过；CI 在 compose 启动并就绪后设置 {@code RUN_GOLDEN_PATH=true} 触发。
 * 服务地址 / MySQL 地址可经环境变量覆盖，默认指向 localhost 暴露端口。</p>
 *
 * <p><b>时效性说明</b>：种子游玩日期为 {@code 2026-07-01}，默认退改规则 cutoff 24h，
 * 故退票步骤要求运行时间早于 2026-06-30。详见 integration-tests 说明与各服务 CLAUDE.md。</p>
 */
@EnabledIfEnvironmentVariable(named = "RUN_GOLDEN_PATH", matches = "true")
@DisplayName("门票黄金路径：下单→预占→支付→出票→核验→退票→释放库存")
class TicketGoldenPathIT {

    private static final String TENANT_ID = "1001";
    private static final long SKU_ID = 1001L;
    private static final String SALE_DATE = "2026-07-01";

    private static final String ORDER_BASE = env("ORDER_BASE_URL", "http://localhost:8087");
    private static final String PAYMENT_BASE = env("PAYMENT_BASE_URL", "http://localhost:8085");
    private static final String INVENTORY_BASE = env("INVENTORY_BASE_URL", "http://localhost:8084");
    private static final String TICKET_BASE = env("TICKET_VERIFICATION_BASE_URL", "http://localhost:8086");

    private static final String MYSQL_HOST = env("MYSQL_HOST", "localhost");
    private static final String MYSQL_PORT = env("MYSQL_PORT", "3306");
    private static final String MYSQL_USER = env("MYSQL_USERNAME", "root");
    private static final String MYSQL_PASSWORD = env("MYSQL_PASSWORD", "root");

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    @Test
    void goldenPath() throws Exception {
        // 0) 等待 order-service 就绪（compose 健康检查后仍需等待应用 + Flyway 完成）
        awaitUntil(Duration.ofSeconds(180), () -> serviceUp(ORDER_BASE + "/api/v1/orders/0"));

        // 1) 创建订单：两条明细（场次 1 / 场次 2，各 1 张），会员价 88 × 2 = 176
        String createBody = """
                {
                  "user_id": 123,
                  "items": [
                    {"sku_id": %d, "sale_date": "%s", "time_slot_id": 1, "quantity": 1},
                    {"sku_id": %d, "sale_date": "%s", "time_slot_id": 2, "quantity": 1}
                  ]
                }
                """.formatted(SKU_ID, SALE_DATE, SKU_ID, SALE_DATE);
        JsonNode created = postJson(ORDER_BASE + "/api/v1/orders", createBody);
        long orderId = created.get("order_id").asLong();
        assertThat(created.get("status").asText()).isEqualTo("PENDING_PAYMENT");
        assertThat(created.get("total_amount").asDouble()).isEqualTo(176.00);
        assertThat(created.get("items")).hasSize(2);
        String orderItemA = created.get("items").get(0).get("order_item_id").asText();
        String orderItemB = created.get("items").get(1).get("order_item_id").asText();

        // 2) 预占已生效：两个明细对应预占均为 HOLDING
        assertThat(reservationStatus(orderItemA)).isEqualTo("HOLDING");
        assertThat(reservationStatus(orderItemB)).isEqualTo("HOLDING");

        // 3) 发起支付，拿到 payment_id
        JsonNode pay = postJson(ORDER_BASE + "/api/v1/orders/" + orderId + "/pay", "{\"channel\":\"WECHAT\"}");
        String paymentId = pay.get("payment_id").asText();
        assertThat(paymentId).isNotBlank();

        // 4) 模拟支付渠道回调 → 发 PaymentSucceeded → order 确认预占 + 出票 + 置 PAID
        postJson(PAYMENT_BASE + "/internal/callbacks/mock",
                "{\"payment_id\":\"%s\",\"channel_trade_no\":\"IT-TRADE-1\"}".formatted(paymentId));

        // 5) 等待订单异步置为 PAID（确认预占 + 出票完成）
        awaitUntil(Duration.ofSeconds(60), () ->
                "PAID".equals(orderStatus(orderId)));
        assertThat(reservationStatus(orderItemA)).isEqualTo("CONFIRMED");
        assertThat(reservationStatus(orderItemB)).isEqualTo("CONFIRMED");

        // 出票后每个明细应有 1 张凭证
        Credential credA = awaitCredential(orderItemA);
        Credential credB = awaitCredential(orderItemB);
        assertThat(credA.status).isEqualTo("UNUSED");
        assertThat(credB.status).isEqualTo("UNUSED");

        // 6) 核验 itemA 的凭证 → SUCCESS
        JsonNode verify = postJson(TICKET_BASE + "/api/v1/credentials/verify",
                "{\"verify_code\":\"%s\",\"terminal_id\":\"MINIAPP-SCAN-01\"}".formatted(credA.verifyCode));
        assertThat(verify.get("result").asText()).isEqualTo("SUCCESS");
        assertThat(verify.get("credential_id").asLong()).isEqualTo(credA.credentialId);
        assertThat(credentialStatus(credA.credentialId)).isEqualTo("VERIFIED");

        // 7) 申请退票 itemB 的凭证 → 同步调用 payment 退款 → 返回 refund_id
        JsonNode refund = postJson(TICKET_BASE + "/api/v1/credentials/" + credB.credentialId + "/refund-request", null);
        assertThat(refund.get("refund_id").asLong()).isPositive();

        // 8) 等待 RefundSucceeded 被核验服务消费：凭证 REFUNDED + 库存预占 RELEASED
        awaitUntil(Duration.ofSeconds(60), () ->
                "REFUNDED".equals(credentialStatus(credB.credentialId))
                        && "RELEASED".equals(reservationStatus(orderItemB)));

        // 9) 终态断言
        assertThat(credentialStatus(credA.credentialId)).isEqualTo("VERIFIED");
        assertThat(credentialStatus(credB.credentialId)).isEqualTo("REFUNDED");
        assertThat(reservationStatus(orderItemA)).isEqualTo("CONFIRMED"); // 已核验，库存保持已售
        assertThat(reservationStatus(orderItemB)).isEqualTo("RELEASED");  // 已退票，库存已释放

        // 库存账本：itemB 退票释放后该场次余量回补
        JsonNode availB = get(INVENTORY_BASE + "/api/v1/inventory/availability?sku_id="
                + SKU_ID + "&sale_date=" + SALE_DATE + "&time_slot_id=2");
        assertThat(availB.get("sold_count").asInt()).isEqualTo(0);
        assertThat(availB.get("locked_count").asInt()).isEqualTo(0);

        // 订单：itemA 已核验 / itemB 已退票（非核验），未达「全部核验」，保持 PAID（见 order-service CLAUDE.md 决策 5）
        assertThat(orderStatus(orderId)).isEqualTo("PAID");
    }

    // ------------------------------------------------------------------
    // HTTP 辅助
    // ------------------------------------------------------------------
    private JsonNode postJson(String url, String body) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("X-Tenant-Id", TENANT_ID)
                .header("Content-Type", "application/json");
        b = (body == null)
                ? b.POST(HttpRequest.BodyPublishers.noBody())
                : b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("POST %s -> %s", url, resp.body()).isEqualTo(200);
        JsonNode root = om.readTree(resp.body());
        assertThat(root.get("code").asText()).as("POST %s code", url).isEqualTo("OK");
        return root.get("data");
    }

    private JsonNode get(String url) throws Exception {
        HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                .header("X-Tenant-Id", TENANT_ID).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("GET %s -> %s", url, resp.body()).isEqualTo(200);
        return om.readTree(resp.body()).get("data");
    }

    private boolean serviceUp(String url) {
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("X-Tenant-Id", TENANT_ID).GET().build(), HttpResponse.BodyHandlers.ofString());
            // 任意 HTTP 应答（含 404）都说明应用已起来
            return resp.statusCode() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String orderStatus(long orderId) {
        try {
            return get(ORDER_BASE + "/api/v1/orders/" + orderId).get("status").asText();
        } catch (Exception e) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // JDBC 辅助（黑盒读取下游库验证终态；测试专用，不经业务接口）
    // ------------------------------------------------------------------
    private record Credential(long credentialId, String verifyCode, String status) {
    }

    private Credential awaitCredential(String orderItemId) {
        final Credential[] holder = new Credential[1];
        awaitUntil(Duration.ofSeconds(60), () -> {
            Credential c = queryCredential(orderItemId);
            holder[0] = c;
            return c != null;
        });
        return holder[0];
    }

    private Credential queryCredential(String orderItemId) {
        String sql = "SELECT credential_id, verify_code, status FROM ticket_verification_db.ticket_credential "
                + "WHERE order_item_id = ?";
        try (Connection conn = conn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Credential(rs.getLong(1), rs.getString(2), rs.getString(3));
                }
                return null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String credentialStatus(long credentialId) {
        return scalar("SELECT status FROM ticket_verification_db.ticket_credential WHERE credential_id = "
                + credentialId);
    }

    private String reservationStatus(String orderItemId) {
        String sql = "SELECT status FROM inventory_db.inventory_reservation WHERE reservation_id = ?";
        try (Connection conn = conn(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, orderItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String scalar(String sql) {
        try (Connection conn = conn(); PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Connection conn() throws Exception {
        String url = "jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        return DriverManager.getConnection(url, MYSQL_USER, MYSQL_PASSWORD);
    }

    // ------------------------------------------------------------------
    // 轮询
    // ------------------------------------------------------------------
    private void awaitUntil(Duration timeout, BooleanSupplier condition) {
        long deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException last = null;
        while (System.nanoTime() < deadline) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (RuntimeException e) {
                last = e;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
        throw new AssertionError("条件在 " + timeout.toSeconds() + "s 内未满足", last);
    }

    private static String env(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }
}
