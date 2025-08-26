package com.danshuang.AiLive.data.model

import androidx.annotation.Keep
import java.util.Date

/**
 * API响应的统一格式
 */
@Keep
data class ServerResponse<T>(
    val code: Int,
    val msg: String,
    val data: T? = null
) {
    val isSuccess: Boolean
        get() = code == 1

    val isError: Boolean
        get() = !isSuccess
}

/**
 * 登录用户信息
 */
@Keep
data class LoggedInUser(
    val token: String,
    val mobile: String,
    val expiresTime: UInt,
    val userProfile: UserProfile
) {
    val isTokenExpired: Boolean
        get() = System.currentTimeMillis() / 1000 > expiresTime.toLong()
}

/**
 * 用户个人信息
 */
@Keep
data class UserProfile(
    val nickname: String = "",
    val headimg: String = "",
    val balance: String = "",
    val point: String = ""
) {
    val displayBalance: String
        get() = if (balance.isNotEmpty() && point.isNotEmpty()) {
            "￥$balance / $point"
        } else ""
}

/**
 * 登录请求结果的封装类
 */
sealed class LoginResult {
    data class Success(val user: LoggedInUser) : LoginResult()
    data class Error(val exception: Throwable) : LoginResult()
    data object Loading : LoginResult()
}

/**
 * 抖音账号绑定信息
 */
@Keep
data class DouyinAccount(
    val douyinId: String,
    val nickname: String,
    val avatar: String,
    val roomId: String,
    val bindTime: Long = System.currentTimeMillis()
)

/**
 * 知识库条目
 */
@Keep
data class KnowledgeItem(
    val id: String,
    val typeId: String,
    val question: String,
    val answer: String,
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * AI评论数据
 */
@Keep
data class AiComment(
    val content: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 配置项数据
 */
@Keep
data class ConfigItem(
    val key: String,
    val value: String,
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 在线用户信息
 */
@Keep
data class OnlineUser(
    val userId: String,
    val nickname: String,
    val avatar: String,
    val joinTime: Long = System.currentTimeMillis()
)