package com.steamhub.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一结果包装类，所有 Controller 一律返回该结构，前端据 code 判断成败：0 成功，其余为失败。
 * 结构固定为 {code, message, data}，三个字段在全站保持不变，便于前端做统一拦截。
 *
 * @param <T> 业务数据类型
 */
@Data
@NoArgsConstructor
@Schema(description = "统一响应结果")
public class Result<T> {

    /** 成功业务码 */
    public static final int SUCCESS_CODE = 0;

    /** 成功默认提示 */
    public static final String SUCCESS_MESSAGE = "成功";

    /** 业务码，0 表示成功，非 0 表示失败，取值见 {@link ErrorCode} */
    @Schema(description = "业务码，0 表示成功", example = "0")
    private int code;

    /** 提示信息，失败时为具体原因，可直接展示给用户 */
    @Schema(description = "提示信息", example = "成功")
    private String message;

    /** 业务数据，失败时为 null */
    @Schema(description = "业务数据，失败时为 null")
    private T data;

    /**
     * 构造带自定义业务码与提示的结果，供内部静态方法复用。
     *
     * @param code    业务码，0 表示成功
     * @param message 提示信息，非空
     * @param data    业务数据，允许为 null
     */
    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 构造无数据成功结果，用于删除、退出登录等无返回内容的场景。
     *
     * @return code=0 且 data 为 null 的结果
     */
    public static Result<Void> success() {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, null);
    }

    /**
     * 构造成功结果。
     *
     * @param data 业务数据，允许为 null
     * @param <T>  业务数据类型
     * @return code=0、message 为「成功」的结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, SUCCESS_MESSAGE, data);
    }

    /**
     * 构造带自定义提示的成功结果，用于需要提示用户操作已完成但数据无需回传的场景。
     *
     * @param message 自定义提示，非空
     * @param data    业务数据，允许为 null
     * @param <T>     业务数据类型
     * @return code=0 的结果
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data);
    }

    /**
     * 构造失败结果，提示取错误码自带文案。
     *
     * @param errorCode 错误码，非空
     * @param <T>       业务数据类型，失败时恒为 null
     * @return 带错误码与其默认提示的结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    /**
     * 构造失败结果，用自定义提示覆盖错误码自带文案。
     *
     * @param errorCode 错误码，非空
     * @param message   自定义提示，非空
     * @param <T>       业务数据类型，失败时恒为 null
     * @return 带错误码与自定义提示的结果
     */
    public static <T> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(errorCode.getCode(), message, null);
    }

    /**
     * 构造失败结果，直接指定业务码与提示，用于错误码枚举未覆盖的场景。
     *
     * @param code    业务码，非 0
     * @param message 提示信息，非空
     * @param <T>     业务数据类型，失败时恒为 null
     * @return 指定业务码与提示的结果
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    /**
     * 判断本次调用是否成功，供调用方在链式处理中快速分支。
     * 标注 @JsonIgnore，避免被 Jackson 当成属性序列化成多余的 success 字段，保持响应体只有 code/message/data。
     *
     * @return code 等于 0 时返回 true
     */
    @JsonIgnore
    public boolean isSuccess() {
        return this.code == SUCCESS_CODE;
    }
}
