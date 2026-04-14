package com.hk.word.gameboosterproject.core.network

/**
 * 单次 HTTP 调用的可变更快照，供 [HttpInterceptor] 在发出请求前改写。
 */
data class HttpRequest(
    val url: String,
    val method: String,
    val queryParams: Map<String, String>? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null,
    val bodyContentType: String = "application/json; charset=UTF-8"
)

fun interface HttpInterceptor {
    fun intercept(request: HttpRequest): HttpRequest
}

fun interface HttpLogger {
    fun log(message: String)
}
