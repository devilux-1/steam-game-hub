package com.steamhub.client;

/**
 * Nexus 接口调用异常，携带足够定位问题的上下文（游戏 domain、HTTP 状态），
 * 由同步服务按游戏维度捕获并记录，不影响其它游戏继续同步。
 */
public class NexusApiException extends RuntimeException {

    /**
     * 构造异常。
     *
     * @param message 错误描述，包含 domain 与失败原因，非空
     */
    public NexusApiException(String message) {
        super(message);
    }

    /**
     * 构造异常并保留底层原因。
     *
     * @param message 错误描述，非空
     * @param cause   底层异常，可能为 null
     */
    public NexusApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
