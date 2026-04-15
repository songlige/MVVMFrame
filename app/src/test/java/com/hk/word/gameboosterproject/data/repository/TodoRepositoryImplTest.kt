package com.hk.word.gameboosterproject.data.repository

import com.hk.word.gameboosterproject.core.network.NetworkException
import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.data.local.dao.TodoDao
import com.hk.word.gameboosterproject.data.local.entity.TodoEntity
import com.hk.word.gameboosterproject.data.remote.api.TodoRemoteDataSource
import com.hk.word.gameboosterproject.data.remote.dto.TodoDto
import com.hk.word.gameboosterproject.domain.repository.TodoDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class TodoRepositoryImplTest {
    @Test
    fun `returns cached todo when network request fails and cache is fresh`() = runBlocking {
        val repository = TodoRepositoryImpl(
            remoteDataSource = FakeTodoRemoteDataSource(
                todoResult = NetworkResult.Error(NetworkException.ConnectionError(IOException("offline")))
            ),
            todoDao = FakeTodoDao(
                initialTodos = listOf(
                    TodoEntity(
                        id = 7,
                        title = "cached todo",
                        completed = true,
                        cachedAt = 9_000L
                    )
                )
            ),
            cacheTtlMs = 2_000L,
            currentTimeMillis = { 10_000L }
        )

        val result = repository.getTodo(7)

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(7, data.todo.id)
        assertEquals("cached todo", data.todo.title)
        assertTrue(data.source is TodoDataSource.LocalCache)
    }

    @Test
    fun `returns original error when network request fails and cache is expired`() = runBlocking {
        val networkError = NetworkResult.Error(NetworkException.ConnectionError(IOException("offline")))
        val repository = TodoRepositoryImpl(
            remoteDataSource = FakeTodoRemoteDataSource(todoResult = networkError),
            todoDao = FakeTodoDao(
                initialTodos = listOf(
                    TodoEntity(
                        id = 7,
                        title = "stale todo",
                        completed = true,
                        cachedAt = 7_000L
                    )
                )
            ),
            cacheTtlMs = 2_000L,
            currentTimeMillis = { 10_000L }
        )

        val result = repository.getTodo(7)

        assertTrue(result is NetworkResult.Error)
        assertEquals(networkError.exception, (result as NetworkResult.Error).exception)
    }

    @Test
    fun `stores remote todo into cache on success`() = runBlocking {
        val todoDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(
            remoteDataSource = FakeTodoRemoteDataSource(
                todoResult = NetworkResult.Success(
                    TodoDto(
                        id = 3,
                        title = "remote todo",
                        completed = false
                    )
                )
            ),
            todoDao = todoDao
        )

        val result = repository.getTodo(3)

        assertTrue(result is NetworkResult.Success)
        assertEquals(3, todoDao.storedTodos.single().id)
        assertEquals("remote todo", todoDao.storedTodos.single().title)
    }

    @Test
    fun `returns remote todo list and caches all items on success`() = runBlocking {
        val todoDao = FakeTodoDao()
        val repository = TodoRepositoryImpl(
            remoteDataSource = FakeTodoRemoteDataSource(
                todoListResult = NetworkResult.Success(
                    listOf(
                        TodoDto(id = 1, title = "first todo", completed = false),
                        TodoDto(id = 2, title = "second todo", completed = true)
                    )
                )
            ),
            todoDao = todoDao,
            currentTimeMillis = { 5_000L }
        )

        val result = repository.getTodos()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(2, data.todos.size)
        assertEquals("first todo", data.todos.first().title)
        assertTrue(data.source is TodoDataSource.Remote)
        assertEquals(2, todoDao.storedTodos.size)
        assertEquals(5_000L, todoDao.storedTodos.first().cachedAt)
    }

    @Test
    fun `returns cached todo list when list request fails and cache is fresh`() = runBlocking {
        val repository = TodoRepositoryImpl(
            remoteDataSource = FakeTodoRemoteDataSource(
                todoListResult = NetworkResult.Error(
                    NetworkException.ConnectionError(IOException("offline"))
                )
            ),
            todoDao = FakeTodoDao(
                initialTodos = listOf(
                    TodoEntity(id = 1, title = "cached first", completed = false, cachedAt = 9_000L),
                    TodoEntity(id = 2, title = "cached second", completed = true, cachedAt = 9_000L)
                )
            ),
            cacheTtlMs = 2_000L,
            currentTimeMillis = { 10_000L }
        )

        val result = repository.getTodos()

        assertTrue(result is NetworkResult.Success)
        val data = (result as NetworkResult.Success).data
        assertEquals(2, data.todos.size)
        assertEquals("cached first", data.todos.first().title)
        assertTrue(data.source is TodoDataSource.LocalCache)
    }
}

private class FakeTodoRemoteDataSource(
    private val todoResult: NetworkResult<TodoDto> = NetworkResult.Error(
        NetworkException.ConnectionError(IOException("todo result not configured"))
    ),
    private val todoListResult: NetworkResult<List<TodoDto>> = NetworkResult.Error(
        NetworkException.ConnectionError(IOException("todo list result not configured"))
    )
) : TodoRemoteDataSource {
    override suspend fun fetchTodo(todoId: Int): NetworkResult<TodoDto> = todoResult

    override suspend fun fetchTodos(): NetworkResult<List<TodoDto>> = todoListResult
}

private class FakeTodoDao(
    initialTodos: List<TodoEntity> = emptyList()
) : TodoDao {
    var storedTodos: List<TodoEntity> = initialTodos

    override suspend fun getTodoById(todoId: Int): TodoEntity? {
        return storedTodos.firstOrNull { it.id == todoId }
    }

    override suspend fun getAllTodos(): List<TodoEntity> {
        return storedTodos.sortedBy { it.id }
    }

    override suspend fun insertTodo(todo: TodoEntity) {
        storedTodos = storedTodos
            .filterNot { it.id == todo.id }
            .plus(todo)
            .sortedBy { it.id }
    }

    override suspend fun insertTodos(todos: List<TodoEntity>) {
        storedTodos = todos.sortedBy { it.id }
    }
}
