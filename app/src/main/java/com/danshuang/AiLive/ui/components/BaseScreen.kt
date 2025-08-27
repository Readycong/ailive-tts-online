package com.danshuang.AiLive.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.danshuang.AiLive.service.BaseScreenManager
import com.danshuang.AiLive.service.LoginCheckResult
import com.danshuang.AiLive.service.rememberBaseScreenManager
import com.danshuang.AiLive.service.rememberLoginState
import com.danshuang.AiLive.ui.screens.LoginScreen

/**
 * BaseScreen组件，为所有需要登录保护的界面提供统一的登录状态管理
 * 
 * @param requireLogin 是否需要登录才能访问，默认为true
 * @param onLoginSuccess 登录成功后的回调，默认为空
 * @param onSessionExpired 会话过期时的回调，默认为空
 * @param loadingContent 登录状态检查时显示的加载内容
 * @param content 登录验证通过后显示的主要内容
 */
@Composable
fun BaseScreen(
    requireLogin: Boolean = true,
    onLoginSuccess: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    loadingContent: @Composable () -> Unit = { DefaultLoadingContent() },
    content: @Composable () -> Unit
) {
    if (!requireLogin) {
        // 不需要登录验证，直接显示内容
        content()
        return
    }
    
    val loginState by rememberLoginState(
        onLoginExpired = onSessionExpired.takeIf { onSessionExpired !== {} }
    )
    
    when (loginState) {
        is LoginCheckResult.Loading -> {
            loadingContent()
        }
        is LoginCheckResult.NotLoggedIn -> {
            LoginScreen(
                onLoginSuccess = onLoginSuccess
            )
        }
        is LoginCheckResult.LoggedIn -> {
            content()
        }
    }
}

/**
 * 默认的加载内容
 */
@Composable
private fun DefaultLoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
//            Text(
//                text = "检查登录状态...",
//                style = MaterialTheme.typography.bodyMedium
//            )
        }
    }
}

/**
 * 带有登录状态监控的屏幕组件
 * 自动监控登录状态变化，当用户注销或会话过期时自动跳转到登录页
 * 
 * @param onLogout 注销回调
 * @param content 主要内容
 */
@Composable
fun ProtectedScreen(
    onLogout: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val baseScreenManager = rememberBaseScreenManager()
    val isLoggedIn by baseScreenManager.getLoginStatusFlow().collectAsState(initial = true)
    
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            onLogout()
        }
    }
    
    if (isLoggedIn) {
        content()
    } else {
        LoginScreen(
            onLoginSuccess = { /* 在ProtectedScreen中，登录成功后会自动重新组合 */ }
        )
    }
}

/**
 * 用于包装现有Screen的高阶组件
 * 为现有的Screen添加登录保护功能，无需修改原有代码结构
 * 
 * @param screenName 屏幕名称，用于调试和日志
 * @param requireLogin 是否需要登录保护
 * @param onLoginSuccess 登录成功回调
 * @param onLogout 注销回调
 * @param onSessionExpired 会话过期回调
 * @param content 原有的Screen内容
 */
@Composable
fun WithLoginProtection(
    screenName: String = "Screen",
    requireLogin: Boolean = true,
    onLoginSuccess: () -> Unit = {},
    onLogout: () -> Unit = {},
    onSessionExpired: () -> Unit = {},
    content: @Composable () -> Unit
) {
    if (!requireLogin) {
        content()
        return
    }
    
    BaseScreen(
        requireLogin = true,
        onLoginSuccess = onLoginSuccess,
        onSessionExpired = {
            onSessionExpired()
            onLogout() // 会话过期时也触发注销逻辑
        },
        content = {
            ProtectedScreen(
                onLogout = onLogout,
                content = content
            )
        }
    )
}