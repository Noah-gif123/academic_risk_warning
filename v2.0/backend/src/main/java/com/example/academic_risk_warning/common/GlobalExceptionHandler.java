package com.example.academic_risk_warning.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

/** 全局异常处理：参数校验、业务异常、兜底异常 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e) {
        HttpStatus status = e.getCode() == 401 ? HttpStatus.UNAUTHORIZED : HttpStatus.OK;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe == null ? "参数校验失败" : fe.getField() + " " + fe.getDefaultMessage();
        return Result.fail(400, msg);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(400, "参数格式不正确：" + e.getName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.fail(400, e.getMessage() == null ? "参数不合法" : e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Result.fail(404, "资源不存在：" + e.getResourcePath()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(Result.fail(405, "不支持的请求方法：" + e.getMethod()));
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleOther(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(500, "服务内部错误：" + e.getMessage());
    }
}
