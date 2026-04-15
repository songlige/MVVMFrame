package com.hk.word.gameboosterproject.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 首页路由层 Composable，负责从 ViewModel 订阅状态并将它传递给展示层 `HomeScreen`。
 *
 * - 会使用 `viewModel.uiState` 的生命周期安全收集 (`collectAsStateWithLifecycle`)。
 * - 首次进入时自动触发列表加载，并将搜索、筛选、刷新事件透传给展示层。
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
    LaunchedEffect(Unit) {
        viewModel.loadTodos()
    }
    HomeScreen(
        state = state,
        onRefreshClick = viewModel::loadTodos,
        onRetryClick = viewModel::retryLoad,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onFilterChange = viewModel::onFilterChange,
        modifier = modifier
    )
}

/**
 * 首页的展示层 Composable，基于传入的 [HomeUiState] 渲染 UI。
 *
 * 视图行为：
 * - 支持标题关键字搜索与完成状态筛选；
 * - 根据内容状态显示加载态、错误态、空结果态或列表内容；
 * - 点击按钮可触发刷新和失败重试。
 *
 * @param state 当前 UI 状态，包含列表数据、搜索词、筛选值、`loading`、`errorMessage` 等字段
 * @param modifier 可选的 [Modifier]，用于外层布局调整
 */
@Composable
private fun HomeScreen(
    state: HomeUiState,
    onRefreshClick: () -> Unit,
    onRetryClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (TodoCompletionFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = "Todo 列表", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = state.resultSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(
                onClick = onRefreshClick,
                enabled = !state.loading
            ) {
                Text(text = if (state.loading) "加载中..." else "刷新")
            }
        }
        if (state.statusMessage != null) {
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("搜索标题") },
            singleLine = true
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TodoFilterOptionButton(
                text = TodoCompletionFilter.ALL.label,
                selected = state.selectedFilter == TodoCompletionFilter.ALL,
                onClick = { onFilterChange(TodoCompletionFilter.ALL) }
            )
            TodoFilterOptionButton(
                text = TodoCompletionFilter.COMPLETED.label,
                selected = state.selectedFilter == TodoCompletionFilter.COMPLETED,
                onClick = { onFilterChange(TodoCompletionFilter.COMPLETED) }
            )
            TodoFilterOptionButton(
                text = TodoCompletionFilter.INCOMPLETE.label,
                selected = state.selectedFilter == TodoCompletionFilter.INCOMPLETE,
                onClick = { onFilterChange(TodoCompletionFilter.INCOMPLETE) }
            )
        }
        when {
            state.loading && !state.hasLoadedOnce -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("正在加载 Todo 列表...")
                    }
                }
            }

            state.errorMessage != null && state.allTodos.isEmpty() -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "加载失败",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = state.errorMessage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRetryClick) {
                            Text(text = "重新获取列表")
                        }
                    }
                }
            }

            state.hasContent -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.visibleTodos,
                        key = { todo -> todo.id }
                    ) { todo ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "#${todo.id} ${todo.title}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (todo.completed) "状态：已完成" else "状态：未完成",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (todo.completed) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            state.hasLoadedOnce -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (state.hasActiveFilters) "没有符合条件的 Todo" else "暂无 Todo 数据",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (state.hasActiveFilters) {
                                "试试调整搜索词或切换筛选条件。"
                            } else {
                                "点击刷新后会重新从网络拉取列表。"
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.TodoFilterOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = text)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = text)
        }
    }
}
