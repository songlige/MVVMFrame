package com.hk.word.gameboosterproject.core.network

/**
 * 网络请求结果封装。
 *
 * 这个密封接口表示一次网络调用的两种可能结果：
 * - Success：请求成功且返回了数据
 * - Error：请求失败并包含网络异常
 *
 * @param T 成功时返回的数据类型
 */
sealed interface NetworkResult<out T> {
    /**
     * 请求成功，携带返回的数据
     *
     * @param T 返回的数据类型
     * @property data 返回的数据
     */
    data class Success<T>(val data: T) : NetworkResult<T>

    /**
     * 请求出错，携带封装的网络异常
     *
     * @property exception 导致请求失败的 [NetworkException]
     */
    data class Error(val exception: NetworkException) : NetworkResult<Nothing>
}
