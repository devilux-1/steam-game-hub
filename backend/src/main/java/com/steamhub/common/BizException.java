package com.steamhub.common;

import lombok.Getter;

/**
 * 业务异常，由全局异常处理器转换成统一响应体，业务代码只需抛出而无需关心响应格式。
 */
@Getter
public class BizException extends RuntimeException {

    /** 业务错误码，决定响应体中的 code */
    private final ErrorCode errorCode;

    /**
     * 使用错误码自带的提示构造异常。
     *
     * @param errorCode 错误码，非空
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * 使用自定义提示构造异常。
     *
     * @param errorCode 错误码，非空
     * @param message   自定义提示，非空，会覆盖错误码自带的提示
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
