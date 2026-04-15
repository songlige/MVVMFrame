package com.hk.word.gameboosterproject.presentation.home

import com.hk.word.gameboosterproject.domain.model.Todo

enum class TodoCompletionFilter(val label: String) {
    ALL("全部"),
    COMPLETED("已完成"),
    INCOMPLETE("未完成")
}

data class HomeUiState(
    val loading: Boolean = false,
    val allTodos: List<Todo> = emptyList(),
    val visibleTodos: List<Todo> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: TodoCompletionFilter = TodoCompletionFilter.ALL,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val hasLoadedOnce: Boolean = false
) {
    val resultSummary: String
        get() = "共 ${visibleTodos.size} 项"

    val hasContent: Boolean
        get() = visibleTodos.isNotEmpty()

    val hasActiveFilters: Boolean
        get() = searchQuery.isNotBlank() || selectedFilter != TodoCompletionFilter.ALL
}
