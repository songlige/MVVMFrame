package com.hk.word.gameboosterproject.domain.repository

import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.model.Todo

interface TodoRepository {
    suspend fun getTodo(): NetworkResult<Todo>
}
