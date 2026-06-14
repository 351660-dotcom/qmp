package com.qmp.verification.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 反序列化其他服务返回的统一响应体（09 文档 1.1 {@code {code, message, data, trace_id}}）。
 * 仅取本服务关心的 {@code code}/{@code data} 字段。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResult<T> {

    private String code;
    private T data;
}
