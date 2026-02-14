package com.danshuang.AiLive.navigation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.SettingsInputComponent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.danshuang.AiLive.service.BaseScreenManager
import com.danshuang.AiLive.service.rememberBaseScreenManager
import com.danshuang.AiLive.ui.components.BaseScreen
import com.danshuang.AiLive.ui.screens.LoginScreen
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.ui.theme.backgroundLight
import com.danshuang.AiLive.util.GlobalEventManager

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    data object Home : BottomNavItem("主页", Icons.Default.Home, "home")
    data object Model : BottomNavItem("大模型", Icons.Default.Person, "model")
    data object AILive : BottomNavItem("AI直播", Icons.Default.PlayArrow, "ai_live")
    data object Knowledge : BottomNavItem("知识库", Icons.Default.Search, "knowledge")
    data object Profile : BottomNavItem("设置", Icons.Default.SettingsInputComponent, "profile")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val baseScreenManager = rememberBaseScreenManager()
    val isLoggedIn by baseScreenManager.getLoginStatusFlow().collectAsState(initial = false)
    
    // 注册全局事件接收器
    val unauthorizedErrorReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == GlobalEventManager.ACTION_UNAUTHORIZED_ERROR) {
                    // 收到未授权错误事件，清除登录状态并跳转到登录页
                    CoroutineScope(Dispatchers.Main).launch {
                        baseScreenManager.logout()
                    }
                }
            }
        }
    }
    
    // 注册和注销广播接收器
    LaunchedEffect(Unit) {
        val filter = IntentFilter(GlobalEventManager.ACTION_UNAUTHORIZED_ERROR)
        LocalBroadcastManager.getInstance(context).registerReceiver(unauthorizedErrorReceiver, filter)
    }
    
    // 在组件销毁时注销接收器
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(unauthorizedErrorReceiver)
        }
    }
    
    BaseScreen(
        requireLogin = true,
        onLoginSuccess = {
            // 登录成功后的回调可以在这里处理额外逻辑
        }
    ) {
        MainAppNavigation(
            onLogout = {
                // 注销时清空登录状态，BaseScreen会自动跳转到登录页
            }
        )
    }
}

@Composable
fun MainAppNavigation(
    onLogout: () -> Unit = {}
) {
    val navigationState = rememberNavigationState()
    val baseScreenManager = rememberBaseScreenManager()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Model,
        BottomNavItem.AILive,
        BottomNavItem.Knowledge,
        BottomNavItem.Profile
    )

    // 处理系统返回键
    BackHandler(enabled = navigationState.currentScreen !is Screen.Home) {
        when (navigationState.currentScreen) {
            is Screen.ContentEdit, is Screen.LiveScript, 
            is Screen.GreetingSettings, is Screen.NicknameRestrictions,
            is Screen.CommentRestrictions, is Screen.VoiceSettings,
            is Screen.VoiceCloning, is Screen.ChangePassword,
            is Screen.Help, is Screen.AccountBinding, is Screen.CardActivation,
            is Screen.ProductList, is Screen.ProductEdit,
            is Screen.WebView -> {
                // 这些页面有明确的返回页面，使用navigateUp
                navigationState.navigateUp()
            }
            is Screen.AILive, is Screen.Knowledge, is Screen.Model, is Screen.Profile -> {
                // 其他主要页面返回到主页
                navigationState.navigateTo(Screen.Home)
            }
            else -> {
                // 其他情况返回到主页
                navigationState.navigateTo(Screen.Home)
            }
        }
    }

    Scaffold(
        bottomBar = {
            // 在内容编辑页面、话术页面、招呼设置页面、限制设置页面、声音克隆页面、AI直播页面和卡密激活页面隐藏底部导航
            if (navigationState.currentScreen !is Screen.ContentEdit && 
                navigationState.currentScreen !is Screen.LiveScript &&
                navigationState.currentScreen !is Screen.GreetingSettings &&
                navigationState.currentScreen !is Screen.NicknameRestrictions &&
                navigationState.currentScreen !is Screen.CommentRestrictions &&
                navigationState.currentScreen !is Screen.VoiceSettings &&
                navigationState.currentScreen !is Screen.VoiceCloning &&
                navigationState.currentScreen !is Screen.VoiceManagement &&
                navigationState.currentScreen !is Screen.AILive &&
                navigationState.currentScreen !is Screen.WebView &&
                navigationState.currentScreen !is Screen.ProductList &&
                navigationState.currentScreen !is Screen.ProductEdit &&
                navigationState.currentScreen !is Screen.CardActivation) {
                NavigationBar(
                    containerColor = backgroundLight,
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = when (navigationState.currentScreen) {
                                is Screen.Home -> index == 0
                                is Screen.Model -> index == 1
                                is Screen.AILive -> index == 2
                                is Screen.Knowledge -> index == 3
                                is Screen.Profile -> index == 4
                                else -> false
                            },
                            onClick = {
                                when (index) {
                                    0 -> navigationState.navigateTo(Screen.Home)
                                    1 -> navigationState.navigateTo(Screen.Model)
                                    2 -> navigationState.navigateTo(Screen.AILive)
                                    3 -> navigationState.navigateTo(Screen.Knowledge)
                                    4 -> navigationState.navigateTo(Screen.Profile)
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            ScreenNavigation(
                navigationState = navigationState,
                onLogout = {
                    // 使用BaseScreenManager处理注销
                    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                        baseScreenManager.logout()
                    }
                    onLogout()
                }
            )
        }
    }
}