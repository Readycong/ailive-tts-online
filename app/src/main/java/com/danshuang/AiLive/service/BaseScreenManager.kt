package com.danshuang.AiLive.service

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.util.UnauthorizedErrorHandler
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull

/**
 * BaseScreen管理器，用于统一管理所有界面的登录状态验证
 * 提供登录状态检查、会话监控和统一的登录跳转逻辑
 */
class BaseScreenManager private constructor(
    private val context: Context
) {
    private val loginRepository = LoginRepository(context)
    
    companion object {
        @Volatile
        private var INSTANCE: BaseScreenManager? = null
        
        fun getInstance(context: Context): BaseScreenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BaseScreenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 获取登录状态Flow
     */
    fun getLoginStatusFlow() = loginRepository.isLoggedInFlow()
    
    /**
     * 获取认证令牌Flow
     */
    fun getAuthTokenFlow() = loginRepository.authToken
    
    /**
     * 获取用户手机号Flow
     */
    fun getUserPhoneFlow() = loginRepository.userPhone
    
    /**
     * 检查当前是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return loginRepository.authToken.firstOrNull() != null
    }
    
    /**
     * 清除登录信息（注销）
     */
    suspend fun logout() {
        loginRepository.clearLoginInfo()
    }
    
    /**
     * 处理未授权错误（401）
     * 停止所有服务，清除登录状态
     */
    fun handleUnauthorizedError() {
        UnauthorizedErrorHandler.getInstance().handleUnauthorizedError(context)
    }
    
    /**
     * 保存登录信息
     */
    suspend fun saveLoginInfo(token: String, phone: String) {
        loginRepository.saveLoginInfo(token, phone)
    }
}

/**
 * 用于在Compose中获取BaseScreenManager实例的组合函数
 */
@Composable
fun rememberBaseScreenManager(): BaseScreenManager {
    val context = LocalContext.current
    return remember { BaseScreenManager.getInstance(context) }
}

/**
 * 登录状态检查结果
 */
sealed class LoginCheckResult {
    data object LoggedIn : LoginCheckResult()
    data object NotLoggedIn : LoginCheckResult()
    data object Loading : LoginCheckResult()
}

/**
 * 用于检查登录状态的组合函数
 * 自动监控登录状态变化，当用户注销或会话过期时触发回调
 */
@Composable
fun rememberLoginState(
    onLoginExpired: (() -> Unit)? = null
): State<LoginCheckResult> {
    val baseScreenManager = rememberBaseScreenManager()
    val loginState by baseScreenManager.getLoginStatusFlow().collectAsState(initial = null)
    
    // 监控登录状态变化
    LaunchedEffect(loginState) {
        if (loginState == false && onLoginExpired != null) {
            onLoginExpired()
        }
    }
    
    return remember(loginState) {
        derivedStateOf {
            when (loginState) {
                null -> LoginCheckResult.Loading
                true -> LoginCheckResult.LoggedIn
                false -> LoginCheckResult.NotLoggedIn
            }
        }
    }
}

/**
 * 用于全局登录状态监控的组合函数
 * 在应用级别监控登录状态，确保会话过期时自动处理
 */
@Composable
fun GlobalLoginMonitor(
    onSessionExpired: () -> Unit
) {
    val baseScreenManager = rememberBaseScreenManager()
    val isLoggedIn by baseScreenManager.getLoginStatusFlow().collectAsState(initial = true)
    
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            onSessionExpired()
        }
    }
}