package com.danshuang.AiLive.data.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.danshuang.AiLive.data.LoginRepository


/**
 * Token管理器
 * 负责token的生命周期管理、验证和自动刷新
 */
class TokenManager(
    private val loginRepository: LoginRepository
) {
    
    /**
     * 获取当前token
     */
    val currentToken: Flow<String?> = loginRepository.authToken
    
    /**
     * 检查token是否有效（非空且未过期）
     */
    val isTokenValid: Flow<Boolean> = loginRepository.isTokenValidFlow()
    
    /**
     * 检查用户是否已登录且token有效
     */
    val isLoggedIn: Flow<Boolean> = loginRepository.isLoggedInFlow()
    
    /**
     * 获取token过期时间
     */
    val tokenExpiresTime: Flow<Long?> = loginRepository.expiresTime
    
    /**
     * 检查token是否即将过期（距离过期时间少于指定分钟数）
     */
    fun isTokenExpiringSoon(minutesThreshold: Long = 30): Flow<Boolean> {
        return tokenExpiresTime.map { expiresTime ->
            if (expiresTime == null) return@map false
            val currentTime = System.currentTimeMillis() / 1000
            val timeUntilExpiry = expiresTime - currentTime
            timeUntilExpiry <= minutesThreshold * 60
        }
    }
    
    /**
     * 获取token剩余有效时间（秒）
     */
    fun getTokenRemainingTime(): Flow<Long> {
        return tokenExpiresTime.map { expiresTime ->
            if (expiresTime == null) return@map 0L
            val currentTime = System.currentTimeMillis() / 1000
            maxOf(0L, expiresTime - currentTime)
        }
    }
    
    /**
     * 刷新token（当前简单实现，实际项目中可能需要调用刷新接口）
     */
    suspend fun refreshToken(): Boolean {
        // TODO: 实现token刷新逻辑
        // 这里应该调用服务器的token刷新接口
        // 并更新本地存储的token信息
        return false
    }
    
    /**
     * 清除token信息
     */
    suspend fun clearToken() {
        loginRepository.clearLoginInfo()
    }
    
    /**
     * 保存新的token信息
     */
    suspend fun saveToken(token: String, expiresTime: UInt) {
        val currentUser = loginRepository.loggedInUser
        // 这里需要获取当前用户的手机号，实际实现中可能需要调整
        // 暂时使用空字符串，实际使用时需要传入正确的手机号
        loginRepository.saveLoginInfo(token, "", expiresTime)
    }
    
    /**
     * 验证token格式是否正确
     */
    fun isTokenFormatValid(token: String?): Boolean {
        return !token.isNullOrBlank() && token.length > 10
    }
}