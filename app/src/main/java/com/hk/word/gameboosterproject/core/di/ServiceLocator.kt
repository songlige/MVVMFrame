package com.hk.word.gameboosterproject.core.di

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.hk.word.gameboosterproject.BuildConfig
import com.hk.word.gameboosterproject.core.network.HttpClient
import com.hk.word.gameboosterproject.core.network.HttpLogger
import com.hk.word.gameboosterproject.data.local.db.AppDatabase
import com.hk.word.gameboosterproject.data.remote.api.TodoApiService
import com.hk.word.gameboosterproject.data.repository.TodoRepositoryImpl
import com.hk.word.gameboosterproject.domain.usecase.GetTodoListUseCase
import com.hk.word.gameboosterproject.presentation.home.HomeViewModel

/**
 * ServiceLocator: 简单的依赖定位器（手写 DI 容器）
 *
 * 说明：
 * - 提供网络客户端、API、仓库、用例和 ViewModelFactory 的单例实例。
 * - 使用 Kotlin 的 lazy 保证按需初始化（线程安全），适合小型项目或示例。
 * - 若项目需求增长，建议迁移到 Hilt/Dagger 或手动可注入的工厂以便更好地测试。
 *
 * 用法示例：
 * val factory = ServiceLocator.homeViewModelFactory
 * val vm = ViewModelProvider(this, factory).get(HomeViewModel::class.java)
 */
object ServiceLocator {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    // Http 客户端，带可选的日志记录器（仅在 DEBUG 模式下启用）
    private val httpClient by lazy {
        HttpClient(
            logger = if (BuildConfig.DEBUG) {
                // 将网络日志输出到 Logcat，标签为 "HttpClient"
                HttpLogger { message -> Log.d("HttpClient", message) }
            } else {
                null
            }
        )
    }

    private val appDatabase by lazy {
        val context = checkNotNull(appContext) {
            "ServiceLocator must be initialized before accessing AppDatabase."
        }
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "gamebooster.db"
        ).build()
    }

    private val todoDao by lazy { appDatabase.todoDao() }

    // API 服务：使用同一个 httpClient 创建（单例）
    private val todoApiService by lazy { TodoApiService(httpClient) }

    // 仓库实现：负责调用 API 并转换为域层需要的数据结构
    private val todoRepository by lazy { TodoRepositoryImpl(todoApiService, todoDao) }

    private val getTodoListUseCase by lazy { GetTodoListUseCase(todoRepository) }

    // 为 HomeViewModel 提供一个简单的 ViewModelProvider.Factory
    // 通过 factory 可以在 Activity/Fragment 中安全创建 HomeViewModel
    val homeViewModelFactory: ViewModelProvider.Factory
        get() = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(getTodoListUseCase) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
}
