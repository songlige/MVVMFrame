package com.hk.word.gameboosterproject.presentation.home

import com.hk.word.gameboosterproject.core.network.NetworkException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 将底层网络异常转换为可直接展示给用户的首页文案。
 */
object TodoUserMessageMapper {
    fun map(exception: NetworkException, todoId: Int? = null): String =
        when (exception) {
            is NetworkException.HttpError -> {
                when (exception.code) {
                    404 -> {
                        if (todoId != null) {
                            "编号 #$todoId 的 Todo 不存在，请切换其他编号重试。"
                        } else {
                            "请求的数据不存在，请稍后重试。"
                        }
                    }

                    in 500..599 -> "服务器暂时不可用，请稍后再试。"
                    else -> "请求失败（HTTP ${exception.code}），请稍后重试。"
                }
            }

            is NetworkException.ParseError -> {
                "服务器返回的数据格式异常，请稍后重试。"
            }

            is NetworkException.ConnectionError -> {
                when (exception.error) {
                    is SocketTimeoutException -> "请求超时，请检查网络后重试。"
                    is UnknownHostException -> "当前无法连接服务器，请确认网络是否可用。"
                    else -> "网络连接失败，请稍后重试。"
                }
            }
        }
}
