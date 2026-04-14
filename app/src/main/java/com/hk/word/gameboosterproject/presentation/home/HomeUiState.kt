package com.hk.word.gameboosterproject.presentation.home

data class HomeUiState(
    val loading: Boolean = false,
    val todoText: String = "点击加载远程任务",
    val errorMessage: String? = null
)
