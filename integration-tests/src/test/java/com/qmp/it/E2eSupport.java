package com.qmp.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 各业态端到端测试公用支撑：统一 HTTP（带租户头 + ApiResponse 断言）、JDBC 直读、轮询、造数 ID。
 * 服务地址 / MySQL 地址可经环境变量覆盖，默认指向 docker-compose 暴露端口。
 */
abstract class E2eSupport {

    protected static final String TENANT_ID = "1001";
    protected static final long SCENIC_ID = 3001L;
    protected static final long MERCHANT_ID = 2001L;
    /** 后台鉴权令牌（与 docker-compose ADMIN_API_TOKEN 一致）；服务未启用鉴权时该头被忽略。 */
    protected static final String ADMIN_TOKEN = env("ADMIN_API_TOKEN", "scenic-admin-dev-token");

    protected static final String PRODUCT_BASE = env("PRODUCT_BASE_URL", "http://localhost:8081");
    protected static final String PRICING_BASE = env("PRICING_BASE_URL", "http://localhost:8082");
    protected static final String MEMBER_BASE = env("MEMBER_BASE_URL", "http://localhost:8083");
    protected static final String INVENTORY_BASE = env("INVENTORY_BASE_URL", "http://localhost:8084");
    protected static final String PAYMENT_BASE = env("PAYMENT_BASE_URL", "http://localhost:8085");
    protected static final String TICKET_BASE = env("TICKET_VERIFICATION_BASE_URL", "http://localhost:8086");
    protected static final String ORDER_BASE = env("ORDER_BASE_URL", "http://localhost:8087");
    protected static final String HOTEL_BASE = env("HOTEL_BASE_URL", "http://localhost:8088");
    protected static final String MARKETING_BASE = env("MARKETING_BASE_URL", "http://localhost:8089");
    protected static final String DINING_BASE = env("DINING_BASE_URL", "http://localhost:8090");
    protected static final String SUPPLY_BASE = env("SUPPLY_BASE_URL", "http://localhost:8091");
    protected static final String PERFORMANCE_BASE = env("PERFORMANCE_BASE_URL", "http://localhost:8092");

    private static final String MYSQL_HOST = env("MYSQL_HOST", "localhost");
    private static final String MYSQL_PORT = env("MYSQL_PORT", "3306");
    private static final String MYSQL_USER = env("MYSQL_USERNAME", "root");
    private static final String MYSQL_PASSWORD = env("MYSQL_PASSWORD", "root");

    protected final HttpClient http = HttpClient.newHttpClient();
    protected final ObjectMapper om = new ObjectMapper();

    // ---- HTTP（返回 data 节点；断言 200 + code=OK）----
    protected JsonNode post(String url, String body) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .header("X-Tenant-Id", TENANT_ID)
                    .header("X-Admin-Token", ADMIN_TOKEN)
                    .header("Content-Type", "application/json");
            b = (body == null) ? b.POST(HttpRequest.BodyPublishers.noBody())
                    : b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            HttpResponse<String> resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).as("POST %s -> %s", url, resp.body()).isEqualTo(200);
            JsonNode root = om.readTree(resp.body());
            assertThat(root.get("code").asText()).as("POST %s code", url).isEqualTo("OK");
            return root.get("data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonNode put(String url, String body) {
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("X-Tenant-Id", TENANT_ID).header("X-Admin-Token", ADMIN_TOKEN)
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).as("PUT %s -> %s", url, resp.body()).isEqualTo(200);
            return om.readTree(resp.body()).get("data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 原始 POST（自定义租户，不断言状态），用于校验错误码 / 隔离租户的用例。 */
    protected HttpResponse<String> rawPost(String url, String body, String tenant) {
        try {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                    .header("X-Tenant-Id", tenant)
                    .header("X-Admin-Token", ADMIN_TOKEN)
                    .header("Content-Type", "application/json");
            b = (body == null) ? b.POST(HttpRequest.BodyPublishers.noBody())
                    : b.POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
            return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonNode dataOf(HttpResponse<String> resp) {
        try {
            return om.readTree(resp.body()).get("data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected JsonNode get(String url) {
        try {
            HttpResponse<String> resp = http.send(HttpRequest.newBuilder(URI.create(url))
                    .header("X-Tenant-Id", TENANT_ID).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(resp.statusCode()).as("GET %s -> %s", url, resp.body()).isEqualTo(200);
            return om.readTree(resp.body()).get("data");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---- JDBC ----
    protected String scalar(String sql) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** 执行 UPDATE/DELETE 等更新语句（测试造数/构造边界态用，不经业务接口），返回影响行数。 */
    protected int execute(String sql) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            return ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Connection conn() throws Exception {
        return DriverManager.getConnection("jdbc:mysql://" + MYSQL_HOST + ":" + MYSQL_PORT
                + "/?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", MYSQL_USER, MYSQL_PASSWORD);
    }

    // ---- 轮询 ----
    protected void awaitUntil(Duration timeout, BooleanSupplier cond) {
        long deadline = System.nanoTime() + timeout.toNanos();
        RuntimeException last = null;
        while (System.nanoTime() < deadline) {
            try {
                if (cond.getAsBoolean()) {
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

    protected void awaitServiceUp(String url) {
        awaitUntil(Duration.ofSeconds(180), () -> {
            try {
                HttpResponse<String> r = http.send(HttpRequest.newBuilder(URI.create(url))
                        .header("X-Tenant-Id", TENANT_ID).GET().build(), HttpResponse.BodyHandlers.ofString());
                return r.statusCode() > 0;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /** 每次运行生成唯一 ID，避免重复跑测试时唯一键冲突（sku_id/seat 等）。 */
    protected long uniqueId() {
        return ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_000_000_000L);
    }

    protected static String env(String name, String def) {
        String v = System.getenv(name);
        return (v == null || v.isBlank()) ? def : v;
    }
}
