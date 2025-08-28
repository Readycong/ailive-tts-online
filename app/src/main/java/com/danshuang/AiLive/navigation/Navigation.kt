package com.danshuang.AiLive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.danshuang.AiLive.ui.components.WithLoginProtection
import com.danshuang.AiLive.ui.screens.ContentEditScreen
import com.danshuang.AiLive.ui.screens.HomeScreen
import com.danshuang.AiLive.ui.screens.AILiveScreen
import com.danshuang.AiLive.ui.screens.GreetingSettingsScreen
import com.danshuang.AiLive.ui.screens.KnowledgeScreen
import com.danshuang.AiLive.ui.screens.LiveScriptScreen
import com.danshuang.AiLive.ui.screens.ModelScreen
import com.danshuang.AiLive.ui.screens.ProfileScreen
import com.danshuang.AiLive.ui.screens.ChangePasswordScreen
import com.danshuang.AiLive.ui.screens.HelpScreen
import com.danshuang.AiLive.ui.screens.CardActivationScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Model : Screen("model")
    data object AILive : Screen("ai_live")
    data object Knowledge : Screen("knowledge")
    data object Profile : Screen("profile")
    data object ChangePassword : Screen("change_password")
    data object Help : Screen("help")
    data object AccountBinding : Screen("account_binding") {
        fun createRoute() = "account_binding"
    }
    data object ContentEdit : Screen("content_edit/{contentKey}") {
        fun createRoute(contentKey: String) = "content_edit/$contentKey"
    }
    data object LiveScript : Screen("live_script/{scriptKey}") {
        fun createRoute(scriptKey: String) = "live_script/$scriptKey"
    }
    data object GreetingSettings : Screen("greeting_settings") {
        fun createRoute() = "greeting_settings"
    }
    data object NicknameRestrictions : Screen("nickname_restrictions") {
        fun createRoute() = "nickname_restrictions"
    }
    data object CommentRestrictions : Screen("comment_restrictions") {
        fun createRoute() = "comment_restrictions"
    }
    data object VoiceSettings : Screen("voice_settings") {
        fun createRoute() = "voice_settings"
    }
    data object VoiceCloning : Screen("voice_cloning") {
        fun createRoute() = "voice_cloning"
    }
    data object CardActivation : Screen("card_activation") {
        fun createRoute() = "card_activation"
    }
}

class NavigationState {
    var currentScreen by mutableStateOf<Screen>(Screen.Home)
    var contentEditKey by mutableStateOf<String?>(null)
    var liveScriptKey by mutableStateOf<String?>(null)
    
    fun navigateTo(screen: Screen) {
        currentScreen = screen
    }
    
    fun navigateToContentEdit(key: String) {
        contentEditKey = key
        currentScreen = Screen.ContentEdit
    }
    
    fun navigateToLiveScript(key: String) {
        liveScriptKey = key
        currentScreen = Screen.LiveScript
    }
    
    fun navigateToGreetingSettings() {
        currentScreen = Screen.GreetingSettings
    }
    
    fun navigateToNicknameRestrictions() {
        currentScreen = Screen.NicknameRestrictions
    }
    
    fun navigateToCommentRestrictions() {
        currentScreen = Screen.CommentRestrictions
    }

    fun navigateToVoiceSettings() {
        currentScreen = Screen.VoiceSettings
    }

    fun navigateToVoiceCloning() {
        currentScreen = Screen.VoiceCloning
    }

    fun navigateToChangePassword() {
        currentScreen = Screen.ChangePassword
    }

    fun navigateToHelp() {
        currentScreen = Screen.Help
    }

    fun navigateToAccountBinding() {
        currentScreen = Screen.AccountBinding
    }

    fun navigateToCardActivation() {
        currentScreen = Screen.CardActivation
    }

    fun navigateUp() {
        when (currentScreen) {
            is Screen.ContentEdit -> {
                contentEditKey = null
                currentScreen = Screen.Model
            }
            is Screen.LiveScript -> {
                liveScriptKey = null
                currentScreen = Screen.Model
            }
            is Screen.GreetingSettings -> {
                currentScreen = Screen.Model
            }
            is Screen.NicknameRestrictions -> {
                currentScreen = Screen.Model
            }
            is Screen.CommentRestrictions -> {
                currentScreen = Screen.Model
            }
            is Screen.VoiceSettings -> {
                currentScreen = Screen.Model
            }
            is Screen.VoiceCloning -> {
                currentScreen = Screen.Model
            }
            is Screen.ChangePassword -> {
                currentScreen = Screen.Profile
            }
            is Screen.Help -> {
                currentScreen = Screen.Profile
            }
            is Screen.AccountBinding -> {
                currentScreen = Screen.Home
            }
            is Screen.CardActivation -> {
                currentScreen = Screen.Profile
            }
            else -> {
                currentScreen = Screen.Home
            }
        }
    }
}

@Composable
fun rememberNavigationState(): NavigationState {
    return remember { NavigationState() }
}

@Composable
fun ScreenNavigation(
    navigationState: NavigationState,
    onLogout: () -> Unit = {}
) {
    when (navigationState.currentScreen) {
        is Screen.Home -> WithLoginProtection(
            screenName = "Home",
            onLogout = onLogout
        ) {
            HomeScreen(navController = navigationState)
        }
        is Screen.Model -> WithLoginProtection(
            screenName = "Model",
            onLogout = onLogout
        ) {
            ModelScreen(navigationState)
        }
        is Screen.AILive -> WithLoginProtection(
            screenName = "AILive",
            onLogout = onLogout
        ) {
            AILiveScreen(navController = navigationState)
        }
        is Screen.Knowledge -> WithLoginProtection(
            screenName = "Knowledge",
            onLogout = onLogout
        ) {
            KnowledgeScreen(navController = navigationState)
        }
        is Screen.Profile -> WithLoginProtection(
            screenName = "Profile",
            onLogout = onLogout
        ) {
            ProfileScreen(
                onLogout = onLogout,
                onNavigateToChangePassword = { navigationState.navigateToChangePassword() },
                onNavigateToHelp = { navigationState.navigateToHelp() },
                onNavigateToCardActivation = { navigationState.navigateToCardActivation() }
            )
        }
        is Screen.ChangePassword -> WithLoginProtection(
            screenName = "ChangePassword",
            onLogout = onLogout
        ) {
            ChangePasswordScreen(
                onBack = { navigationState.navigateUp() }
            )
        }
        is Screen.Help -> WithLoginProtection(
            screenName = "Help",
            onLogout = onLogout
        ) {
            HelpScreen(
                onBack = { navigationState.navigateUp() }
            )
        }
        is Screen.ContentEdit -> {
            navigationState.contentEditKey?.let { key ->
                WithLoginProtection(
                    screenName = "ContentEdit",
                    onLogout = onLogout
                ) {
                    ContentEditScreen(
                        navController = navigationState,
                        contentKey = key
                    )
                }
            }
        }
        is Screen.LiveScript -> {
            navigationState.liveScriptKey?.let { key ->
                WithLoginProtection(
                    screenName = "LiveScript",
                    onLogout = onLogout
                ) {
                    LiveScriptScreen(
                        navController = navigationState,
                        scriptKey = key
                    )
                }
            }
        }
        is Screen.GreetingSettings -> WithLoginProtection(
            screenName = "GreetingSettings",
            onLogout = onLogout
        ) {
            GreetingSettingsScreen(
                navController = navigationState
            )
        }
        is Screen.NicknameRestrictions -> WithLoginProtection(
            screenName = "NicknameRestrictions",
            onLogout = onLogout
        ) {
            com.danshuang.AiLive.ui.screens.RestrictionSettingsScreen(
                navController = navigationState,
                restrictionType = "nickname"
            )
        }
        is Screen.CommentRestrictions -> WithLoginProtection(
            screenName = "CommentRestrictions",
            onLogout = onLogout
        ) {
            com.danshuang.AiLive.ui.screens.RestrictionSettingsScreen(
                navController = navigationState,
                restrictionType = "comment"
            )
        }
        is Screen.VoiceSettings -> WithLoginProtection(
            screenName = "VoiceSettings",
            onLogout = onLogout
        ) {
            com.danshuang.AiLive.ui.screens.VoiceSettingsScreen(
                navController = navigationState
            )
        }
        is Screen.VoiceCloning -> WithLoginProtection(
            screenName = "VoiceCloning",
            onLogout = onLogout
        ) {
            com.danshuang.AiLive.ui.screens.VoiceCloningScreen(
                navController = navigationState
            )
        }
        is Screen.AccountBinding -> WithLoginProtection(
            screenName = "AccountBinding",
            onLogout = onLogout
        ) {
            com.danshuang.AiLive.ui.screens.AccountBindingScreen(
                navController = navigationState
            )
        }
        is Screen.CardActivation -> WithLoginProtection(
            screenName = "CardActivation",
            onLogout = onLogout
        ) {
            CardActivationScreen(
                onNavigateBack = { navigationState.navigateUp() }
            )
        }
    }
}