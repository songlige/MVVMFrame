package com.hk.word.gameboosterproject.data.remote.dto

import org.json.JSONArray
import org.json.JSONObject

data class TodoDto(
    val id: Int,
    val title: String,
    val completed: Boolean
) {
    companion object {
        fun fromJson(raw: String): TodoDto {
            val json = JSONObject(raw)
            return TodoDto(
                id = json.optInt("id"),
                title = json.optString("title"),
                completed = json.optBoolean("completed")
            )
        }

        fun listFromJson(raw: String): List<TodoDto> {
            val jsonArray = JSONArray(raw)
            return buildList(jsonArray.length()) {
                for (index in 0 until jsonArray.length()) {
                    val json = jsonArray.optJSONObject(index) ?: continue
                    add(
                        TodoDto(
                            id = json.optInt("id"),
                            title = json.optString("title"),
                            completed = json.optBoolean("completed")
                        )
                    )
                }
            }
        }
    }
}
