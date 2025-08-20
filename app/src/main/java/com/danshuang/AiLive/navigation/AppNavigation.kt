package com.danshuang.AiLive.navigation

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
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.ui.screens.AILiveScreen
import com.danshuang.AiLive.ui.screens.HomeScreen
import com.danshuang.AiLive.ui.screens.KnowledgeScreen
import com.danshuang.AiLive.ui.screens.LoginScreen
import com.danshuang.AiLive.ui.screens.ModelScreen
import com.danshuang.AiLive.ui.screens.ProfileScreen

sealed class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val route: String
) {
    data object Home : BottomNavItem("主页", Icons.Default.Home, "home")
    data object Knowledge : BottomNavItem("知识库", Icons.Default.Search, "knowledge")
    data object AILive : BottomNavItem("AI直播", Icons.Default.PlayArrow, "ai_live")
    data object Model : BottomNavItem("主播模型", Icons.Default.Person, "model")
    data object Profile : BottomNavItem("我的", Icons.Default.AccountCircle, "profile")
}

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    
    var isLoggedIn by remember { mutableStateOf(false) }
    
    // 检查是否已登录
    LaunchedEffect(Unit) {
        loginRepository.isLoggedInFlow().collect { loggedIn ->
            isLoggedIn = loggedIn
        }
    }
    
    if (!isLoggedIn) {
        LoginScreen(
            onLoginSuccess = { isLoggedIn = true }
        )
    } else {
        MainAppNavigation(
            onLogout = { isLoggedIn = false }
        )
    }
}

@Composable
fun MainAppNavigation(
    onLogout: () -> Unit = {}
) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Knowledge,
        BottomNavItem.AILive,
        BottomNavItem.Model,
        BottomNavItem.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.title) },
                        label = { Text(item.title) },
                        selected = selectedItem == index,
                        onClick = { selectedItem = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedItem) {
                0 -> HomeScreen()
                1 -> KnowledgeScreen()
                2 -> AILiveScreen()
                3 -> ModelScreen()
                4 -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}