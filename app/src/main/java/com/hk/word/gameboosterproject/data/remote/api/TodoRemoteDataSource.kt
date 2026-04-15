package com.hk.word.gameboosterproject.data.remote.api

import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.data.remote.dto.TodoDto

interface TodoRemoteDataSource {
    suspend fun fetchTodo(todoId: Int): NetworkResult<TodoDto>
}
