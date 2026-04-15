package com.hk.word.gameboosterproject.domain.repository

import com.hk.word.gameboosterproject.core.network.NetworkException
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.model.Todo

sealed interface TodoDataSource {
    data object Remote : TodoDataSource
    data class LocalCache(val fallbackReason: NetworkException) : TodoDataSource
}

data class TodoLoadResult(
    val todo: Todo,
    val source: TodoDataSource
)

interface TodoRepository {
    suspend fun getTodo(todoId: Int): NetworkResult<TodoLoadResult>
}
