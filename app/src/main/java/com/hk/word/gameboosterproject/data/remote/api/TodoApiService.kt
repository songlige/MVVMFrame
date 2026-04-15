package com.hk.word.gameboosterproject.data.remote.api

import com.hk.word.gameboosterproject.core.network.HttpClient
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.data.remote.dto.TodoDto

class TodoApiService(
    private val httpClient: HttpClient
) : TodoRemoteDataSource {
    override suspend fun fetchTodo(todoId: Int): NetworkResult<TodoDto> {
        return httpClient.get("https://jsonplaceholder.typicode.com/todos/$todoId") { raw ->
            TodoDto.fromJson(raw)
        }
    }
}
