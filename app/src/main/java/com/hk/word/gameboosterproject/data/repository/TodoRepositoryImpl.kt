package com.hk.word.gameboosterproject.data.repository

import com.hk.word.gameboosterproject.core.network.NetworkResult
import com.hk.word.gameboosterproject.data.remote.api.TodoApiService
import com.hk.word.gameboosterproject.domain.model.Todo
import com.hk.word.gameboosterproject.domain.repository.TodoRepository

/**
 * Todo 仓库的实现：通过注入的 [TodoApiService] 拉取远端数据，
 * 并将远端的 DTO 映射为领域层的 [Todo] 对象。
 *
 * - 在成功 (NetworkResult.Success) 情况下，将 DTO 转换为领域模型并返回 Success
 * - 在错误 (NetworkResult.Error) 情况下，直接透传错误结果
 *
 * 这样上层（例如 UseCase / ViewModel）可以统一处理 [NetworkResult] 的成功/失败分支。
 *
 * @param apiService 用于实际执行网络请求的 API 服务
 */
class TodoRepositoryImpl(
    private val apiService: TodoApiService
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
    override suspend fun getTodo(): NetworkResult<Todo> {
        return when (val result = apiService.fetchTodo()) {
            is NetworkResult.Success -> {
                val dto = result.data
                NetworkResult.Success(
                    Todo(
                        id = dto.id,
                        title = dto.title,
                        completed = dto.completed
                    )
                )
            }

            is NetworkResult.Error -> result
        }
    }
}
