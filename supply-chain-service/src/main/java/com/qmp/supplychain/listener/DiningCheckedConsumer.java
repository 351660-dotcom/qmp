package com.qmp.supplychain.listener;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qmp.kernel.context.TenantContext;
import com.qmp.kernel.event.EventEnvelope;
import com.qmp.supplychain.event.DiningCheckedPayload;
import com.qmp.supplychain.entity.Warehouse;
import com.qmp.supplychain.service.BomService;
import com.qmp.supplychain.service.StockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 消费 {@code DiningChecked}（topic={@code dining_dining-checked}）：按 BOM 核减门店库存（12 文档 6.3）。
 *
 * <p>幂等：{@code processed_event (consumer_group, event_id)}。门店仓由 merchant_id 定位；
 * 未配置仓/BOM/库存仅记录异常（待库管补录后重放），不抛出以免无意义重投。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = "dining_dining-checked",
        consumerGroup = DiningCheckedConsumer.CONSUMER_GROUP,
        consumeMode = ConsumeMode.ORDERLY)
public class DiningCheckedConsumer implements RocketMQListener<String> {

    public static final String CONSUMER_GROUP = "supply-chain-dining-checked-consumer";

    private final ObjectMapper objectMapper;
    private final EventDedup eventDedup;
    private final StockService stockService;
    private final BomService bomService;

    @Override
    public void onMessage(String message) {
        EventEnvelope<DiningCheckedPayload> envelope = parse(message);
        if (eventDedup.isProcessed(CONSUMER_GROUP, envelope.getEventId())) {
            return;
        }
        TenantContext.set(envelope.getTenantId());
        try {
            DiningCheckedPayload payload = envelope.getPayload();
            Warehouse storeWarehouse = stockService.findStoreWarehouse(payload.getMerchantId());
            if (storeWarehouse == null) {
                log.warn("未找到门店仓，跳过 BOM 核减: merchantId={}", payload.getMerchantId());
            } else if (payload.getLines() != null) {
                for (DiningCheckedPayload.Line line : payload.getLines()) {
                    bomService.deductForDish(storeWarehouse.getWarehouseId(), line.getSkuId(),
                            line.getQuantity() != null ? line.getQuantity() : 0);
                }
            }
            eventDedup.markProcessed(CONSUMER_GROUP, envelope.getEventId(), envelope.getEventType());
            log.info("DiningChecked 处理完成: tableOrderId={}", payload.getTableOrderId());
        } finally {
            TenantContext.clear();
        }
    }

    private EventEnvelope<DiningCheckedPayload> parse(String message) {
        try {
            return objectMapper.readValue(message, new TypeReference<EventEnvelope<DiningCheckedPayload>>() {
            });
        } catch (Exception e) {
            throw new IllegalStateException("DiningChecked 事件解析失败: " + message, e);
        }
    }
}
