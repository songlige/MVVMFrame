package com.hk.word.gameboosterproject.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.ProtocolException
import java.net.URL
import java.net.URLEncoder

/**
 * 简单的同步/阻塞式 HTTP 客户端封装，使用 HttpURLConnection 实现。
 *
 * 设计与行为说明：
 * - 所有对外方法都是 suspend，并在内部使用 withContext(Dispatchers.IO) 将阻塞 IO 移到 IO 线程。
 * - 提供常见的 HTTP 方法：GET/POST/PUT/DELETE/PATCH。每个方法接收 URL、可选的 query params、headers 和 body（如适用）。
 * - 支持拦截器（HttpInterceptor），在请求被发送前按顺序处理并返回最终的 HttpRequest（可用于添加公用 headers、签名等）。
 * - 支持可选的 HttpLogger，用于将请求/响应日志输出到任意实现（例如 Logcat），若为 null 则不记录日志。
 * - 使用严格的错误封装（NetworkResult / NetworkException），将网络错误、解析错误和 HTTP 状态码错误区分开来。
 *
 * 重要注意事项：
 * - 不在此处做异步重试/恢复或复杂的连接池管理；适用于简单 REST 请求和示例项目。若需生产级别功能，请引入 OkHttp/Retrofit 等成熟库。
 */
class HttpClient(
    private val connectTimeoutMs: Int = 8_000,
    private val readTimeoutMs: Int = 8_000,
    private val interceptors: List<HttpInterceptor> = emptyList(),
    private val logger: HttpLogger? = null
) {
    /**
     * 简单的 GET 请求封装。
     * - parser: 将响应 body (String) 解析为期望类型 T 的函数。若解析失败会返回 NetworkResult.Error(ParseError)
     */
    suspend fun <T> get(
        url: String,
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        parser: (String) -> T
    ): NetworkResult<T> {
        return request(
            url = url,
            method = "GET",
            body = null,
            queryParams = queryParams,
            headers = headers,
            parser = parser
        )
    }

    /** POST 请求（默认 bodyContentType 为 application/json） */
    suspend fun <T> post(
        url: String,
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        body: String? = null,
        bodyContentType: String = "application/json; charset=UTF-8",
        parser: (String) -> T
    ): NetworkResult<T> =
        request(
            url = url,
            method = "POST",
            body = body,
            bodyContentType = bodyContentType,
            queryParams = queryParams,
            headers = headers,
            parser = parser
        )

    /** PUT 请求 */
    suspend fun <T> put(
        url: String,
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        body: String? = null,
        bodyContentType: String = "application/json; charset=UTF-8",
        parser: (String) -> T
    ): NetworkResult<T> =
        request(
            url = url,
            method = "PUT",
            body = body,
            bodyContentType = bodyContentType,
            queryParams = queryParams,
            headers = headers,
            parser = parser
        )

    /** DELETE 请求 */
    suspend fun <T> delete(
        url: String,
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        body: String? = null,
        bodyContentType: String = "application/json; charset=UTF-8",
        parser: (String) -> T
    ): NetworkResult<T> =
        request(
            url = url,
            method = "DELETE",
            body = body,
            bodyContentType = bodyContentType,
            queryParams = queryParams,
            headers = headers,
            parser = parser
        )

    /** PATCH 请求（注意：部分 Android 版本需要反射回退以支持 PATCH，这在本文件底部被处理） */
    suspend fun <T> patch(
        url: String,
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        body: String? = null,
        bodyContentType: String = "application/json; charset=UTF-8",
        parser: (String) -> T
    ): NetworkResult<T> =
        request(
            url = url,
            method = "PATCH",
            body = body,
            bodyContentType = bodyContentType,
            queryParams = queryParams,
            headers = headers,
            parser = parser
        )

    /**
     * 核心请求实现：
     * - 在 IO 线程运行。
     * - 应用拦截器得到最终 HttpRequest。
     * - 记录请求/响应日志（若 logger 非空）。
     * - 检查 HTTP 状态码，非 2xx 返回 NetworkResult.Error(HttpError)。
     * - 使用传入的 parser 将响应 body 转换为目标类型 T，解析异常会被捕获并返回 ParseError。
     */
    private suspend fun <T> request(
        url: String,
        method: String,
        body: String? = null,
        bodyContentType: String = "application/json; charset=UTF-8",
        queryParams: Map<String, String>? = null,
        headers: Map<String, String>? = null,
        parser: (String) -> T
    ): NetworkResult<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val initial = HttpRequest(
                url = url,
                method = method,
                queryParams = queryParams,
                headers = headers,
                body = body,
                bodyContentType = bodyContentType
            )
            // 依次执行拦截器，得到最终的请求（示例：全局 header、鉴权 token 注入）
            val finalRequest = interceptors.fold(initial) { acc, interceptor ->
                interceptor.intercept(acc)
            }
            val fullUrl = appendQueryParams(finalRequest.url, finalRequest.queryParams)
            // 记录发出请求（扩展函数接受 nullable receiver，因此即便 logger 为 null 也可以安全调用）
            logger.logOutgoing(finalRequest, fullUrl)

            connection = (URL(fullUrl).openConnection() as HttpURLConnection).apply {
                setHttpMethod(finalRequest.method)
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                setRequestProperty("Accept", "application/json")
                if (finalRequest.body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", finalRequest.bodyContentType)
                }
                finalRequest.headers?.forEach { (name, value) -> setRequestProperty(name, value) }
            }

            // 写入请求体（如果有）
            if (finalRequest.body != null) {
                val bytes = finalRequest.body.toByteArray(Charsets.UTF_8)
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.outputStream.use { it.write(bytes) }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.use { input ->
                BufferedReader(InputStreamReader(input)).readText()
            }
            // 记录响应（会截断过长的 body）
            logger.logIncoming(code, responseText)

            if (code !in 200..299) {
                return@withContext NetworkResult.Error(NetworkException.HttpError(code, responseText))
            }

            val rawBody = responseText.orEmpty()
            val parsed = runCatching { parser(rawBody) }
                .getOrElse { error ->
                    return@withContext NetworkResult.Error(
                        NetworkException.ParseError(rawBody, error)
                    )
                }

            NetworkResult.Success(parsed)
        } catch (error: Exception) {
            // 记录异常（如果 logger 存在），并统一包装为 ConnectionError
            logger?.log("<-- error: ${'$'}{error.message}")
            NetworkResult.Error(NetworkException.ConnectionError(error))
        } finally {
            connection?.disconnect()
        }
    }
}

private const val LOG_RESPONSE_BODY_MAX = 2048

/**
 * 扩展：记录发出请求的摘要（headers 会做简单脱敏处理）
 * 接收者为 HttpLogger?，因此可以在调用端直接用 `logger.logOutgoing(...)`，当 logger==null 时会立刻返回不做任何工作。
 */
private fun HttpLogger?.logOutgoing(request: HttpRequest, fullUrl: String) {
    this ?: return
    val headerLines = buildEffectiveHeaders(request).entries.joinToString("\n") { (k, v) ->
        "$k: ${maskHeaderValue(k, v)}"
    }
    val bodyLine = request.body?.let { "body: $it" } ?: "body: <empty>"
    log("--> ${request.method} $fullUrl\n$headerLines\n$bodyLine")
}

/**
 * 扩展：记录响应，超过 LOG_RESPONSE_BODY_MAX 会被截断并显示总长度。
 */
private fun HttpLogger?.logIncoming(code: Int, responseText: String?) {
    this ?: return
    val body = responseText.orEmpty()
    val shown = if (body.length > LOG_RESPONSE_BODY_MAX) {
        body.take(LOG_RESPONSE_BODY_MAX) + "... (${body.length} chars total)"
    } else {
        body
    }
    log("<-- $code\n$shown")
}

/**
 * 构建有效的 headers 映射，包含默认 Accept 和必要时的 Content-Type。
 */
private fun buildEffectiveHeaders(request: HttpRequest): Map<String, String> = buildMap {
    put("Accept", "application/json")
    if (request.body != null) put("Content-Type", request.bodyContentType)
    request.headers?.forEach { (k, v) -> put(k, v) }
}

/**
 * 对敏感 header（如 Authorization/Cookie）进行脱敏显示，其他 header 原样返回。
 */
private fun maskHeaderValue(name: String, value: String): String =
    when {
        name.equals("Authorization", ignoreCase = true) -> "***"
        name.equals("Cookie", ignoreCase = true) -> "***"
        else -> value
    }

/**
 * 将 query 参数编码后追加到 URL。保留 fragment（# 后的部分）。
 */
private fun appendQueryParams(url: String, queryParams: Map<String, String>?): String {
    if (queryParams.isNullOrEmpty()) return url
    val query = queryParams.entries.joinToString("&") { (k, v) ->
        "${encodeQueryComponent(k)}=${encodeQueryComponent(v)}"
    }
    val fragmentIndex = url.indexOf('#')
    val base = if (fragmentIndex >= 0) url.substring(0, fragmentIndex) else url
    val fragment = if (fragmentIndex >= 0) url.substring(fragmentIndex) else ""
    val sep = if ('?' in base) '&' else '?'
    return base + sep + query + fragment
}

private fun encodeQueryComponent(value: String): String =
    URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

/** 部分 Android/Java 版本上 `setRequestMethod("PATCH")` 会失败，反射回退以发送 PATCH。 */
private fun HttpURLConnection.setHttpMethod(method: String) {
    try {
        requestMethod = method
    } catch (e: ProtocolException) {
        if (method != "PATCH") throw e
        try {
            val field = HttpURLConnection::class.java.getDeclaredField("method")
            field.isAccessible = true
            field.set(this, method)
        } catch (_: Exception) {
            throw e
        }
    }
}
