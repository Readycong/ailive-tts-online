package com.danshuang.AiLive.data.model

// 绑定步骤枚举
enum class BindingStep {
    URL_INPUT,      // 步骤一：URL输入
    ACCOUNT_PREVIEW // 步骤二：账户预览
}

// 信息提取状态枚举
enum class ExtractionStatus {
    IDLE,           // 空闲状态
    LOADING,        // 加载中
    SUCCESS,        // 提取成功
    ERROR           // 提取失败
}

// 用户账户信息
data class UserAccountInfo(
    val avatar: String = "",
    val nickname: String = "",
    val userId: String = "",
    val secUid: String = "",
    val homeUrl: String = "",
    val isLive: Boolean = false
)

// 绑定结果数据类
data class BindingResult(
    val success: Boolean,
    val message: String
)

// 登录状态枚举
enum class LoginState {
    LOGGED_IN,      // 已登录
    NOT_LOGGED_IN,  // 未登录
    LOGGING_IN      // 登录中
}