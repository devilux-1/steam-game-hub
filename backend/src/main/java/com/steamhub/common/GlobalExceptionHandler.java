package com.steamhub.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器，把校验失败、业务异常、未预料的异常统一转换成 {@link ApiResponse}，避免前端收到裸 500。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体校验失败（@RequestBody 搭配 @Valid）。
     *
     * @param e 校验失败异常，携带全部字段错误
     * @return HTTP 400，message 为各字段错误拼接结果
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("请求体参数校验失败: {}", message);
        // 无可读字段错误时退回错误码默认文案
        return build(ErrorCode.PARAM_INVALID, message.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : message);
    }

    /**
     * 处理查询参数/路径参数校验失败（@Validated 标注在类上时由方法参数约束触发）。
     *
     * @param e 约束违反异常，携带全部违规项
     * @return HTTP 400，message 为各违规项提示拼接结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求参数校验失败: {}", message);
        return build(ErrorCode.PARAM_INVALID, message.isEmpty() ? ErrorCode.PARAM_INVALID.getMessage() : message);
    }

    /**
     * 处理业务异常，按异常携带的错误码返回。
     *
     * @param e 业务异常，携带错误码与提示
     * @return HTTP 状态由错误码推导，message 为异常提示
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<ApiResponse<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return build(e.getErrorCode(), e.getMessage());
    }

    /**
     * 兜底处理未预料异常，屏蔽堆栈细节只回传通用提示。
     *
     * @param e 未被其它处理器捕获的异常
     * @return HTTP 500，data 为 null
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 未预料异常必须打全栈日志，否则线上难以定位
        log.error("服务器内部错误", e);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getMessage());
    }

    /**
     * 把单个字段错误格式化为「字段名: 校验提示」。
     *
     * @param fieldError 字段错误，字段名与提示都可能为空
     * @return 格式化后的提示，例如「title: 不能为空」
     */
    private String formatFieldError(FieldError fieldError) {
        String defaultMessage = fieldError.getDefaultMessage();
        return fieldError.getField() + ": " + (defaultMessage == null ? "参数不合法" : defaultMessage);
    }

    /**
     * 按错误码构造错误响应：错误码等于标准 HTTP 状态码时直接复用，否则统一按 400 返回。
     *
     * @param errorCode 错误码，非空
     * @param message   提示信息，非空
     * @return HTTP 状态与统一响应体
     */
    private ResponseEntity<ApiResponse<Void>> build(ErrorCode errorCode, String message) {
        HttpStatus status = HttpStatus.resolve(errorCode.getCode());
        // 业务码（如 BIZ_FAIL=1000）不是标准 HTTP 状态码，统一按 400 交给前端按 code 处理
        return ResponseEntity.status(status == null ? HttpStatus.BAD_REQUEST : status)
                .body(ApiResponse.fail(errorCode, message));
    }
}
