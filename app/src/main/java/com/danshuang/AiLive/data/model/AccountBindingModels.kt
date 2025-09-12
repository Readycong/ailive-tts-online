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

// 模拟用户账户信息
data class UserAccountInfo(
    val avatar: String = "https://via.placeholder.com/80",
    val nickname: String = "示例用户",
    val isLive: Boolean = false,
    val userId: String = "" // 添加用户ID字段
)

// 绑定结果数据类
data class BindingResult(
    val success: Boolean,
    val message: String
)