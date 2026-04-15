package com.hk.word.gameboosterproject.presentation.home

data class HomeUiState(
    val loading: Boolean = false,
    val currentTodoId: Int = MIN_TODO_ID,
    val todoTitle: String? = null,
    val completed: Boolean? = null,
    val statusMessage: String? = null,
    val errorMessage: String? = null
) {
    val canLoadPrevious: Boolean
        get() = currentTodoId > MIN_TODO_ID

    val canLoadNext: Boolean
        get() = currentTodoId < MAX_TODO_ID

    val hasContent: Boolean
        get() = todoTitle != null && completed != null

    companion object {
        const val MIN_TODO_ID = 1
        const val MAX_TODO_ID = 200
    }
}
