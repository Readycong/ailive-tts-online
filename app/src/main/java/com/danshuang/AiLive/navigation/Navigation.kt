package com.danshuang.AiLive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.danshuang.AiLive.ui.screens.ContentEditScreen
import com.danshuang.AiLive.ui.screens.HomeScreen
import com.danshuang.AiLive.ui.screens.AILiveScreen
import com.danshuang.AiLive.ui.screens.KnowledgeScreen
import com.danshuang.AiLive.ui.screens.ModelScreen
import com.danshuang.AiLive.ui.screens.ProfileScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Model : Screen("model")
    data object AILive : Screen("ai_live")
    data object Knowledge : Screen("knowledge")
    data object Profile : Screen("profile")
    data object ContentEdit : Screen("content_edit/{contentKey}") {
        fun createRoute(contentKey: String) = "content_edit/$contentKey"
    }
}

class NavigationState {
    var currentScreen by mutableStateOf<Screen>(Screen.Home)
    var contentEditKey by mutableStateOf<String?>(null)
    
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }
    
    fun navigateToContentEdit(key: String) {
        contentEditKey = key
        currentScreen = Screen.ContentEdit
    }
    
    fun navigateUp() {
        if (currentScreen == Screen.ContentEdit) {
            contentEditKey = null
            currentScreen = Screen.Model
        }
    }
}

@Composable
fun rememberNavigationState(): NavigationState {
    return remember { NavigationState() }
}

@Composable
fun AppNavigation(
    navigationState: NavigationState,
    onLogout: () -> Unit = {}
) {
    when (navigationState.currentScreen) {
        is Screen.Home -> HomeScreen()
        is Screen.Model -> ModelScreen(navigationState)
        is Screen.AILive -> AILiveScreen()
        is Screen.Knowledge -> KnowledgeScreen()
        is Screen.Profile -> ProfileScreen(onLogout = onLogout)
        is Screen.ContentEdit -> {
            navigationState.contentEditKey?.let { key ->
                ContentEditScreen(
                    navController = navigationState,
                    contentKey = key
                )
            }
        }
    }
}