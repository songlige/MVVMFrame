package com.hk.word.gameboosterproject.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hk.word.gameboosterproject.data.local.entity.TodoEntity

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE id = :todoId LIMIT 1")
    suspend fun getTodoById(todoId: Int): TodoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: TodoEntity)
}
