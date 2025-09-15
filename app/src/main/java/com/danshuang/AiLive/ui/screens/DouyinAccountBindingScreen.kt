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

// DataStore实例
private val Context.dataStore by preferencesDataStore(name = "login_prefs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DouyinAccountBindingScreen(
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
    var loginPageUrl by remember { mutableStateOf("about:blank") }
    
    // 初始化时检查本地存储的登录信息
    LaunchedEffect(Unit) {
        try {
            Log.d("DouyinAccountBinding", "Checking initial login status")
            val douyinAccount = loginRepository.getDouyinAccount()
            if (douyinAccount != null && douyinAccount.userId.isNotEmpty()) {
                Log.d("DouyinAccountBinding", "User is logged in with account: ${douyinAccount.userId}")
                isUserLoggedIn = true
                showUserInfoCard = true
                loginState = LoginState.LOGGED_IN
                userAccountInfo = UserAccountInfo(
                    avatar = douyinAccount.avatar,
                    nickname = douyinAccount.nickname,
                    userId = douyinAccount.userId,
                    secUid = "", // secUid在本地存储中没有保存
                    homeUrl = douyinAccount.homeUrl,
                    isLive = douyinAccount.isLive
                )
            } else {
                Log.d("DouyinAccountBinding", "User is not logged in")
                loginPageUrl = "https://www.douyin.com/help"
                isUserLoggedIn = false
                showQRCode = true
                loginState = LoginState.NOT_LOGGED_IN
            }
        } catch (e: Exception) {
            Log.e("DouyinAccountBinding", "Error checking login status", e)
            errorMessage = "检查登录状态时出错: ${e.message}"
            showQRCode = true
            loginState = LoginState.NOT_LOGGED_IN
        }
    }
    
    // 退出登录函数
    fun logout() {
        Log.d("DouyinAccountBinding", "Logout function called")
        scope.launch {
            try {
                Log.d("DouyinAccountBinding", "Starting logout process")
                // 清除抖音账户绑定信息
                loginRepository.clearDouyinAccount()
                Log.d("DouyinAccountBinding", "Cleared douyin account info")
                
                // 重置状态
                isUserLoggedIn = false
                showUserInfoCard = false
                showQRCode = true
                userAccountInfo = null
                qrCodeData = ""
                loginState = LoginState.NOT_LOGGED_IN
                errorMessage = null
                loginPageUrl = "https://www.douyin.com/help"
                Log.d("DouyinAccountBinding", "Reset state variables")

                // 清除所有cookie
                try {
                    android.webkit.CookieManager.getInstance().apply {
                        removeAllCookies(null)
                        removeSessionCookies(null)
                        flush()
                    }
                    Log.d("DouyinAccountBinding", "Cleared cookies")
                } catch (e: Exception) {
                    Log.e("DouyinAccountBinding", "Error clearing cookies", e)
                }

                // 清除WebStorage数据（包括localStorage和sessionStorage）
                try {
                    android.webkit.WebStorage.getInstance().deleteAllData()
                    Log.d("DouyinAccountBinding", "Cleared web storage")
                } catch (e: Exception) {
                    Log.e("DouyinAccountBinding", "Error clearing web storage", e)
                }

                // 清除数据库
                try {
                    android.webkit.WebViewDatabase.getInstance(context).clearUsernamePassword()
                    android.webkit.WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
                    Log.d("DouyinAccountBinding", "Cleared webview database")
                } catch (e: Exception) {
                    Log.e("DouyinAccountBinding", "Error clearing webview database", e)
                }
                
                // 重新加载WebView以显示登录页面
                try {
                    webViewInstance?.loadUrl(loginPageUrl)
                    Log.d("DouyinAccountBinding", "Reloaded webview with login page")
                } catch (e: Exception) {
                    Log.e("DouyinAccountBinding", "Error reloading webview", e)
                }
                
                // 强制WebView重新创建以确保状态更新
                webViewLoaded = false
                Log.d("DouyinAccountBinding", "Logout process completed successfully")
            } catch (e: Exception) {
                Log.e("DouyinAccountBinding", "Error during logout", e)
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
            Log.e("DouyinAccountBinding", "Error retrying load", e)
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
                    if (url?.contains("/web/query/user") == true && status == 200) {
                        Log.d("WebViewXHR", "User info response received")
                        // 解析用户信息数据
                        response?.let { jsonResponse ->
                            try {
                                val jsonObject = JSONObject(jsonResponse)
                                if (jsonObject.has("user_uid")) {
                                    if (jsonObject.optString("user_uid", "").length > 1) {
                                        isInLoginState = true
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("WebViewXHR", "Error parsing user info data", e)
                                onError("解析用户信息数据时出错: ${e.message}")
                            }
                        }
                    }
                    if (url?.contains("/aweme/favorite/") == true && status == 200) {
                        Log.d("WebViewXHR", "Login validation response received")
                        // 解析登录验证数据
                        response?.let { jsonResponse ->
                            try {
                                val dataObject = JSONObject(jsonResponse)
                                // 检查是否有sec_uid
                                if (dataObject.has("sec_uid")) {
                                    val secUid = dataObject.getString("sec_uid")
                                    if (secUid.isNotEmpty()) {
                                        isInLoginState = true
                                        Log.d("WebViewXHR", "Login successful, sec_uid: $secUid")
                                    } else {
                                        Log.w("WebViewXHR", "SecUid is empty")
                                    }
                                } else {
                                    Log.d("WebViewXHR", "SecUid not found in response")
                                }
                            } catch (e: Exception) {
                                Log.e("WebViewXHR", "Error parsing login validation data", e)
                                onError("解析登录验证数据时出错: ${e.message}")
                            }
                        }
                    }

                    if (isInLoginState) {
                        // 登录成功，获取用户信息
                        // 这里应该通过其他方式获取用户信息，因为WebView中的JavaScript执行可能有限制
                        webView.evaluateJavascript("(function(){var html=document.body.outerHTML; return html.substr(html.indexOf('\"info'),10240)})()"){ html ->
                            var info = html.replace("\\u002F", "&").replace("\\", "").replace("\"", "").substring(6).replace("u0026", "&")
                            Log.d("WebView", "execute get user info: $info")
                            val regex = Regex("(\\w+):([^,]+)")
                            val matches = regex.findAll(info)
                            val userInfoMap = mutableMapOf<String, String>()
                            
                            for (match in matches) {
                                val (key, value) = match.destructured
                                userInfoMap[key] = value
                                Log.d("WebView", "Parsed info - Key: $key, Value: $value")
                            }
                            
                            val secUid = userInfoMap["secUid"] ?: ""
                            val nickname = userInfoMap["nickname"] ?: ""
                            val avatar = userInfoMap["avatar300Url"] ?: ""
                            val userId = userInfoMap["uniqueId"] ?: ""
                            onLoginSuccess(secUid, nickname, avatar, userId)
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
                title = { Text("抖音账户绑定") },
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
                    Log.d("DouyinAccountBinding", "Showing user info card: loginState=$loginState, showUserInfoCard=$showUserInfoCard, userAccountInfo=${userAccountInfo != null}")
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "已绑定抖音账户",
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
                } else {
                    Log.d("DouyinAccountBinding", "Not showing user info card: loginState=$loginState, showUserInfoCard=$showUserInfoCard, userAccountInfo=${userAccountInfo != null}")
                    item {
                        Box(
                            modifier = Modifier.size(1920.dp, 720.dp) // 根据用户偏好设置WebView尺寸
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    Log.d(
                                        "DouyinAccountBinding",
                                        "Creating WebView with loginState: $loginState"
                                    )
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
                                            userAgentString =
                                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                        }

                                        // 根据登录状态决定加载的页面
                                        if (loginState != LoginState.LOGGED_IN) {
                                            Log.d(
                                                "DouyinAccountBinding",
                                                "WebView loading login page"
                                            )
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
                                                    val boundAccount =
                                                        com.danshuang.AiLive.data.model.BoundAccountInfo(
                                                            platform = com.danshuang.AiLive.data.model.PlatformType.DOUYIN,
                                                            avatar = avatar,
                                                            nickname = nickname,
                                                            userId = userId,
                                                            homeUrl = "https://www.douyin.com/user/$secUid",
                                                            isLive = false,
                                                            bindTime = System.currentTimeMillis()
                                                        )
                                                    scope.launch {
                                                        loginRepository.saveDouyinAccount(
                                                            boundAccount
                                                        )
                                                    }
                                                    // 调用ServerApiUtilsV2.bindDouyinLive保存绑定
                                                    scope.launch {
                                                        try {
                                                            // 首先获取token
                                                            val token =
                                                                loginRepository.authToken.first()
                                                                    ?: ""
                                                            if (token.isEmpty()) {
                                                                Log.e("ServerApi", "获取token失败")
                                                                errorMessage = "获取认证信息失败"
                                                                return@launch
                                                            }

                                                            // 创建ServerApiUtilsV2实例
                                                            val apiUtils =
                                                                ServerApiUtilsV2.createFromContext(
                                                                    context,
                                                                    TokenManager(loginRepository),
                                                                    loginRepository
                                                                )

                                                            val response = apiUtils.bindDouyinLive(
                                                                token = token,
                                                                douyinId = secUid,
                                                                nickname = nickname,
                                                                avatar = avatar,
                                                                roomId = "", // 暂时使用空字符串，可以根据需要设置
                                                                userHomeUrl = "https://www.douyin.com/user/$secUid"
                                                            )
                                                            if (response.isSuccess) {
                                                                Log.d(
                                                                    "ServerApi",
                                                                    "抖音账户绑定成功"
                                                                )
                                                            } else {
                                                                Log.e(
                                                                    "ServerApi",
                                                                    "抖音账户绑定失败: ${response.msg}"
                                                                )
                                                                errorMessage =
                                                                    "绑定失败: ${response.msg}"
                                                            }
                                                        } catch (e: UnknownHostException) {
                                                            Log.e("ServerApi", "网络连接错误", e)
                                                            errorMessage =
                                                                "网络连接错误，请检查网络设置"
                                                        } catch (e: SocketTimeoutException) {
                                                            Log.e("ServerApi", "请求超时", e)
                                                            errorMessage = "请求超时，请稍后重试"
                                                        } catch (e: Exception) {
                                                            Log.e(
                                                                "ServerApi",
                                                                "绑定抖音账户时出错",
                                                                e
                                                            )
                                                            errorMessage = "绑定失败: ${e.message}"
                                                        }
                                                    }

                                                    Log.d(
                                                        "WebViewXHR",
                                                        "User info saved successfully"
                                                    )
                                                },
                                                { error ->
                                                    errorMessage = error
                                                }
                                            )

                                            // 添加JavaScript接口以拦截XHR请求
                                            addJavascriptInterface(webAppInterface, "Android")

                                            // 设置WebViewClient以处理页面加载
                                            webViewClient = object : WebViewClient() {
                                                override fun onPageFinished(
                                                    view: WebView?,
                                                    url: String?
                                                ) {
                                                    super.onPageFinished(view, url)
                                                    Log.d("WebView", "Page finished loading: $url")

                                                    if (view == null) return

                                                    if (url != null) {
                                                        if (url.indexOf("https://www.douyin.com/help") >= 0) {
                                                            view.evaluateJavascript("document.querySelectorAll('button').forEach(btn => { if (btn.innerText.includes('登录')) btn.click(); });document.querySelector('#b4kMZDrJ > div').click()") { html ->
                                                                Log.d(
                                                                    "WebView",
                                                                    "execute show login layer: $html"
                                                                )
                                                            }
                                                        }
                                                    }

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

                                                override fun shouldOverrideUrlLoading(
                                                    view: WebView?,
                                                    request: WebResourceRequest?
                                                ): Boolean {
                                                    // 允许所有URL加载
                                                    return false
                                                }
                                            }

                                            // 加载抖音用户主页
                                            loadUrl(loginPageUrl)
                                        } else {
                                            Log.d(
                                                "DouyinAccountBinding",
                                                "WebView loading blank page as user is logged in"
                                            )
                                            // 已登录时加载空白页面
                                            loadUrl("about:blank")
                                        }
                                    }
                                },
                                modifier = Modifier.size(1920.dp, 720.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}