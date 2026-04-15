package com.hk.word.gameboosterproject.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hk.word.gameboosterproject.data.local.dao.TodoDao
import com.hk.word.gameboosterproject.data.local.entity.TodoEntity

@Database(
    entities = [TodoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun todoDao(): TodoDao
}
