package com.qmp.dining.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * 反序列化下游服务统一响应体（09 文档 1.1）。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResult<T> {

    private String code;
    private String message;
    private T data;
}
