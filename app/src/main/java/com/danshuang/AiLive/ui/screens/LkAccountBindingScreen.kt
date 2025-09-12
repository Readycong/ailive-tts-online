package com.danshuang.AiLive.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.danshuang.AiLive.R
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.config.AppConfig
import com.danshuang.AiLive.data.model.BoundAccountInfo
import com.danshuang.AiLive.data.model.PlatformType
import com.danshuang.AiLive.data.model.BindingStep
import com.danshuang.AiLive.data.model.ExtractionStatus
import com.danshuang.AiLive.data.model.BindingResult
import com.danshuang.AiLive.data.model.UserAccountInfo
import android.content.Context
import android.webkit.WebView
import android.webkit.WebViewClient
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

// DataStore实例
private val Context.dataStore by preferencesDataStore(name = "login_prefs")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LkAccountBindingScreen(
    navController: NavigationState
) {
    val context = LocalContext.current
    
    // 初始化依赖项
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val apiUtils = remember { ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository) }

    var accountUrl by remember { mutableStateOf(TextFieldValue("https://www.laik.live/user/MS4wLjABAAAAEpM-VSWnKAiy3wJvdZOXxor9Zo7Ts-buI0VW2jrtIxA?from_tab_name=live")) }
    var isLoading by remember { mutableStateOf(false) }
    var bindingResult by remember { mutableStateOf<com.danshuang.AiLive.data.model.BindingResult?>(null) }
    var bindingTrigger by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(com.danshuang.AiLive.data.model.BindingStep.URL_INPUT) }
    var accountInfo by remember { mutableStateOf<com.danshuang.AiLive.data.model.UserAccountInfo?>(null) }
    var showWebView by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var userHomeUrl by remember { mutableStateOf("") }
    var userRealHomeUrl by remember { mutableStateOf("") }
    var extractionStatus by remember { mutableStateOf(com.danshuang.AiLive.data.model.ExtractionStatus.IDLE) }
    var canBindAccount by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    
    // 提取URL的函数
    fun extractUrlFromText(text: String): String? {
        val urlPattern = Regex("https?://[^\\s]+")
        return urlPattern.find(text)?.value
    }
    
    // 保存账户信息到本地存储
    suspend fun saveAccountToLocal(accountInfo: com.danshuang.AiLive.data.model.UserAccountInfo, url: String) {
        try {
            // 使用LoginRepository中的公共方法来保存绑定账户信息
            val boundAccount = com.danshuang.AiLive.data.model.BoundAccountInfo(
                platform = com.danshuang.AiLive.data.model.PlatformType.LK,
                avatar = accountInfo.avatar,
                nickname = accountInfo.nickname,
                userId = accountInfo.userId,
                homeUrl = url,
                isLive = accountInfo.isLive,
                bindTime = System.currentTimeMillis()
            )
            loginRepository.saveLkAccount(boundAccount)
        } catch (e: Exception) {
            // 处理保存异常
        }
    }
    
    // 提交绑定信息到服务器
    suspend fun submitBindingToServer(accountInfo: com.danshuang.AiLive.data.model.UserAccountInfo, url: String): com.danshuang.AiLive.data.model.BindingResult {
        return withContext(Dispatchers.IO) {
            try {
                // 获取当前登录的token
                val token = tokenManager.currentToken.first()
                if (token.isNullOrBlank()) {
                    return@withContext com.danshuang.AiLive.data.model.BindingResult(false, "用户未登录，请先登录后再绑定账户")
                }
                
                // 从URL中提取房间ID（简单实现，实际可能需要更复杂的解析）
                // val roomId = accountInfo.userId.ifEmpty { "default_room_${System.currentTimeMillis()}" }
                
                // 调用DouyinApiService的bindDouyinLive方法（这里可能需要修改为来客的API）
                val serverResponse = apiUtils.bindDouyinLive(
                    token = token,
                    douyinId = accountInfo.userId,
                    nickname = accountInfo.nickname,
                    avatar = accountInfo.avatar,
                    roomId = "",
                    userHomeUrl = userRealHomeUrl
                )
                
                if (serverResponse.isSuccess) {
                    com.danshuang.AiLive.data.model.BindingResult(true, serverResponse.msg)
                } else {
                    com.danshuang.AiLive.data.model.BindingResult(false, serverResponse.msg)
                }
            } catch (e: Exception) {
                com.danshuang.AiLive.data.model.BindingResult(false, "绑定失败: ${e.message}")
            }
        }
    }

    LaunchedEffect(bindingTrigger) {
        if (bindingTrigger && accountInfo != null) {
            isLoading = true
            isCountingDown = true
            countdownSeconds = AppConfig.Api.getTimeout(context).toInt()
            
            // 开始倒计时
            while (countdownSeconds > 0 && isCountingDown) {
                kotlinx.coroutines.delay(1000)
                if (isCountingDown) {
                    countdownSeconds--
                }
            }
            
            try {
                // 提交到服务器
                val serverResult = submitBindingToServer(accountInfo!!, userHomeUrl)
                
                if (serverResult.success) {
                    // 服务器保存成功，同时保存到本地
                    saveAccountToLocal(accountInfo!!, userHomeUrl)
                    
                    // 保存绑定信息到用户登录信息中
                    val boundAccount = com.danshuang.AiLive.data.model.BoundAccountInfo(
                        platform = com.danshuang.AiLive.data.model.PlatformType.LK,
                        avatar = accountInfo!!.avatar,
                        nickname = accountInfo!!.nickname,
                        userId = accountInfo!!.userId,
                        homeUrl = userRealHomeUrl,
                        isLive = accountInfo!!.isLive,
                        bindTime = System.currentTimeMillis()
                    )
                    
                    bindingResult = serverResult
                    currentStep = com.danshuang.AiLive.data.model.BindingStep.ACCOUNT_PREVIEW
                } else {
                    bindingResult = serverResult
                }
            } catch (e: Exception) {
                bindingResult = com.danshuang.AiLive.data.model.BindingResult(false, "绑定过程中发生错误: ${e.message}")
            } finally {
                isLoading = false
                isCountingDown = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("来客账户绑定") },
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
                                    showWebView = true
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
                        text = "来客账户绑定",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = "请在来客APP中复制您的个人主页链接，粘贴到下方输入框中完成绑定。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = accountUrl,
                        onValueChange = { accountUrl = it },
                        label = { Text("来客个人主页链接") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        supportingText = {
                            Text("示例: https://www.laik.live/user/MS4wLjABAAAA...")
                        }
                    )
                }
                
                item {
                    Button(
                        onClick = {
                            // 提取URL
                            val extractedUrl = extractUrlFromText(accountUrl.text)
                            if (extractedUrl != null) {
                                userHomeUrl = extractedUrl
                                // 模拟账户信息提取
                                accountInfo = com.danshuang.AiLive.data.model.UserAccountInfo(
                                    avatar = "https://via.placeholder.com/80",
                                    nickname = "来客用户",
                                    isLive = false,
                                    userId = "lk_${System.currentTimeMillis()}"
                                )
                                bindingTrigger = true
                            } else {
                                bindingResult = com.danshuang.AiLive.data.model.BindingResult(false, "无法从输入内容中提取有效的URL，请检查输入")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading && accountUrl.text.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            text = if (isLoading) "绑定中..." else "绑定账户",
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
                
                // 显示绑定结果
                bindingResult?.let { result ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (result.success) {
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            } else {
                                CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            }
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                        contentDescription = null,
                                        tint = if (result.success) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                                    Text(
                                        text = result.message,
                                        modifier = Modifier.padding(start = 8.dp),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (result.success) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onErrorContainer
                                        }
                                    )
                                }
                                
                                if (result.success) {
                                    Button(
                                        onClick = { navController.navigateUp() },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 12.dp)
                                    ) {
                                        Text("返回个人中心")
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 账户预览
                if (currentStep == com.danshuang.AiLive.data.model.BindingStep.ACCOUNT_PREVIEW && accountInfo != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "账户信息预览",
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
                                        model = accountInfo?.avatar,
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
                                    text = accountInfo?.nickname ?: "未知用户",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                // 用户ID
                                Text(
                                    text = "用户ID: ${accountInfo?.userId}",
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
                                        imageVector = if (accountInfo?.isLive == true) Icons.Filled.LiveTv else Icons.Filled.TvOff,
                                        contentDescription = null,
                                        tint = if (accountInfo?.isLive == true) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (accountInfo?.isLive == true) "正在直播" else "未在直播",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (accountInfo?.isLive == true) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                                
                                // 绑定时间
                                Text(
                                    text = "绑定时间: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // 网页调试WebView
            if (showWebView) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { showWebView = false }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.8f)
                            .align(Alignment.BottomCenter)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "网页调试",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { showWebView = false }) {
                                    Icon(Icons.Filled.Close, contentDescription = "关闭")
                                }
                            }
                            
                            AndroidView(
                                factory = { ctx ->
                                    WebView(ctx).apply {
                                        webViewClient = WebViewClient()
                                        settings.javaScriptEnabled = true
                                        loadUrl("https://www.laik.live")
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LkAccountBindingScreenPreview() {
    LkAccountBindingScreen(navController = NavigationState())
}