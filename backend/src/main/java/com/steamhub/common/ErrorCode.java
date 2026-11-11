package com.steamhub.common;

import lombok.Getter;

/**
 * 业务错误码，code=0 表示成功；数值与标准 HTTP 状态码一致时会被全局异常处理器复用为 HTTP 状态。
 */
@Getter
public enum ErrorCode {

    /** 成功 */
    SUCCESS(0, "成功"),

    /** 请求参数不合法 */
    PARAM_INVALID(400, "请求参数校验失败"),

    /** 未登录或登录态已过期 */
    UNAUTHORIZED(401, "未登录或登录状态已失效"),

    /** 已登录但无权访问 */
    FORBIDDEN(403, "没有访问权限"),

    /** 目标数据不存在 */
    NOT_FOUND(404, "请求的资源不存在"),

    /** 请求方法与该路径不匹配 */
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),

    /** 唯一键冲突等数据完整性错误 */
    DATA_CONFLICT(409, "数据冲突，请检查是否重复提交"),

    /** 请求体格式或媒体类型不被支持 */
    UNSUPPORTED_MEDIA_TYPE(415, "请求格式不支持"),

    /** 通用业务失败：非标准 HTTP 状态码，HTTP 层按 400 返回 */
    BIZ_FAIL(1000, "业务处理失败"),

    /** 未预料的服务器异常 */
    INTERNAL_ERROR(500, "服务器内部错误");

    /** 业务码，0 表示成功 */
    private final int code;

    /** 该错误码对应的默认提示信息 */
    private final String message;

    /**
     * 构造错误码。
     *
     * @param code    业务码，0 表示成功
     * @param message 默认提示信息，非空
     */
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
