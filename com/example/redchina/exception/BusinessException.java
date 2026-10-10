package com.example.redchina.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义业务异常（用于业务逻辑校验失败场景）
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BusinessException extends RuntimeException {
    private String msg;
    private int code = 500;

    public BusinessException(String msg) {
        super(msg);
        this.msg = msg;
    }

    public BusinessException(String msg, int code) {
        super(msg);
        this.msg = msg;
        this.code = code;
    }
}
