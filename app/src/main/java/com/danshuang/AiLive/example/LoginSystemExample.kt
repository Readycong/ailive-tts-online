package com.danshuang.AiLive.example

import android.content.Context
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.viewmodel.LoginViewModel
import com.danshuang.AiLive.viewmodel.LoginState
import com.danshuang.AiLive.ui.screens.LoginScreen

/**
 * 更新后的登录系统使用示例
 * 
 * 主要变更：
 * 1. LoginState.Success 现在包含完整的 LoggedInUser 对象
 * 2. 集成了真实的 API 调用
 * 3. 自动的数据持久化管理
 * 4. Token 生命周期管理
 */

/**
 * 在 Application 中初始化全局组件的示例
 */
class LoginSystemConfiguration(private val context: Context) {
    
    // 从strings.xml中读取API服务器地址
    private val apiBaseUrl = context.getString(com.danshuang.AiLive.R.string.api_base_url)
    
    // 初始化核心组件
    val loginRepository = LoginRepository(context)
    val tokenManager = TokenManager(loginRepository)
    val apiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
    
    /**
     * 检查当前登录状态
     */
    suspend fun checkLoginStatus(): Boolean {
        return apiUtils.isLoggedIn()
    }
    
    /**
     * 获取当前用户信息
     */
    suspend fun getCurrentUser() = apiUtils.getCurrentUser()
    
    /**
     * 全局注销
     */
    suspend fun globalLogout() {
        apiUtils.logout()
    }
}

/**
 * 如何在 Compose 中使用更新后的登录系统
 */
@Composable
fun ExampleLoginFlow() {
    val viewModel: LoginViewModel = viewModel()
    val loginState by viewModel.loginState.collectAsState()
    
    // 监听登录状态变化
    LaunchedEffect(loginState) {
        when (val currentState = loginState) {
            is LoginState.Success -> {
                val user = currentState.user
                
                // 现在可以访问完整的用户信息
                println("登录成功:")
                println("Token: ${user.token}")
                println("手机号: ${user.mobile}")
                println("昵称: ${user.userProfile.nickname}")
                println("头像: ${user.userProfile.headimg}")
                println("余额显示: ${user.userProfile.displayBalance}")
                println("Token过期时间: ${user.expiresTime}")
                println("Token是否过期: ${user.isTokenExpired}")
            }
            is LoginState.Error -> {
                println("登录失败: ${currentState.message}")
            }
            is LoginState.Loading -> {
                println("登录中...")
            }
            is LoginState.Idle -> {
                println("等待登录")
            }
        }
    }
    
    LoginScreen(
        viewModel = viewModel,
        onLoginSuccess = {
            // 登录成功后的处理
            // 现在在 LoginState.Success 中已经包含了完整的用户信息
        }
    )
}

/**
 * 在其他 ViewModel 中使用登录信息的示例
 */
class ExampleViewModel(
    private val loginRepository: LoginRepository,
    private val apiUtils: ServerApiUtilsV2
) {
    
    /**
     * 获取当前登录用户
     */
    suspend fun loadCurrentUser() {
        val user = apiUtils.getCurrentUser()
        if (user != null) {
            println("当前用户: ${user.userProfile.nickname}")
            
            // 检查token是否即将过期
            if (user.isTokenExpired) {
                println("Token已过期，需要重新登录")
            }
        }
    }
    
    /**
     * 更新用户资料
     */
    suspend fun updateUserProfile() {
        val memberInfoResult = apiUtils.refreshUserInfo()
        if (memberInfoResult.isSuccess) {
            println("用户信息已更新")
        }
    }
    
    /**
     * 执行需要登录的操作
     */
    suspend fun performAuthenticatedAction() {
        if (!apiUtils.isLoggedIn()) {
            println("用户未登录，需要先登录")
            return
        }
        
        // 执行需要认证的操作...
    }
}

/**
 * 数据持久化的使用示例
 */
@Composable
fun ExampleLoginStatusMonitor() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    
    // 监听登录状态
    val isLoggedIn by loginRepository.isLoggedInFlow().collectAsState(initial = false)
    val currentUser by loginRepository.loggedInUser.collectAsState(initial = null)
    
    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            println("用户已登录")
            currentUser?.let { user ->
                println("用户信息: ${user.userProfile.nickname}")
            }
        } else {
            println("用户未登录")
        }
    }
}

/**
 * 错误处理示例
 */
@Composable 
fun ExampleErrorHandling() {
    val viewModel: LoginViewModel = viewModel()
    val loginState by viewModel.loginState.collectAsState()
    
    when (val currentState = loginState) {
        is LoginState.Error -> {
            // 显示错误信息
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { androidx.compose.material3.Text("登录失败") },
                text = { androidx.compose.material3.Text(currentState.message) },
                confirmButton = {
                    androidx.compose.material3.TextButton(
                        onClick = { viewModel.clearError() }
                    ) {
                        androidx.compose.material3.Text("确定")
                    }
                }
            )
        }
        else -> { /* 其他状态处理 */ }
    }
}