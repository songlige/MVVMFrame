package com.hk.word.gameboosterproject.data.local.mapper

import com.hk.word.gameboosterproject.data.local.entity.TodoEntity
import com.hk.word.gameboosterproject.data.remote.dto.TodoDto
import com.hk.word.gameboosterproject.domain.model.Todo

fun TodoDto.toEntity(cachedAt: Long = System.currentTimeMillis()): TodoEntity =
    TodoEntity(
        id = id,
        title = title,
        completed = completed,
        cachedAt = cachedAt
    )

fun TodoEntity.toDomain(): Todo =
    Todo(
        id = id,
        title = title,
        completed = completed
    )

fun TodoDto.toDomain(): Todo =
    Todo(
        id = id,
        title = title,
        completed = completed
    )
