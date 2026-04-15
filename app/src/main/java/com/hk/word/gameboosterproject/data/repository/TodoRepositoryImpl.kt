package com.hk.word.gameboosterproject.data.repository

import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.data.local.dao.TodoDao
import com.hk.word.gameboosterproject.data.local.entity.TodoEntity
import com.hk.word.gameboosterproject.data.local.mapper.toDomain
import com.hk.word.gameboosterproject.data.local.mapper.toEntity
import com.hk.word.gameboosterproject.data.remote.api.TodoRemoteDataSource
import com.hk.word.gameboosterproject.domain.repository.TodoDataSource
import com.hk.word.gameboosterproject.domain.repository.TodoListLoadResult
import com.hk.word.gameboosterproject.domain.repository.TodoLoadResult
import com.hk.word.gameboosterproject.domain.repository.TodoRepository

/**
 * Todo 仓库的实现：通过注入的 [TodoApiService] 拉取远端数据，
 * 并结合 [TodoDao] 完成本地缓存读写。
 *
 * - 远端成功时：写入 Room 缓存，再返回最新数据
 * - 远端失败时：优先回退到本地缓存；若无缓存则透传网络错误
 *
 * 这样上层（例如 UseCase / ViewModel）可以统一处理 [NetworkResult] 的成功/失败分支。
 *
 * @param remoteDataSource 用于实际执行网络请求的远端数据源
 * @param todoDao 用于读写 Todo 本地缓存的 Room DAO
 */
class TodoRepositoryImpl(
    private val remoteDataSource: TodoRemoteDataSource,
    private val todoDao: TodoDao,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : TodoRepository {
    /**
     * 从远端获取 Todo 并映射为领域模型。
     *
     * 这是一个挂起函数：
     * - 如果 API 返回 [NetworkResult.Success]，会将 DTO 映射为 [Todo] 并返回 [NetworkResult.Success<Todo>]。
     * - 如果 API 返回 [NetworkResult.Error]，会直接返回该错误，以便上层可以读取并展示错误信息。
     *
     * 返回类型：`NetworkResult<Todo>`，上层需根据 Success/Error 做相应处理。
     */
    override suspend fun getTodo(todoId: Int): NetworkResult<TodoLoadResult> {
        return when (val result = remoteDataSource.fetchTodo(todoId)) {
            is NetworkResult.Success -> {
                val dto = result.data
                todoDao.insertTodo(dto.toEntity())
                NetworkResult.Success(
                    TodoLoadResult(
                        todo = dto.toDomain(),
                        source = TodoDataSource.Remote
                    )
                )
            }

            is NetworkResult.Error -> {
                val cachedTodo = todoDao.getTodoById(todoId)
                if (cachedTodo != null && !isCacheExpired(cachedTodo)) {
                    NetworkResult.Success(
                        TodoLoadResult(
                            todo = cachedTodo.toDomain(),
                            source = TodoDataSource.LocalCache(result.exception)
                        )
                    )
                } else {
                    result
                }
            }
        }
    }

    override suspend fun getTodos(): NetworkResult<TodoListLoadResult> {
        return when (val result = remoteDataSource.fetchTodos()) {
            is NetworkResult.Success -> {
                val cachedAt = currentTimeMillis()
                val todoEntities = result.data.map { dto -> dto.toEntity(cachedAt = cachedAt) }
                todoDao.insertTodos(todoEntities)
                NetworkResult.Success(
                    TodoListLoadResult(
                        todos = result.data.map { dto -> dto.toDomain() },
                        source = TodoDataSource.Remote
                    )
                )
            }

            is NetworkResult.Error -> {
                val cachedTodos = todoDao.getAllTodos()
                if (cachedTodos.isNotEmpty() && cachedTodos.all { !isCacheExpired(it) }) {
                    NetworkResult.Success(
                        TodoListLoadResult(
                            todos = cachedTodos.map { entity -> entity.toDomain() },
                            source = TodoDataSource.LocalCache(result.exception)
                        )
                    )
                } else {
                    result
                }
            }
        }
    }

    private fun isCacheExpired(cachedTodo: TodoEntity): Boolean {
        val cacheAgeMs = currentTimeMillis() - cachedTodo.cachedAt
        return cacheAgeMs > cacheTtlMs
    }

    private companion object {
        const val DEFAULT_CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
