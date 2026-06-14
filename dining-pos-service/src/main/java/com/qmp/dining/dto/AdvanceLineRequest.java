package com.qmp.dining.dto;

import lombok.Data;

/**
 * KDS 推进点单项状态请求。
 */
@Data
public class AdvanceLineRequest {

    /** COOKING/READY/SERVED。 */
    private String target;
}
