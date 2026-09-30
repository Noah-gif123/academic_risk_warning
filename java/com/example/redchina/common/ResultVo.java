package com.example.redchina.common;

import lombok.Data;

/**
 * 全局统一返回结果
 */
@Data
public class ResultVo<T> {
    private int code;       // 响应码（200成功，500失败）
    private String msg;     // 响应信息
    private T data;         // 响应数据

    // 带参数的私有构造函数
    private ResultVo(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    // 成功响应（无数据）
    public static <T> ResultVo<T> success() {
        return new ResultVo<>(200, "操作成功", null);
    }

    // 成功响应（带数据）
    public static <T> ResultVo<T> success(T data) {
        return new ResultVo<>(200, "操作成功", data);
    }

    // 新增：成功响应（带数据+自定义消息）
    public static <T> ResultVo<T> success(T data, String msg) {
        return new ResultVo<>(200, msg, data);
    }

    // 失败响应
    public static <T> ResultVo<T> error(String msg) {
        return new ResultVo<>(500, msg, null);
    }

    // 自定义响应
    public static <T> ResultVo<T> build(int code, String msg, T data) {
        return new ResultVo<>(code, msg, data);
    }
}