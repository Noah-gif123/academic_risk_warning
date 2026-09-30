package com.example.redchina.handler;



import com.example.redchina.common.ResultVo;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    // 处理业务异常
    @ExceptionHandler(RuntimeException.class)
    @ResponseBody
    public ResultVo<?> handleRuntimeException(RuntimeException e) {
        return ResultVo.error(e.getMessage());
    }

    // 处理参数校验异常
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseBody
    public ResultVo<?> handleConstraintViolationException(jakarta.validation.ConstraintViolationException e) {
        String message = e.getConstraintViolations().iterator().next().getMessage();
        return ResultVo.error(message);
    }

    // 处理其他异常
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResultVo<?> handleException(Exception e) {
        e.printStackTrace();
        return ResultVo.error("系统异常，请联系管理员");
    }
}
