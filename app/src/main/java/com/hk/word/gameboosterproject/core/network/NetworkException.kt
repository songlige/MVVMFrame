package com.hk.word.gameboosterproject.core.network

/**
 * 网络相关的异常基类，用于表示网络请求过程中可能出现的不同错误类型。
 *
 * 这个密封类封装了几种常见的网络错误：HTTP 错误、解析错误和连接错误。
 * 它继承自 [Exception]，并携带可选的底层异常原因。
 *
 * @param message 错误消息文本，供上层日志或展示使用
 * @param cause 可选的导致本异常的底层异常
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * HTTP 错误（非 2xx 响应），携带响应码和响应体（如果有）
     *
     * @property code HTTP 状态码
     * @property body 响应体字符串，可能为空
     */
    data class HttpError(val code: Int, val body: String?) :
        NetworkException("Http error $code")

    /**
     * 解析响应时发生错误（例如 JSON 解析失败）
     *
     * @property raw 原始响应文本（可能为 null）
     * @property error 导致解析失败的底层异常
     */
    data class ParseError(val raw: String?, val error: Throwable) :
        NetworkException("Parse response failed", error)

    /**
     * 网络连接错误（例如超时、无法连接等）
     *
     * @property error 导致连接失败的底层异常
     */
    data class ConnectionError(val error: Throwable) :
        NetworkException("Network connection failed", error)
}
