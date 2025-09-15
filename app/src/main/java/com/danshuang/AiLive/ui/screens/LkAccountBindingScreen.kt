package com.danshuang.AiLive.ui.screens

import android.content.Context
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.datastore.preferences.preferencesDataStore
import coil.compose.AsyncImage
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.LoginState
import com.danshuang.AiLive.data.model.UserAccountInfo
import com.danshuang.AiLive.navigation.NavigationState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import com.danshuang.AiLive.util.Utils

// DataStore实例
private val Context.dataStore by preferencesDataStore(name = "login_prefs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LkAccountBindingScreen(
    navController: NavigationState
) {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    val scope = rememberCoroutineScope()
    
    // 状态管理
    var isUserLoggedIn by remember { mutableStateOf(false) }
    var showUserInfoCard by remember { mutableStateOf(false) }
    var showQRCode by remember { mutableStateOf(false) }
    var qrCodeData by remember { mutableStateOf("") }
    var loadingState by remember { mutableStateOf(false) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewLoaded by remember { mutableStateOf(false) }
    var userAccountInfo by remember { mutableStateOf<UserAccountInfo?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var loginState by remember { mutableStateOf(LoginState.NOT_LOGGED_IN) }
    var retryCount by remember { mutableStateOf(0) }
    var lastErrorTime by remember { mutableStateOf(0L) }
    var loginPageUrl by remember { mutableStateOf("https://eos.douyin.com/livesite/login") }
    
    // 初始化时检查本地存储的登录信息
    LaunchedEffect(Unit) {
        try {
            Log.d("LaikeAccountBinding", "Checking initial login status")
            val lkAccount: UserAccountInfo? = null
            if (lkAccount != null && lkAccount.userId.isNotEmpty()) {
                Log.d("LaikeAccountBinding", "User is logged in with account: ${lkAccount.userId}")
                isUserLoggedIn = true
                showUserInfoCard = true
                loginState = LoginState.LOGGED_IN
                userAccountInfo = UserAccountInfo(
                    avatar = lkAccount.avatar,
                    nickname = lkAccount.nickname,
                    userId = lkAccount.userId,
                    secUid = "", // secUid在本地存储中没有保存
                    homeUrl = lkAccount.homeUrl,
                    isLive = lkAccount.isLive
                )
            } else {
                Log.d("LaikeAccountBinding", "User is not logged in")
                loginPageUrl = "https://eos.douyin.com/livesite/login"
                isUserLoggedIn = false
                showQRCode = true
                loginState = LoginState.NOT_LOGGED_IN
            }
        } catch (e: Exception) {
            Log.e("LaikeAccountBinding", "Error checking login status", e)
            errorMessage = "检查登录状态时出错: ${e.message}"
            showQRCode = true
            loginState = LoginState.NOT_LOGGED_IN
        }
    }
    
    // 退出登录函数
    fun logout() {
        Log.d("LaikeAccountBinding", "Logout function called")
        scope.launch {
            try {
                Log.d("LaikeAccountBinding", "Starting logout process")
                // 清除直播专业版登录信息
                loginRepository.clearLkAccount()
                Log.d("LaikeAccountBinding", "Cleared laike account info")
                
                // 重置状态
                isUserLoggedIn = false
                showUserInfoCard = false
                showQRCode = true
                userAccountInfo = null
                qrCodeData = ""
                loginState = LoginState.NOT_LOGGED_IN
                errorMessage = null
                loginPageUrl = "https://eos.douyin.com/livesite/login"
                Log.d("LaikeAccountBinding", "Reset state variables")

                if(false) {
                    // 清除所有cookie
                    try {
                        android.webkit.CookieManager.getInstance().apply {
                            removeSessionCookies(null)
                        }
                    } catch (e: Exception) {
                        Log.e("LaikeAccountBinding", "Error clearing cookies: ${e.message}")
                    }
                    // 清除所有cookie
                    try {
                        android.webkit.CookieManager.getInstance().apply {
                            removeSessionCookies(null)
                            setCookie("https://eos.douyin.com", ""); // 清除指定域下的Cookie
                            flush()
                        }
                        Log.d("LaikeAccountBinding", "Cleared cookies")
                    } catch (e: Exception) {
                        Log.e("LaikeAccountBinding", "Error clearing cookies", e)
                    }

                    // 清除WebStorage数据（包括localStorage和sessionStorage）
                    try {
                        android.webkit.WebStorage.getInstance().deleteAllData()
                        Log.d("LaikeAccountBinding", "Cleared web storage")
                    } catch (e: Exception) {
                        Log.e("LaikeAccountBinding", "Error clearing web storage", e)
                    }

                    // 清除数据库
                    try {
                        android.webkit.WebViewDatabase.getInstance(context).clearUsernamePassword()
                        android.webkit.WebViewDatabase.getInstance(context)
                            .clearHttpAuthUsernamePassword()
                        Log.d("LaikeAccountBinding", "Cleared webview database")
                    } catch (e: Exception) {
                        Log.e("LaikeAccountBinding", "Error clearing webview database", e)
                    }
                }
                
                // 重新加载WebView以显示登录页面
                try {
                    webViewInstance?.loadUrl(loginPageUrl)
                    Log.d("LaikeAccountBinding", "Reloaded webview with login page")
                } catch (e: Exception) {
                    Log.e("LaikeAccountBinding", "Error reloading webview", e)
                }
                
                // 强制WebView重新创建以确保状态更新
                webViewLoaded = false
                Log.d("LaikeAccountBinding", "Logout process completed successfully")
            } catch (e: Exception) {
                Log.e("LaikeAccountBinding", "Error during logout", e)
                errorMessage = "退出登录时出错: ${e.message}"
            }
        }
    }
    
    // 重试加载函数
    fun retryLoad() {
        if (System.currentTimeMillis() - lastErrorTime < 3000) {
            // 防止频繁重试
            return
        }
        
        retryCount++
        lastErrorTime = System.currentTimeMillis()
        
        if (retryCount > 5) {
            errorMessage = "重试次数过多，请检查网络连接后稍后再试"
            return
        }
        
        try {
            qrCodeData = ""
            loadingState = true
            errorMessage = null
        } catch (e: Exception) {
            Log.e("LaikeAccountBinding", "Error retrying load", e)
            errorMessage = "重试加载时出错: ${e.message}"
        }
    }
    
    // WebView接口类，用于拦截XHR请求
    class WebAppInterface(
        private val scope: kotlinx.coroutines.CoroutineScope,
        private val webView: WebView,
        private val onLoginSuccess: (String, String, String, String) -> Unit,
        private val onError: (String) -> Unit
    ) {
        @JavascriptInterface
        fun onXhrResponse(method: String?, url: String?, status: Int, response: String?) {
            scope.launch {
                try {
                    Log.d("WebViewXHR", "XHR Request - Method: $method, URL: $url, Status: $status, Response: ${response?.take(500)}")

                    var isInLoginState = false
                    // 拦截登录验证请求
                    if (url?.contains("live/user/info/v1/") == true && status == 200) {
                        Log.d("WebViewXHR", "User info response received")
                        // 解析用户信息数据
                        response?.let { jsonResponse ->
                            try {
                                val jsonObject = JSONObject(jsonResponse)
                                if (jsonObject.has("sec_uid")) {
                                    if (jsonObject.optString("sec_uid", "").length > 1) {
                                        isInLoginState = true

                                        val secUid = jsonObject.optString("sec_uid", "")
                                        val nickname = jsonObject.optString("username", "")
                                        val avatar = jsonObject.optString("avatar_url", "")
                                        val userId = jsonObject.optString("douyin_unique_id", "")
                                        onLoginSuccess(secUid, nickname, avatar, userId)
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("WebViewXHR", "Error parsing user info data", e)
                                onError("解析用户信息数据时出错: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebViewXHR", "Error in onXhrResponse", e)
                    onError("处理网络响应时出错: ${e.message}")
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("直播专业版登录") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 主要内容
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 用户信息卡片（已登录状态）
                if (loginState == LoginState.LOGGED_IN && showUserInfoCard && userAccountInfo != null) {
                    Log.d("LaikeAccountBinding", "Showing user info card: loginState=$loginState, showUserInfoCard=$showUserInfoCard, userAccountInfo=${userAccountInfo != null}")
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "已绑定来客直播账户",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // 头像
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = userAccountInfo?.avatar,
                                        contentDescription = "用户头像",
                                        modifier = Modifier
                                            .size(80.dp)
                                            .clip(CircleShape),
                                        fallback = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                                        error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // 昵称
                                Text(
                                    text = userAccountInfo?.nickname ?: "未知用户",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // 退出登录按钮
                                Button(
                                    onClick = {
                                        Log.d("Test", "Logout button clicked") // 检查是否打印
                                        logout()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    enabled = true // 确保可用
                                ) {
                                    Text("退出登录")
                                }
                            }
                        }
                    }
                }
//                else {
//                    Log.d("LaikeAccountBinding", "Not showing user info card: loginState=$loginState, showUserInfoCard=$showUserInfoCard, userAccountInfo=${userAccountInfo != null}")
                    item {
                        Box(
                            modifier = Modifier.size(1920.dp, 720.dp) // 根据用户偏好设置WebView尺寸
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    Log.d("LaikeAccountBinding", "Creating WebView with loginState: $loginState")
                                    WebView(ctx).apply {
                                        webViewInstance = this
                                        WebView.setWebContentsDebuggingEnabled(true)
                                        settings.apply {
                                            javaScriptEnabled = true
                                            displayZoomControls = true
                                            setSupportZoom(true)
                                            allowContentAccess = true
                                            javaScriptCanOpenWindowsAutomatically = true
                                            domStorageEnabled = true
                                            databaseEnabled = true
                                            useWideViewPort = true
                                            loadWithOverviewMode = true
                                            setDatabaseEnabled(true)
                                            setGeolocationEnabled(true)
                                            // 设置为PC浏览器模式
                                            userAgentString = Utils.userAgent
                                        }
                                        
                                        // 根据登录状态决定加载的页面
                                        if (loginState != LoginState.LOGGED_IN) {
                                            Log.d("LaikeAccountBinding", "WebView loading login page")
                                            // 未登录时加载登录页面
                                            // 创建WebAppInterface实例
                                            val webAppInterface = WebAppInterface(
                                                scope, this,
                                                { secUid, nickname, avatar, userId ->
                                                    // 登录成功处理
                                                    val userAccount = UserAccountInfo(
                                                        avatar = avatar,
                                                        nickname = nickname,
                                                        userId = userId,
                                                        secUid = secUid,
                                                        homeUrl = "https://www.douyin.com/user/$secUid",
                                                        isLive = false // 默认为未直播
                                                    )
                                                    
                                                    userAccountInfo = userAccount
                                                    isUserLoggedIn = true
                                                    showUserInfoCard = true
                                                    showQRCode = false
                                                    loadingState = false
                                                    loginState = LoginState.LOGGED_IN
                                                    retryCount = 0 // 重置重试计数
                                                    errorMessage = null

                                                    // 保存用户信息到本地存储
                                                    val boundAccount = com.danshuang.AiLive.data.model.BoundAccountInfo(
                                                        platform = com.danshuang.AiLive.data.model.PlatformType.LK,
                                                        avatar = avatar,
                                                        nickname = nickname,
                                                        userId = userId,
                                                        homeUrl = "https://www.douyin.com/user/$secUid",
                                                        isLive = false,
                                                        bindTime = System.currentTimeMillis()
                                                    )
                                                    scope.launch {
                                                        loginRepository.saveLkAccount(boundAccount)
                                                    }
                                                    Log.d("WebViewXHR", "User info saved successfully")
                                                    loginPageUrl = "about:blank"
                                                    // 加载空白页以停止WebView活动
                                                    this.loadUrl("about:blank")
                                                },
                                                { error ->
                                                    errorMessage = error
                                                }
                                            )
                                            
                                            // 添加JavaScript接口以拦截XHR请求
                                            addJavascriptInterface(webAppInterface, "Android")
                                            
                                            // 设置WebViewClient以处理页面加载
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    super.onPageFinished(view, url)
                                                    Log.d("WebView", "Page finished loading: $url")
                                                    
                                                    if (view == null) return
                                                    
                                                    // 注入JavaScript代码以拦截XMLHttpRequest
                                                    val jsCode = """
                                            (function() {
                                                var originalOpen = XMLHttpRequest.prototype.open;
                                                var originalSend = XMLHttpRequest.prototype.send;
                                                
                                                XMLHttpRequest.prototype.open = function(method, url) {
                                                    this._url = url;
                                                    this._method = method;
                                                    return originalOpen.apply(this, arguments);
                                                };
                                                
                                                XMLHttpRequest.prototype.send = function(data) {
                                                    var self = this;
                                                    this.addEventListener('load', function() {
                                                        var response = self.response;
                                                        if (typeof response === 'object') {
                                                            response = JSON.stringify(response);
                                                        }
                                                        Android.onXhrResponse(self._method, self._url, self.status, response);
                                                    });
                                                    return originalSend.apply(this, arguments);
                                                };
                                            })();
                                        """.trimIndent()
                                                    
                                                    view.evaluateJavascript(jsCode, null)
                                                    webViewLoaded = true
                                                    
                                                    // 如果是初始加载，显示加载状态
                                                    if (!isUserLoggedIn && qrCodeData.isEmpty()) {
                                                        loadingState = true
                                                    }
                                                }
                                                
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                                    // 允许所有URL加载
                                                    return false
                                                }
                                            }
                                            
                                            // 加载抖音用户主页
                                            loadUrl(loginPageUrl)
                                        } else {
                                            Log.d("LaikeAccountBinding", "WebView loading blank page as user is logged in")
                                            // 已登录时加载空白页面
                                            loadUrl("about:blank")
                                        }
                                    }
                                },
                                update = { webView ->
                                    Log.d("LaikeAccountBinding", "Updating WebView with loginPageUrl: $loginPageUrl, loginState: $loginState")
                                    // 根据登录状态决定加载的页面
                                    if (loginState != LoginState.LOGGED_IN) {
                                        // 未登录时加载登录页面
                                        if (webView.url != loginPageUrl) {
                                            Log.d("LaikeAccountBinding", "Loading new loginPageUrl: $loginPageUrl")
                                            webView.loadUrl(loginPageUrl)
                                        }
                                    } else {
                                        // 已登录时加载空白页面
                                        if (webView.url != "about:blank") {
                                            Log.d("LaikeAccountBinding", "Loading blank page as user is logged in")
                                            webView.loadUrl("about:blank")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(1920.dp, 720.dp)
                            )
                        }
                    }

            }
        }
    }
}