package com.hk.word.gameboosterproject.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.model.Todo
import com.hk.word.gameboosterproject.domain.repository.TodoDataSource
import com.hk.word.gameboosterproject.domain.usecase.GetTodoListUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel：负责通过 [GetTodoListUseCase] 加载 Todo 列表并将结果暴露为 UI 状态。
 *
 * 该 ViewModel 管理一个 [HomeUiState]，包含列表数据、搜索词、筛选条件和错误提示等字段。
 * UI 层应订阅 [uiState] 以响应状态变化。
 *
 * @param getTodoListUseCase 用于获取 Todo 列表的用例
 */
class HomeViewModel(
    private val getTodoListUseCase: GetTodoListUseCase
) : ViewModel() {
    // 可变的 UI 状态，仅在 ViewModel 内部更新
    private val _uiState = MutableStateFlow(HomeUiState())

    /**
     * 对外暴露的不可变 UI 状态流（供 UI 层订阅）。
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * 异步加载 Todo 列表并更新 UI 状态：
     * - 请求开始时设置 loading = true，并清空旧的错误提示
     * - 成功时缓存完整列表，并按当前搜索词/筛选条件计算展示结果
     * - 失败时将底层异常映射为用户可读文案，并将 loading 设为 false
     *
     * 使用 [viewModelScope] 启动协程，确保生命周期安全。
     */
    fun loadTodos() {
        if (_uiState.value.loading) return
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            when (val result = getTodoListUseCase()) {
                is NetworkResult.Success -> {
                    val currentState = _uiState.value
                    val filteredTodos = filterTodos(
                        todos = result.data.todos,
                        searchQuery = currentState.searchQuery,
                        selectedFilter = currentState.selectedFilter
                    )
                    _uiState.update {
                        it.copy(
                            loading = false,
                            allTodos = result.data.todos,
                            visibleTodos = filteredTodos,
                            statusMessage = when (result.data.source) {
                                TodoDataSource.Remote -> "已从网络加载最新列表"
                                is TodoDataSource.LocalCache -> "网络失败，已展示本地缓存列表"
                            },
                            errorMessage = null,
                            hasLoadedOnce = true
                        )
                    }
                }

                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            allTodos = emptyList(),
                            visibleTodos = emptyList(),
                            statusMessage = null,
                            errorMessage = TodoUserMessageMapper.map(result.exception),
                            hasLoadedOnce = true
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                visibleTodos = filterTodos(
                    todos = state.allTodos,
                    searchQuery = query,
                    selectedFilter = state.selectedFilter
                )
            )
        }
    }

    fun onFilterChange(filter: TodoCompletionFilter) {
        _uiState.update { state ->
            state.copy(
                selectedFilter = filter,
                visibleTodos = filterTodos(
                    todos = state.allTodos,
                    searchQuery = state.searchQuery,
                    selectedFilter = filter
                )
            )
        }
    }

    fun retryLoad() {
        loadTodos()
    }

    private fun filterTodos(
        todos: List<Todo>,
        searchQuery: String,
        selectedFilter: TodoCompletionFilter
    ): List<Todo> {
        val normalizedQuery = searchQuery.trim()
        return todos.filter { todo ->
            val matchesQuery = normalizedQuery.isBlank() ||
                todo.title.contains(normalizedQuery, ignoreCase = true)
            val matchesFilter = when (selectedFilter) {
                TodoCompletionFilter.ALL -> true
                TodoCompletionFilter.COMPLETED -> todo.completed
                TodoCompletionFilter.INCOMPLETE -> !todo.completed
            }
            matchesQuery && matchesFilter
        }
    }
}
