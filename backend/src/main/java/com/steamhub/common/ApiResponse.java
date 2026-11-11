package com.steamhub.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一响应体，前端据 code 判断成败：0 成功，其余为失败。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统一响应体")
public class ApiResponse<T> {

    /** 成功业务码 */
    public static final int SUCCESS_CODE = 0;

    /** 成功默认提示 */
    public static final String SUCCESS_MESSAGE = "成功";

    /** 业务码，0 表示成功 */
    @Schema(description = "业务码，0 表示成功", example = "0")
    private int code;

    /** 提示信息，失败时为具体原因 */
    @Schema(description = "提示信息", example = "成功")
    private String message;

    /** 业务数据，失败时为 null */
    @Schema(description = "业务数据，失败时为 null")
    private T data;

    /**
     * 构造成功响应。
     *
     * @param data 业务数据，允许为 null
     * @param <T>  业务数据类型
     * @return code=0、message 为「成功」的响应体
     */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 构造带自定义提示的成功响应。
     *
     * @param message 自定义提示，非空
     * @param data    业务数据，允许为 null
     * @param <T>     业务数据类型
     * @return code=0 的响应体
     */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(SUCCESS_CODE, message, data);
    }

    /**
     * 构造无数据的成功响应，用于删除、退出登录等无返回内容的场景。
     *
     * @return code=0 且 data 为 null 的响应体
     */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 构造失败响应，提示取错误码自带文案。
     *
     * @param errorCode 错误码，非空
     * @param <T>       业务数据类型，失败时恒为 null
     * @return 带错误码与其默认提示的响应体
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 构造失败响应，用自定义提示覆盖错误码文案。
     *
     * @param errorCode 错误码，非空
     * @param message   自定义提示，非空
     * @param <T>       业务数据类型，失败时恒为 null
     * @return 带错误码与自定义提示的响应体
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null);
    }

    /**
     * 构造失败响应，直接指定业务码与提示。
     *
     * @param code    业务码，非 0
     * @param message 提示信息，非空
     * @param <T>     业务数据类型，失败时恒为 null
     * @return 指定业务码与提示的响应体
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
