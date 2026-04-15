package com.hk.word.gameboosterproject.presentation.home

import com.hk.word.gameboosterproject.core.network.NetworkException
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.domain.model.Todo
import com.hk.word.gameboosterproject.domain.repository.TodoDataSource
import com.hk.word.gameboosterproject.domain.repository.TodoListLoadResult
import com.hk.word.gameboosterproject.domain.repository.TodoLoadResult
import com.hk.word.gameboosterproject.domain.repository.TodoRepository
import com.hk.word.gameboosterproject.domain.usecase.GetTodoListUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTodos populates state from remote result`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            getTodoListUseCase = GetTodoListUseCase(
                FakeTodoRepository(
                    todoListResult = NetworkResult.Success(
                        TodoListLoadResult(
                            todos = listOf(
                                Todo(id = 1, title = "first task", completed = false),
                                Todo(id = 2, title = "second task", completed = true)
                            ),
                            source = TodoDataSource.Remote
                        )
                    )
                )
            )
        )

        viewModel.loadTodos()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.loading)
        assertEquals(2, state.allTodos.size)
        assertEquals(2, state.visibleTodos.size)
        assertEquals("已从网络加载最新列表", state.statusMessage)
        assertTrue(state.hasLoadedOnce)
    }

    @Test
    fun `search and filter narrow visible todos locally`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            getTodoListUseCase = GetTodoListUseCase(
                FakeTodoRepository(
                    todoListResult = NetworkResult.Success(
                        TodoListLoadResult(
                            todos = listOf(
                                Todo(id = 1, title = "alpha task", completed = false),
                                Todo(id = 2, title = "beta task", completed = true),
                                Todo(id = 3, title = "alpha done", completed = true)
                            ),
                            source = TodoDataSource.Remote
                        )
                    )
                )
            )
        )

        viewModel.loadTodos()
        advanceUntilIdle()
        viewModel.onSearchQueryChange("alpha")
        viewModel.onFilterChange(TodoCompletionFilter.COMPLETED)

        val state = viewModel.uiState.value
        assertEquals("alpha", state.searchQuery)
        assertEquals(TodoCompletionFilter.COMPLETED, state.selectedFilter)
        assertEquals(1, state.visibleTodos.size)
        assertEquals("alpha done", state.visibleTodos.single().title)
    }

    @Test
    fun `loadTodos exposes readable error message on failure`() = runTest(dispatcher) {
        val viewModel = HomeViewModel(
            getTodoListUseCase = GetTodoListUseCase(
                FakeTodoRepository(
                    todoListResult = NetworkResult.Error(
                        NetworkException.ConnectionError(IOException("offline"))
                    )
                )
            )
        )

        viewModel.loadTodos()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasLoadedOnce)
        assertTrue(state.errorMessage?.contains("网络") == true)
        assertTrue(state.visibleTodos.isEmpty())
    }
}

private class FakeTodoRepository(
    private val todoListResult: NetworkResult<TodoListLoadResult>
) : TodoRepository {
    override suspend fun getTodo(todoId: Int): NetworkResult<TodoLoadResult> {
        return NetworkResult.Error(NetworkException.ConnectionError(IOException("unused")))
    }

    override suspend fun getTodos(): NetworkResult<TodoListLoadResult> = todoListResult
}
