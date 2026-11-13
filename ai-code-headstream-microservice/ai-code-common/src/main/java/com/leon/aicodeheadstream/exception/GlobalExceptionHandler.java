package com.leon.aicodeheadstream.exception;

import com.leon.aicodeheadstream.common.BaseResponse;
import com.leon.aicodeheadstream.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@Hidden
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    public BaseResponse<?> multipartExceptionHandler(MultipartException e) {
        log.error("MultipartException", e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, "文件上传请求格式错误，请使用 multipart/form-data 提交");
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
