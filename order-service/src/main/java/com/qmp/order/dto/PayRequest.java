package com.qmp.order.dto;

import lombok.Data;

/**
 * 发起支付请求（09 文档八 {@code POST /api/v1/orders/{order_id}/pay}）。
 */
@Data
public class PayRequest {

    /** WECHAT/ALIPAY。 */
    private String channel;
}
