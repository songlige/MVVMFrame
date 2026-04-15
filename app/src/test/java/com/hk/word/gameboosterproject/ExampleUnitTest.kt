package com.hk.word.gameboosterproject

import com.hk.word.gameboosterproject.core.network.NetworkException
import com.hk.word.gameboosterproject.presentation.home.TodoUserMessageMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class ExampleUnitTest {
    @Test
    fun `404 maps to not found message`() {
        val message = TodoUserMessageMapper.map(NetworkException.HttpError(404, null), todoId = 201)

        assertEquals("编号 #201 的 Todo 不存在，请切换其他编号重试。", message)
    }

    @Test
    fun `timeout maps to retryable network message`() {
        val message = TodoUserMessageMapper.map(
            NetworkException.ConnectionError(SocketTimeoutException("timeout")),
            todoId = 1
        )

        assertEquals("请求超时，请检查网络后重试。", message)
    }

    @Test
    fun `unknown host maps to connectivity hint`() {
        val message = TodoUserMessageMapper.map(
            NetworkException.ConnectionError(UnknownHostException("offline")),
            todoId = 1
        )

        assertTrue(message.contains("网络"))
    }
}