package com.hk.word.gameboosterproject.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.repository.TodoDataSource
import com.hk.word.gameboosterproject.domain.usecase.GetTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel：负责通过 [GetTodoUseCase] 加载指定编号的 Todo 并将结果暴露为 UI 状态。
 *
 * 该 ViewModel 管理一个 [HomeUiState]，包含当前编号、加载结果和错误提示等字段。
 * UI 层应订阅 [uiState] 以响应状态变化。
 *
 * @param getTodoUseCase 用于获取 Todo 的用例，返回 [com.hk.word.gameboosterproject.core.network.NetworkResult]
 */
class HomeViewModel(
    private val getTodoUseCase: GetTodoUseCase
) : ViewModel() {
    // 可变的 UI 状态，仅在 ViewModel 内部更新
    private val _uiState = MutableStateFlow(HomeUiState())

    /**
     * 对外暴露的不可变 UI 状态流（供 UI 层订阅）。
     */
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    /**
     * 异步加载指定编号的 Todo 并更新 UI 状态：
     * - 请求开始时设置 loading = true，并清空旧的内容与错误提示
     * - 成功时填充 Todo 标题与完成状态，并将 loading 设为 false
     * - 失败时将底层异常映射为用户可读文案，并将 loading 设为 false
     *
     * 使用 [viewModelScope] 启动协程，确保生命周期安全。
     */
    fun loadTodo(todoId: Int = _uiState.value.currentTodoId) {
        if (_uiState.value.loading) return
        val targetTodoId = todoId.coerceIn(HomeUiState.MIN_TODO_ID, HomeUiState.MAX_TODO_ID)
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = true,
                    currentTodoId = targetTodoId,
                    todoTitle = null,
                    completed = null,
                    statusMessage = null,
                    errorMessage = null
                )
            }
            when (val result = getTodoUseCase(targetTodoId)) {
                is NetworkResult.Success -> {
                    val todo = result.data.todo
                    _uiState.update {
                        it.copy(
                            loading = false,
                            currentTodoId = todo.id,
                            todoTitle = todo.title,
                            completed = todo.completed,
                            statusMessage = when (result.data.source) {
                                TodoDataSource.Remote -> "已从网络加载最新数据"
                                is TodoDataSource.LocalCache -> "网络失败，已展示本地缓存数据"
                            }
                        )
                    }
                }

                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = TodoUserMessageMapper.map(result.exception, targetTodoId)
                        )
                    }
                }
            }
        }
    }

    fun loadPreviousTodo() {
        loadTodo(_uiState.value.currentTodoId - 1)
    }

    fun loadNextTodo() {
        loadTodo(_uiState.value.currentTodoId + 1)
    }

    fun retryLoad() {
        loadTodo(_uiState.value.currentTodoId)
    }
}
