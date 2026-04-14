package com.hk.word.gameboosterproject.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 首页路由层 Composable，负责从 ViewModel 订阅状态并将它传递给展示层 `HomeScreen`。
 *
 * - 会使用 `viewModel.uiState` 的生命周期安全收集 (`collectAsStateWithLifecycle`)。
 * - 将点击事件回调 `viewModel::loadTodo` 透传给展示层。
 *
 * @param viewModel 提供 UI 状态与行为的 [HomeViewModel]
 * @param modifier 可选的 [Modifier]，用于外层布局调整
 */
@Composable
fun HomeRoute(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onLoadClick = viewModel::loadTodo,
        modifier = modifier
    )
}

/**
 * 首页的展示层 Composable，基于传入的 [HomeUiState] 渲染 UI。
 *
 * 视图行为：
 * - 显示 `todoText` 文本；
 * - 当 `errorMessage` 不为空时显示错误提示；
 * - 点击按钮会触发 `onLoadClick` 加载远程数据；
 * - 根据 `loading` 控制按钮可用性与加载指示器显示。
 *
 * @param state 当前 UI 状态，包含 `todoText`、`loading`、`errorMessage` 等字段
 * @param onLoadClick 当用户点击“请求远程数据”按钮时调用的回调
 * @param modifier 可选的 [Modifier]，用于外层布局调整
 */
@Composable
private fun HomeScreen(
    state: HomeUiState,
    onLoadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = state.todoText, style = MaterialTheme.typography.titleMedium)
        if (state.errorMessage != null) {
            Text(
                text = state.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Button(
            onClick = onLoadClick,
            enabled = !state.loading,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Text(text = "请求远程数据")
        }
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }
    }
}
