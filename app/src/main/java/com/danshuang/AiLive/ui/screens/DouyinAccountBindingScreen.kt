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
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.model.LoginState
import com.danshuang.AiLive.data.model.UserAccountInfo
import com.danshuang.AiLive.navigation.NavigationState
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
    
    // 初始化时检查本地存储的登录信息
    LaunchedEffect(Unit) {
        try {
            val douyinAccount = loginRepository.getDouyinAccount()
            if (douyinAccount != null && douyinAccount.userId.isNotEmpty()) {
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
        scope.launch {
            try {
                loginRepository.clearDouyinAccount()
                isUserLoggedIn = false
                showUserInfoCard = false
                showQRCode = true
                userAccountInfo = null
                qrCodeData = ""
                loginState = LoginState.NOT_LOGGED_IN
                errorMessage = null
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
        private val onQrCodeReceived: (String) -> Unit,
        private val onLoginSuccess: (String, String, String, String) -> Unit,
        private val onError: (String) -> Unit
    ) {
        @JavascriptInterface
        fun onXhrResponse(method: String?, url: String?, status: Int, response: String?) {
            scope.launch {
                try {
                    Log.d("WebViewXHR", "XHR Request - Method: $method, URL: $url, Status: $status")
                    
                    // 拦截二维码请求
                    if (url?.contains("https://login.douyin.com/passport/web/get_qrcode/") == true && status == 200) {
                        Log.d("WebViewXHR", "QR Code response received")
                        // 解析二维码数据
                        response?.let { jsonResponse ->
                            try {
                                val jsonObject = JSONObject(jsonResponse)
                                if (jsonObject.has("data")) {
                                    val dataObject = jsonObject.getJSONObject("data")
                                    if (dataObject.has("qrcode")) {
                                        val base64Data = dataObject.getString("qrcode")
                                        onQrCodeReceived(base64Data)
                                        retryCount = 0 // 重置重试计数
                                        Log.d("WebViewXHR", "QR Code extracted successfully")
                                    } else {
                                        Log.w("WebViewXHR", "QR code data not found in response")
                                        // 如果没有二维码数据，可能是请求频率过高
                                        if (retryCount < 3) {
                                            kotlinx.coroutines.delay(2000)
                                            retryLoad()
                                        }
                                    }
                                } else {
                                    Log.w("WebViewXHR", "Data not found in QR code response")
                                }
                            } catch (e: Exception) {
                                Log.e("WebViewXHR", "Error parsing QR code data", e)
                                onError("解析二维码数据时出错: ${e.message}")
                            }
                        }
                    }
                    
                    // 拦截登录验证请求
                    if (url?.contains("https://www.douyin.com/aweme/v1/aweme/favorite/") == true && status == 200) {
                        Log.d("WebViewXHR", "Login validation response received")
                        // 解析登录验证数据
                        response?.let { jsonResponse ->
                            try {
                                val jsonObject = JSONObject(jsonResponse)
                                // 检查是否有sec_uid
                                if (jsonObject.has("data")) {
                                    val dataObject = jsonObject.getJSONObject("data")
                                    if (dataObject.has("sec_uid")) {
                                        val secUid = dataObject.getString("sec_uid")
                                        if (secUid.isNotEmpty()) {
                                            Log.d("WebViewXHR", "Login successful, sec_uid: $secUid")
                                            // 登录成功，获取用户信息
                                            // 这里应该通过其他方式获取用户信息，因为WebView中的JavaScript执行可能有限制
                                            onLoginSuccess(secUid, "", "", "")
                                        } else {
                                            Log.w("WebViewXHR", "SecUid is empty")
                                        }
                                    } else {
                                        Log.d("WebViewXHR", "SecUid not found in response")
                                    }
                                } else {
                                    Log.w("WebViewXHR", "Data not found in login validation response")
                                }
                            } catch (e: Exception) {
                                Log.e("WebViewXHR", "Error parsing login validation data", e)
                                onError("解析登录验证数据时出错: ${e.message}")
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
                title = { Text("抖音账户绑定") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("网页调试") },
                                onClick = { 
                                    // 调试功能可以在这里实现
                                    showDropdownMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.BugReport, contentDescription = "调试")
                                }
                            )
                        }
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
                item {
                    Text(
                        text = "抖音账户绑定",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "通过扫码登录绑定您的抖音账户，以便进行直播互动。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                // 显示错误信息
                errorMessage?.let { message ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                if (retryCount < 5) {
                                    Button(
                                        onClick = { retryLoad() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.onError,
                                            contentColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text("重试")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 加载状态
                if (loginState == LoginState.LOGGING_IN || loadingState) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "正在处理登录请求...",
                                    modifier = Modifier.padding(top = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                // 用户信息卡片（已登录状态）
                if (loginState == LoginState.LOGGED_IN && showUserInfoCard && userAccountInfo != null) {
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
                                
                                // 用户ID
                                Text(
                                    text = "用户ID: ${userAccountInfo?.userId}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                
                                // 直播状态
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (userAccountInfo?.isLive == true) Icons.Filled.LiveTv else Icons.Filled.TvOff,
                                        contentDescription = null,
                                        tint = if (userAccountInfo?.isLive == true) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (userAccountInfo?.isLive == true) "正在直播" else "未在直播",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (userAccountInfo?.isLive == true) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                
                                // 绑定时间
                                Text(
                                    text = "绑定时间: ${android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", java.util.Date())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                
                                // 退出登录按钮
                                Button(
                                    onClick = { logout() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    )
                                ) {
                                    Text("退出登录")
                                }
                            }
                        }
                    }
                }
                
                // 二维码显示区域（未登录状态）
                if (loginState == LoginState.NOT_LOGGED_IN && showQRCode) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "请使用抖音APP扫码登录",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // 二维码显示
                                if (qrCodeData.isNotEmpty()) {
                                    // 显示二维码图片
                                    AsyncImage(
                                        model = "data:image/png;base64,$qrCodeData",
                                        contentDescription = "登录二维码",
                                        modifier = Modifier
                                            .size(200.dp)
                                            .padding(8.dp)
                                    )
                                    Text(
                                        text = "使用抖音APP扫描上方二维码完成登录绑定",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 16.dp)
                                    )
                                } else {
                                    // 显示加载状态
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(32.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text(
                                            text = "正在获取二维码...",
                                            modifier = Modifier.padding(top = 16.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "请稍候...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(top = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 重新加载按钮（用于故障恢复）
                if (loginState == LoginState.NOT_LOGGED_IN) {
                    item {
                        OutlinedButton(
                            onClick = { retryLoad() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = "刷新")
                            Text("重新获取二维码", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
            
            // 后台WebView（不可见）
            Box(
                modifier = Modifier
                    .size(1920.dp, 720.dp) // 根据用户偏好设置WebView尺寸
                    .alpha(0f) // 完全透明，后台运行
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewInstance = this
                            
                            // 配置WebView设置
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.builtInZoomControls = false
                            settings.displayZoomControls = false
                            settings.allowContentAccess = true
                            settings.allowFileAccess = true
                            
                            // 设置PC模式User-Agent
                            val pcUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
                            settings.userAgentString = pcUserAgent
                            
                            // 创建WebAppInterface实例
                            val webAppInterface = WebAppInterface(
                                scope,
                                { qrCode ->
                                    qrCodeData = qrCode
                                    showQRCode = true
                                    loadingState = false
                                },
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
                                        platform = com.danshuang.AiLive.data.model.PlatformType.DOUYIN,
                                        avatar = avatar,
                                        nickname = nickname,
                                        userId = userId,
                                        homeUrl = "https://www.douyin.com/user/$secUid",
                                        isLive = false,
                                        bindTime = System.currentTimeMillis()
                                    )
                                    scope.launch {
                                        loginRepository.saveDouyinAccount(boundAccount)
                                    }
                                    
                                    Log.d("WebViewXHR", "User info saved successfully")
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
                                
                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
                                    super.onReceivedError(view, request, error)
                                    Log.e("WebView", "WebView error: ${error?.description}")
                                    
                                    // 检查是否为网络错误
                                    when {
                                        error?.errorCode == android.webkit.WebViewClient.ERROR_HOST_LOOKUP -> {
                                            errorMessage = "网络连接错误，请检查网络设置"
                                        }
                                        error?.errorCode == android.webkit.WebViewClient.ERROR_TIMEOUT -> {
                                            errorMessage = "请求超时，请稍后重试"
                                        }
                                        error?.errorCode == android.webkit.WebViewClient.ERROR_CONNECT -> {
                                            errorMessage = "连接失败，请检查网络连接"
                                        }
                                        else -> {
                                            errorMessage = "加载页面时出错: ${error?.description}"
                                        }
                                    }
                                    
                                    loadingState = false
                                    showQRCode = true
                                }
                                
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    // 允许所有URL加载
                                    return false
                                }
                            }
                            
                            // 加载抖音用户主页
                            loadUrl("https://www.douyin.com/user/self?from_tab_name=main")
                        }
                    },
                    modifier = Modifier
                        .size(1920.dp, 720.dp)
                        .alpha(0f) // 完全透明，后台运行
                )
            }
        }
    }
}