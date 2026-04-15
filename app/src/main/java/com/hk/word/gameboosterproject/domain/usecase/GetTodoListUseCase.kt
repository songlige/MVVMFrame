package com.hk.word.gameboosterproject.domain.usecase

import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.repository.TodoListLoadResult
import com.hk.word.gameboosterproject.domain.repository.TodoRepository

class GetTodoListUseCase(
    private val repository: TodoRepository
) {
    suspend operator fun invoke(): NetworkResult<TodoListLoadResult> {
        return repository.getTodos()
    }
}
