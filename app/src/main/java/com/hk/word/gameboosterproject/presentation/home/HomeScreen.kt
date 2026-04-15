package com.hk.word.gameboosterproject.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
 * - 将加载、重试和切换编号事件透传给展示层。
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
        onPreviousClick = viewModel::loadPreviousTodo,
        onNextClick = viewModel::loadNextTodo,
        onRetryClick = viewModel::retryLoad,
        modifier = modifier
    )
}

/**
 * 首页的展示层 Composable，基于传入的 [HomeUiState] 渲染 UI。
 *
 * 视图行为：
 * - 显示当前 Todo 编号；
 * - 根据内容状态显示空状态、加载态、成功态或错误态；
 * - 点击按钮可触发加载、重试和切换上下一个 Todo；
 * - 根据 `loading` 控制按钮可用性与加载指示器显示。
 *
 * @param state 当前 UI 状态，包含 `currentTodoId`、`todoTitle`、`loading`、`errorMessage` 等字段
 * @param onLoadClick 当用户点击“加载当前编号”按钮时调用的回调
 * @param modifier 可选的 [Modifier]，用于外层布局调整
 */
@Composable
private fun HomeScreen(
    state: HomeUiState,
    onLoadClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Todo 浏览器", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "当前编号：#${state.currentTodoId}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Card(modifier = Modifier.padding(top = 16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    state.loading -> {
                        CircularProgressIndicator()
                        Text(
                            text = "正在加载编号 #${state.currentTodoId}...",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }

                    state.errorMessage != null -> {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Button(
                            onClick = onRetryClick,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Text(text = "重试当前编号")
                        }
                    }

                    state.hasContent -> {
                        Text(
                            text = state.todoTitle.orEmpty(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (state.statusMessage != null) {
                            Text(
                                text = state.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        Text(
                            text = if (state.completed == true) "状态：已完成" else "状态：未完成",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    else -> {
                        Text(
                            text = "点击下方按钮，加载远程 Todo 数据。",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "你可以继续浏览 1 到 200 号 Todo。",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onPreviousClick,
                enabled = state.canLoadPrevious && !state.loading
            ) {
                Text(text = "上一个")
            }
            Button(
                onClick = onLoadClick,
                enabled = !state.loading
            ) {
                Text(text = if (state.hasContent) "重新加载" else "加载当前编号")
            }
            OutlinedButton(
                onClick = onNextClick,
                enabled = state.canLoadNext && !state.loading
            ) {
                Text(text = "下一个")
            }
        }
    }
}
