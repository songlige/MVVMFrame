package com.hk.word.gameboosterproject.data.remote.api

import com.hk.word.gameboosterproject.core.network.HttpClient
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.data.remote.dto.TodoDto

class TodoApiService(private val httpClient: HttpClient) {
    suspend fun fetchTodo(): NetworkResult<TodoDto> {
        return httpClient.get("https://jsonplaceholder.typicode.com/todos/1") { raw ->
            TodoDto.fromJson(raw)
        }
    }
}
