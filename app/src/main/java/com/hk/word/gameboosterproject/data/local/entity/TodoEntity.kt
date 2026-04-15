package com.hk.word.gameboosterproject.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todos")
data class TodoEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val completed: Boolean,
    val cachedAt: Long
)
