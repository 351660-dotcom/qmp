package com.qmp.performance.dto;

import lombok.Data;

/**
 * 预订单发起支付请求。
 */
@Data
public class PayRequest {

    /** WECHAT/ALIPAY。 */
    private String channel;
}
