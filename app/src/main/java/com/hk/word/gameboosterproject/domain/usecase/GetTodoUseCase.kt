package com.hk.word.gameboosterproject.domain.usecase

import com.hk.word.gameboosterproject.domain.repository.TodoRepository

/**
 * 用例：根据编号获取 Todo。
 *
 * 负责协调领域层的获取 Todo 行为；将调用委托给注入的 [TodoRepository]。
 * 该类把仓库结果作为返回值直接透传给调用者（通常为 ViewModel）。
 *
 * @property repository 提供实际数据获取的仓库实现
 */
class GetTodoUseCase(
    private val repository: TodoRepository
) {
    /**
     * 执行用例：异步从仓库获取指定编号的 Todo。
     *
     * 这是一个挂起函数（suspend），并通过 operator fun invoke 使调用方可以像调用函数一样使用本用例：
     * ```
     * val result = getTodoUseCase(1)
     * ```
     *
     * 返回值为仓库的返回类型（例如 `NetworkResult<Todo>`），并不会在此处进行额外封装。
     */
    suspend operator fun invoke(todoId: Int) = repository.getTodo(todoId)
}
