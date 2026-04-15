package com.hk.word.gameboosterproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hk.word.gameboosterproject.core.di.ServiceLocator
import com.hk.word.gameboosterproject.presentation.home.HomeRoute
import com.hk.word.gameboosterproject.presentation.home.HomeViewModel
import com.hk.word.gameboosterproject.ui.theme.GameBoosterProjectTheme

/**
 * 应用的主 Activity：承载 Compose 内容并设置全局主题。
 *
 * 职责：
 * - 启用 edge-to-edge 以支持沉浸式布局；
 * - 使用 `GameBoosterProjectTheme` 包裹应用内容；
 * - 通过 `ServiceLocator.homeViewModelFactory` 创建 `HomeViewModel`，并将其传递给 `HomeRoute`。
 *
 * 注意：ViewModel 的创建使用了项目的 ServiceLocator（简单的依赖注入容器），
 * 确保在测试或替换实现时可以提供不同的 factory。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ServiceLocator.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            GameBoosterProjectTheme {
                // 使用 ServiceLocator 提供的 factory 创建 HomeViewModel
                val homeViewModel: HomeViewModel = viewModel(factory = ServiceLocator.homeViewModelFactory)
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    HomeRoute(
                        viewModel = homeViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}