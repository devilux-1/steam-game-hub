package com.steamhub.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 * 把校验失败、业务异常、路由不存在、数据库冲突与未预料异常统一转换成 {@link Result}，
 * 保证前端永远收到 {code, message, data} 结构，不会出现裸 500 或 Spring 默认错误页。
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
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("请求体参数校验失败: {}", message);
        return build(ErrorCode.PARAM_INVALID, fallback(message, ErrorCode.PARAM_INVALID));
    }

    /**
     * 处理表单/查询串绑定到对象时的校验失败。
     *
     * @param e 绑定异常，携带全部字段错误
     * @return HTTP 400，message 为各字段错误拼接结果
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定校验失败: {}", message);
        return build(ErrorCode.PARAM_INVALID, fallback(message, ErrorCode.PARAM_INVALID));
    }

    /**
     * 处理方法参数上的约束校验失败（类上标注 @Validated 时由方法参数注解触发）。
     *
     * @param e 约束违反异常，携带全部违规项
     * @return HTTP 400，message 为各违规项提示拼接结果
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("请求参数校验失败: {}", message);
        return build(ErrorCode.PARAM_INVALID, fallback(message, ErrorCode.PARAM_INVALID));
    }

    /**
     * 处理必需的查询参数缺失。
     *
     * @param e 缺失参数异常，携带参数名
     * @return HTTP 400，message 指明缺失的参数名
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        String message = "缺少必需参数: " + e.getParameterName();
        log.warn(message);
        return build(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 处理参数类型不匹配，例如把非数字传给 Integer 参数。
     *
     * @param e 类型不匹配异常，携带参数名与期望类型
     * @return HTTP 400，message 指明参数名与期望类型
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String expectType = e.getRequiredType() == null ? "合法类型" : e.getRequiredType().getSimpleName();
        String message = "参数 " + e.getName() + " 类型不正确，应为 " + expectType;
        log.warn(message);
        return build(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 处理请求体无法解析，例如 JSON 语法错误或空请求体。
     *
     * @param e 消息不可读异常
     * @return HTTP 400，message 提示请求体裁剪有误
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        // 不把解析异常原文返回给前端，避免暴露内部类结构
        log.warn("请求体解析失败: {}", e.getMessage());
        return build(ErrorCode.PARAM_INVALID, "请求体格式不正确，请检查 JSON 结构");
    }

    /**
     * 处理业务异常，按异常携带的错误码返回。
     *
     * @param e 业务异常，携带错误码与提示
     * @return HTTP 状态由错误码推导，message 为异常提示
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBizException(BizException e) {
        log.warn("业务异常: code={}, message={}", e.getErrorCode().getCode(), e.getMessage());
        return build(e.getErrorCode(), fallback(e.getMessage(), e.getErrorCode()));
    }

    /**
     * 处理唯一键冲突、外键约束等数据库完整性问题。
     *
     * @param e 数据完整性异常
     * @return HTTP 409，message 为通用冲突提示，不回传 SQL 细节
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Result<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        // 原始信息含表名与 SQL 片段，只记日志不返回
        log.warn("数据完整性冲突: {}", e.getMessage());
        return build(ErrorCode.DATA_CONFLICT, ErrorCode.DATA_CONFLICT.getMessage());
    }

    /**
     * 处理访问不存在的静态资源或路径（Spring 6.1 静态资源未命中时抛出）。
     *
     * @param e 资源未找到异常
     * @param request 当前请求，用于日志记录访问路径
     * @return HTTP 404
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResourceFound(NoResourceFoundException e, HttpServletRequest request) {
        log.warn("路径不存在: {} {}", request.getMethod(), request.getRequestURI());
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 处理没有匹配 Handler 的请求（需开启 throw-exception-if-no-handler-found）。
     *
     * @param e 未找到处理器异常
     * @return HTTP 404
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Result<Void>> handleNoHandlerFound(NoHandlerFoundException e) {
        log.warn("接口不存在: {} {}", e.getHttpMethod(), e.getRequestURL());
        return build(ErrorCode.NOT_FOUND, ErrorCode.NOT_FOUND.getMessage());
    }

    /**
     * 处理请求方法不支持，例如对只接受 GET 的接口发起 POST。
     *
     * @param e 方法不支持异常
     * @return HTTP 405，message 指明支持的请求方法
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        String supported = e.getSupportedHttpMethods() == null
                ? ""
                : "，支持: " + e.getSupportedHttpMethods();
        log.warn("请求方法不支持: {}{}", e.getMethod(), supported);
        return build(ErrorCode.METHOD_NOT_ALLOWED, "请求方法不支持" + supported);
    }

    /**
     * 处理请求体媒体类型不支持，例如接口要求 JSON 却提交了表单。
     *
     * @param e 媒体类型不支持异常
     * @return HTTP 415
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
        log.warn("请求媒体类型不支持: {}", e.getContentType());
        return build(ErrorCode.UNSUPPORTED_MEDIA_TYPE,
                "请求格式不支持，请使用 " + e.getSupportedMediaTypes());
    }

    /**
     * 兜底处理未预料异常，屏蔽堆栈细节只回传通用提示。
     *
     * @param e 未被其它处理器捕获的异常
     * @return HTTP 500，data 为 null
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception e) {
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
     * 提示为空时回退到错误码自带文案，避免前端收到空 message。
     *
     * @param message   待检查的提示，可能为空
     * @param errorCode 错误码，非空
     * @return 非空的提示信息
     */
    private String fallback(String message, ErrorCode errorCode) {
        return (message == null || message.isBlank()) ? errorCode.getMessage() : message;
    }

    /**
     * 按错误码构造错误响应：错误码等于标准 HTTP 状态码时直接复用，否则统一按 400 返回。
     *
     * @param errorCode 错误码，非空
     * @param message   提示信息，非空
     * @return HTTP 状态与统一结果体
     */
    private ResponseEntity<Result<Void>> build(ErrorCode errorCode, String message) {
        HttpStatus status = HttpStatus.resolve(errorCode.getCode());
        // 业务码（如 BIZ_FAIL=1000）不是标准 HTTP 状态码，统一按 400 交给前端按 code 处理
        return ResponseEntity.status(status == null ? HttpStatus.BAD_REQUEST : status)
                .body(Result.fail(errorCode, message));
    }
}
