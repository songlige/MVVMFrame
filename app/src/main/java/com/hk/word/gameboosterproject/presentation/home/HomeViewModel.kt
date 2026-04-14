package com.hk.word.gameboosterproject.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.usecase.GetTodoUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 首页 ViewModel：负责通过 [GetTodoUseCase] 加载 Todo 并将结果暴露为 UI 状态。
 *
 * 该 ViewModel 管理一个 [HomeUiState]，包含 loading、todoText 和 errorMessage 等字段。
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
     * 异步加载 Todo 并更新 UI 状态：
     * - 请求开始时设置 loading = true 并清除 errorMessage
     * - 成功时填充 todoText 并将 loading 设为 false
     * - 失败时将 errorMessage 设为异常消息或默认提示，并将 loading 设为 false
     *
     * 使用 [viewModelScope] 启动协程，确保生命周期安全。
     */
    fun loadTodo() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, errorMessage = null) }
            when (val result = getTodoUseCase()) {
                is NetworkResult.Success -> {
                    val todo = result.data
                    _uiState.update {
                        it.copy(
                            loading = false,
                            todoText = "#${todo.id} ${todo.title}（完成：${todo.completed}）"
                        )
                    }
                }

                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            loading = false,
                            errorMessage = result.exception.message ?: "未知网络错误"
                        )
                    }
                }
            }
        }
    }
}
