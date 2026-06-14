package com.qmp.dining.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 结账请求（12 文档四）。
 */
@Data
public class CheckoutRequest {

    /** 是否使用会员储值抵扣（需台账已关联 member_id 且余额足额，否则整笔储值抵扣为 0）。 */
    @JsonProperty("use_wallet")
    private Boolean useWallet;

    /** 剩余应付的聚合支付渠道（CASH/POS/WECHAT/ALIPAY），v1 当面收讫记账。 */
    private String channel;
}
