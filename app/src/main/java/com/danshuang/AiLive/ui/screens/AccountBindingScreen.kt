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
import com.danshuang.AiLive.util.Utils
import com.danshuang.AiLive.data.model.UserAccountInfo

// DataStore实例
private val Context.dataStore by preferencesDataStore(name = "login_prefs")

// 绑定步骤枚举
enum class BindingStep {
    URL_INPUT,      // 步骤一：URL输入
    ACCOUNT_PREVIEW // 步骤二：账户预览
}

// 信息提取状态枚举
enum class ExtractionStatus {
    IDLE,           // 空闲状态
    LOADING,        // 加载中
    SUCCESS,        // 提取成功
    ERROR           // 提取失败
}

// 绑定结果数据类
data class BindingResult(
    val success: Boolean,
    val message: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBindingScreen(
    navController: NavigationState
) {
    val context = LocalContext.current
    
    // 初始化依赖项
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val apiUtils = remember { ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository) }

//    var accountUrl by remember { mutableStateOf(TextFieldValue("7- 长按复制此条消息，打开抖音搜索，查看TA的更多作品。 https://v.douyin.com/eGtOuTv39gM/ 0@2.com :0pm")) }
    var accountUrl by remember { mutableStateOf(TextFieldValue("https://www.douyin.com/user/MS4wLjABAAAAEpM-VSWnKAiy3wJvdZOXxor9Zo7Ts-buI0VW2jrtIxA?from_tab_name=live")) }
    var isLoading by remember { mutableStateOf(false) }
    var bindingResult by remember { mutableStateOf<BindingResult?>(null) }
    var bindingTrigger by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(BindingStep.URL_INPUT) }
    var accountInfo by remember { mutableStateOf<com.danshuang.AiLive.data.model.UserAccountInfo?>(null) }
    var showWebView by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }
    var userHomeUrl by remember { mutableStateOf("") }
    var userRealHomeUrl by remember { mutableStateOf("") }
    var extractionStatus by remember { mutableStateOf(ExtractionStatus.IDLE) }
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
            val boundAccount = BoundAccountInfo(
                platform = "douyin",
                avatar = accountInfo.avatar,
                nickname = accountInfo.nickname,
                userId = accountInfo.userId,
                homeUrl = url,
                isLive = accountInfo.isLive,
                bindTime = System.currentTimeMillis()
            )
            loginRepository.saveBoundAccountInfo(boundAccount)
        } catch (e: Exception) {
            // 处理保存异常
        }
    }
    
    // 提交绑定信息到服务器
    suspend fun submitBindingToServer(accountInfo: com.danshuang.AiLive.data.model.UserAccountInfo, url: String): BindingResult {
        return withContext(Dispatchers.IO) {
            try {
                // 获取当前登录的token
                val token = tokenManager.currentToken.first()
                if (token.isNullOrBlank()) {
                    return@withContext BindingResult(false, "用户未登录，请先登录后再绑定账户")
                }
                
                // 从URL中提取房间ID（简单实现，实际可能需要更复杂的解析）
                // val roomId = accountInfo.userId.ifEmpty { "default_room_${System.currentTimeMillis()}" }
                
                // 调用DouyinApiService的bindDouyinLive方法
                val serverResponse = apiUtils.bindDouyinLive(
                    token = token,
                    douyinId = accountInfo.userId,
                    nickname = accountInfo.nickname,
                    avatar = accountInfo.avatar,
                    roomId = "",
                    userHomeUrl = userRealHomeUrl
                )
                
                if (serverResponse.isSuccess) {
                    BindingResult(true, serverResponse.msg)
                } else {
                    BindingResult(false, serverResponse.msg)
                }
            } catch (e: Exception) {
                BindingResult(false, "绑定失败: ${e.message}")
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
                    val boundAccount = BoundAccountInfo(
                        platform = "douyin",
                        avatar = accountInfo!!.avatar,
                        nickname = accountInfo!!.nickname,
                        userId = accountInfo!!.userId,
                        homeUrl = userRealHomeUrl,
                        isLive = accountInfo!!.isLive,
                        bindTime = System.currentTimeMillis()
                    )
                    loginRepository.saveBoundAccountInfo(boundAccount)
                    
                    bindingResult = BindingResult(true, "绑定成功")
                } else {
                    // 服务器保存失败
                    bindingResult = BindingResult(false, serverResult.message)
                }
            } catch (e: Exception) {
                bindingResult = BindingResult(false, "绑定过程中发生错误: ${e.message}")
            } finally {
                isLoading = false
                isCountingDown = false
                countdownSeconds = 0
                bindingTrigger = false
            }
        }
    }
    
    // 监听绑定成功状态，自动返回上一页
    LaunchedEffect(bindingResult) {
        bindingResult?.let { result ->
            if (result.success) {
                // 绑定成功后延迟1.5秒再返回上一页，让用户看到成功提示
                kotlinx.coroutines.delay(1500)
                navController.navigateUp()
            }
        }
    }
    
    // 处理账户信息提取状态
    LaunchedEffect(currentStep) {
        if (currentStep == BindingStep.ACCOUNT_PREVIEW && extractionStatus == ExtractionStatus.IDLE) {
            extractionStatus = ExtractionStatus.LOADING
            isLoading = true
            canBindAccount = false
            
            // 如果有真实的抖音URL，等待WebView加载并提取信息
            if (userHomeUrl.contains("douyin.com") || userHomeUrl.contains("v.douyin.com")) {
                // 等待WebView加载并提取信息，设置超时
                kotlinx.coroutines.delay(10000) // 10秒超时
                if (extractionStatus == ExtractionStatus.LOADING) {
                    // 超时后标记为提取失败
                    extractionStatus = ExtractionStatus.ERROR
                    accountInfo = null
                    isLoading = false
                    canBindAccount = false
                }
            } else {
                // 非抖音链接，直接标记为提取失败
                extractionStatus = ExtractionStatus.ERROR
                accountInfo = null
                isLoading = false
                canBindAccount = false
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
                    // 下拉菜单按钮
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
                                    Icon(Icons.Filled.BugReport, contentDescription = "网页调试")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) {
        innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WebView 调试区域 - 在所有内容之前显示
            item {
                WebViewControlSection(
                    url = userHomeUrl.ifEmpty { "about:blank" },
                    isVisible = showWebView,
                    canBindAccount = canBindAccount,
                    onVisibilityChange = { showWebView = it },
                    onAccountInfoExtracted = { avatar, nickname, isLive, userId, loadStatus, actualUrl ->
                        // 根据load状态判断提取结果
                        if (loadStatus == "FOUND") {
                            // 提取成功，更新userHomeUrl为实际的URL
                            userRealHomeUrl = actualUrl ?: userHomeUrl
                            extractionStatus = ExtractionStatus.SUCCESS
                            accountInfo = UserAccountInfo(
                                avatar = avatar,
                                nickname = nickname,
                                isLive = isLive,
                                userId = userId
                            )
                            canBindAccount = true
                            // 重置WebView URL以防止重复加载
                            userHomeUrl = "about:blank"
                        } else {
                            if (!canBindAccount && userHomeUrl != "about:blank") {
                                // 提取失败 (load值为ERROR)
                                extractionStatus = ExtractionStatus.ERROR
                                accountInfo = null
                                canBindAccount = false
                            }
                            // 重置WebView URL
                            userHomeUrl = "about:blank"
                        }
                        isLoading = false
                    }
                )
            }
            
            // 步骤指示器
            item {
                StepIndicator(
                    currentStep = currentStep,
                    totalSteps = 2
                )
            }
            
            // 根据当前步骤显示不同内容
            item {
                when (currentStep) {
                    BindingStep.URL_INPUT -> {
                        UrlInputStep(
                            accountUrl = accountUrl,
                            onUrlChange = { accountUrl = it },
                            onNextStep = {
                                if (accountUrl.text.isNotBlank()) {
                                    // 提取URL并保存
                                    val extractedUrl = extractUrlFromText(accountUrl.text)
                                    if (extractedUrl != null) {
                                        // 清除之前的状态
                                        bindingResult = null
                                        accountInfo = null
                                        extractionStatus = ExtractionStatus.IDLE
                                        canBindAccount = false
                                        // 设置新的URL，WebView将在后台加载
                                        userHomeUrl = extractedUrl
                                        // 切换到账户预览步骤
                                        currentStep = BindingStep.ACCOUNT_PREVIEW
                                    } else {
                                        // 如果没有找到有效的URL，显示错误提示
                                        bindingResult = BindingResult(false, "未找到有效的网址，请检查输入内容")
                                    }
                                }
                            },
                            isEnabled = accountUrl.text.isNotBlank()
                        )
                    }
                    BindingStep.ACCOUNT_PREVIEW -> {
                        AccountPreviewStep(
                            accountInfo = accountInfo,
                            isLoading = isLoading,
                            onBack = {
                                currentStep = BindingStep.URL_INPUT
                                accountInfo = null
                                bindingResult = null
                                extractionStatus = ExtractionStatus.IDLE
                                canBindAccount = false
                                isCountingDown = false
                                countdownSeconds = 0
                                // 返回时隐藏WebView调试界面
                                showWebView = false
                            },
                            onBind = {
                                isLoading = true
                                bindingTrigger = true
                            },
                            bindingResult = bindingResult,
                            extractionStatus = extractionStatus,
                            canBindAccount = canBindAccount,
                            countdownSeconds = countdownSeconds,
                            isCountingDown = isCountingDown
                        )
                    }
                }
            }

            // 帮助信息
            item {
                HelpSection()
            }
        }
    }
}

// URL输入步骤组件
@Composable
private fun UrlInputStep(
    accountUrl: TextFieldValue,
    onUrlChange: (TextFieldValue) -> Unit,
    onNextStep: () -> Unit,
    isEnabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 步骤标题
        Text(
            text = "步骤一：填写抖音账户主页地址",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 输入框标签
        Text(
            text = "请输入您的抖音账户主页地址：",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // 多行输入框
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            BasicTextField(
                value = accountUrl,
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .defaultMinSize(minHeight = 72.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 3,
                maxLines = 5,
                decorationBox = { innerTextField ->
                    if (accountUrl.text.isEmpty()) {
                        Text(
                            text = "例如：0- 长按复制此条消息，打开抖音搜索，查看TA的更多作品。 https://v.douyin.com/Uxkosllosfsdfso/ 7$2.xxx :wam",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
        
        // 下一步按钮
        Button(
            onClick = onNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isEnabled
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "下一步",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "下一步",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountBindingScreenPreview() {
    MaterialTheme {
        AccountBindingScreen(navController = remember { NavigationState() })
    }
}

// 步骤指示器组件
@Composable
private fun StepIndicator(
    currentStep: BindingStep,
    totalSteps: Int
) {
    val currentStepNumber = when (currentStep) {
        BindingStep.URL_INPUT -> 1
        BindingStep.ACCOUNT_PREVIEW -> 2
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSteps) {
                // 步骤圆圈
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= currentStepNumber) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (i < currentStepNumber) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已完成",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = i.toString(),
                            color = if (i <= currentStepNumber) Color.White else MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 连接线
                if (i < totalSteps) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(2.dp)
                            .background(
                                if (i < currentStepNumber) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

// 账户预览步骤组件
@Composable
private fun AccountPreviewStep(
    accountInfo: UserAccountInfo?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onBind: () -> Unit,
    bindingResult: BindingResult?,
    extractionStatus: ExtractionStatus,
    canBindAccount: Boolean,
    countdownSeconds: Int,
    isCountingDown: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 步骤标题
        Text(
            text = "步骤二：确认账户信息",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 账户信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "正在加载账户信息...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                accountInfo?.let { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 头像
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = info.avatar,
                                contentDescription = "头像",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape),
                                fallback = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                                error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 账户信息
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = info.nickname,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // 直播状态
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (info.isLive) Color.Red else Color.Gray
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (info.isLive) "正在直播" else "未开播",
                                    fontSize = 14.sp,
                                    color = if (info.isLive) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } ?: run {
                    // 获取账户信息失败时的提示
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "警告",
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "无法获取账户信息",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "可能是网络问题或网址不正确，您可以返回重试或重新复制网址",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
        
        // 信息提取状态提示
        when (extractionStatus) {
            ExtractionStatus.ERROR -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "提取信息失败",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "请返回上一步确认用户主页链接是否复制正确",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                // 绑定结果提示
                bindingResult?.let { result ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (result.success) 
                                MaterialTheme.colorScheme.primaryContainer
                            else 
                                MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (result.success) 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = result.message,
                                fontSize = 16.sp,
                                color = if (result.success) 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else 
                                    MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
        
        // 按钮组
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 返回按钮
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "返回",
                        fontSize = 16.sp
                    )
                }
            }
            
            // 立即绑定按钮
            Button(
                onClick = onBind,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading && bindingResult == null && canBindAccount
            ) {
                if (isLoading) {
                    if (isCountingDown && countdownSeconds > 0) {
                        // 显示倒计时
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "绑定中 ${countdownSeconds}s",
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        // 正在处理中
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "处理中...",
                                fontSize = 16.sp
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "绑定",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "立即绑定",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// WebView控制组件
@Composable
private fun WebViewControlSection(
    url: String,
    isVisible: Boolean,
    canBindAccount: Boolean,
    onVisibilityChange: (Boolean) -> Unit = {},
    onAccountInfoExtracted: (String, String, Boolean, String, String, String?) -> Unit = { _, _, _, _, _, _ -> }
) {
    var webViewHeight by remember { mutableStateOf(400.dp) }
    
    Column {
        // 后台WebView - 始终存在但隐藏，用于数据提取
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)

                            // 执行JavaScript获取网页内容高度
                            view?.evaluateJavascript(
                                "(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, 400);})();"
                            ) { result ->
                                try {
                                    val height = result.toFloatOrNull() ?: 400f
                                    val density = context.resources.displayMetrics.density
                                    // 限制最大高度为600dp，最小高度为400dp
                                    val newHeight = ((height / density).coerceIn(400f, 600f)).dp
                                    webViewHeight = newHeight
                                } catch (e: Exception) {
                                    webViewHeight = 400.dp
                                }
                            }
                            
                            // 延迟1秒后开始提取账户信息，确保DOM完全渲染
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                // 打印HTML内容到控制台
//                                view?.evaluateJavascript(
//                                    "(function(){return document.documentElement.outerHTML;})();"
//                                ) { result ->
//                                    try {
//                                        // 移除JavaScript返回值的引号包装
//                                        val htmlContent = result.removeSurrounding("\"")
//                                            .replace("\\\"", "\"")  // 处理转义的引号
//                                            .replace("\\n", "\n")     // 处理转义的换行符
//                                            .replace("\\t", "\t")     // 处理转义的制表符
//
//                                        println("=== WebView HTML Content Start ===")
//                                        println("URL: $url")
//                                        println("HTML Length: ${htmlContent.length} characters")
//                                        println("--- HTML Content ---")
//                                        println(htmlContent)
//                                        println("=== WebView HTML Content End ===")
//                                    } catch (e: Exception) {
//                                        println("Error printing HTML content: ${e.message}")
//                                    }
//                                }
                                
                                // 提取账户信息 - 仅当URL包含抖音链接时执行
                                if (!canBindAccount && url?.contains("https://www.douyin.com/user/") == true) {
                                    println("延迟1秒后开始提取账户信息: $url")
                                    view?.evaluateJavascript(
                                        """
                                        (function() {
    try {
        console.log('开始提取抖音账户信息...');
        var infoObj = document.querySelector("#user_detail_element")
        if (!infoObj) {
            console.log('未找到 #user_detail_element 元素');
            return {
                js: "user_detail_element", 
                load: "ERROR", 
                status: 'NO', 
                id: '',
                nickname: '',
                avatar: ''
            }
        }
        console.log('找到用户信息元素，开始解析...');
        var liveStatusElement = infoObj.querySelector("div > div:nth-child(2) > div");
        var liveStatus = liveStatusElement ? liveStatusElement.innerText == '直播中' : false;
        var nicknameElement = infoObj.querySelector("div:nth-child(2) > div:nth-child(2) > div");
        var userIdElement = infoObj.querySelector("div:nth-child(2) > div:nth-child(2) > p > span");
        var avatarElement = document.querySelector("#search-result-container > div:nth-child(3) > ul > li:nth-child(1) > div > a");
        
        var result = {
            js: "user_detail_element", 
            load: "FOUND", 
            status: liveStatus ? 'PLAYING' : 'NO', 
            id: userIdElement ? userIdElement.innerText.trim().replace('抖音号：', '') : '',
            nickname: nicknameElement ? nicknameElement.innerText : '未知用户',
            avatar: avatarElement ? avatarElement.src : '',
            html: document.body.outerHTML
        };
        console.log('账户信息提取完成:', result);
        return result;
    } catch (e) {
        console.error('提取账户信息时发生错误:', e);
        return {
            js: "user_detail_element", 
            load: "ERROR", 
            status: 'NO', 
            id: '',
            nickname: '',
            avatar: ''
        }
    }
})()
                                        """.trimIndent()
                                    ) { result ->
                                        println("=== WebView JSON Content Start ===")
                                        println("URL: $url")
                                        println(result)
                                        println("=== WebView JSON Content END ===")
                                        try {
                                            // 解析JSON结果
                                            val jsonResult = result.removeSurrounding("\"").replace("\\u003C", "<").toString()
                                            val loadMatch = Regex("\"load\":\"([^\"]*)\"").find(jsonResult)
                                            val loadStatus = loadMatch?.groupValues?.get(1) ?: "ERROR"

                                            if (loadStatus == "FOUND") {
                                                val gson = Gson()
                                                gson.fromJson(jsonResult, Map::class.java)
                                                    ?.let { jsonMap ->
                                                        val html = jsonMap["html"]?.toString()?.replace("\\\"", "")
                                                        val avatarMatch = Regex("avatar300Url:(https:[^,]*)").find(html ?: "")
                                                        val avatar = avatarMatch?.groupValues?.get(1) ?: ""

                                                        val nickname = jsonMap["nickname"]?.toString()?.split("\n")?.firstOrNull() ?: "未知用户"
                                                        val userId = jsonMap["id"]?.toString() ?: ""
                                                        val isLive = jsonMap["status"]?.toString() == "PLAYING"

                                                        println("提取成功 - 昵称: $nickname, 用户ID: $userId, 直播状态: $isLive")
                                                        onAccountInfoExtracted(
                                                            avatar,
                                                            nickname,
                                                            isLive,
                                                            userId,
                                                            loadStatus,
                                                            url // 传递实际的URL
                                                        )
                                                    }
                                            } else {
                                                // 提取失败
                                                println("账户信息提取失败: $loadStatus")
                                                onAccountInfoExtracted("", "", false, "", loadStatus, url)
                                            }
                                        } catch (e: Exception) {
                                            // 解析失败
                                            println("解析账户信息时发生异常: ${e.message}")
                                            onAccountInfoExtracted("", "", false, "", "ERROR", url)
                                        }
                                    }
                                }
                            }, 2000) // 延迟1000毫秒（1秒）
                        }
                    }
                    WebView.setWebContentsDebuggingEnabled(true)
                    settings.apply {
                        javaScriptEnabled = true
                        loadWithOverviewMode = true
                        displayZoomControls = false
                        setSupportZoom(true)
                        allowContentAccess = true
                        javaScriptCanOpenWindowsAutomatically = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        setDatabaseEnabled(true)
                        setGeolocationEnabled(true)
                        // 设置为PC浏览器模式
                        userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    }
                    loadUrl(url.ifEmpty { "about:blank" })
                }
            },
            update = { webView ->
                // 当URL状态改变时，重新加载WebView
                if (url.isNotEmpty()) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.size(0.dp) // 隐藏后台WebView
        )
        
        // 调试界面 - 仅在需要时显示
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeIn(animationSpec = tween(durationMillis = 300)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 300)
            ) + fadeOut(animationSpec = tween(durationMillis = 300))
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // WebView标题栏
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "网页",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "抖音主页预览",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "调试模式",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isVisible,
                            onCheckedChange = onVisibilityChange,
                            modifier = Modifier.height(24.dp),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        )
                    }
                    
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        thickness = 1.dp
                    )
                    
                    // 显示WebView内容 - 显示版本WebView
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        
                                        // 打印HTML内容到控制台(调试WebView)
                                        view?.evaluateJavascript(
                                            "(function(){return document.documentElement.outerHTML;})();"
                                        ) { result ->
                                            try {
                                                // 移除JavaScript返回值的引号包装
                                                val htmlContent = result.removeSurrounding("\"")
                                                    .replace("\\\"", "\"")  // 处理转义的引号
                                                    .replace("\\n", "\n")     // 处理转义的换行符
                                                    .replace("\\t", "\t")     // 处理转义的制表符
                                                
                                                println("=== Debug WebView HTML Content Start ===")
                                                println("URL: $url")
                                                println("HTML Length: ${htmlContent.length} characters")
                                                println("--- Debug HTML Content ---")
                                                println(htmlContent)
                                                println("=== Debug WebView HTML Content End ===")
                                            } catch (e: Exception) {
                                                println("Error printing debug HTML content: ${e.message}")
                                            }
                                        }
                                    }
                                }
                                WebView.setWebContentsDebuggingEnabled(true)
                                settings.apply {
                                    javaScriptEnabled = true
                                    loadWithOverviewMode = true
                                    displayZoomControls = false
                                    setSupportZoom(true)
                                    allowContentAccess = true
                                    javaScriptCanOpenWindowsAutomatically = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    setDatabaseEnabled(true)
                                    setGeolocationEnabled(true)
                                    // 设置为PC浏览器模式
                                    userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                                }
                                loadUrl(url.ifEmpty { "about:blank" })
                            }
                        },
                        update = { webView ->
                            // 当URL状态改变时，重新加载调试WebView
                            if (url.isNotEmpty()) {
                                webView.loadUrl(url)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(webViewHeight)
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

// 帮助信息组件
@Composable
private fun HelpSection() {
    Text(
        text = "帮助信息",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    
    var webViewHeight by remember { mutableStateOf(500.dp) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            
                            // 打印HTML内容到控制台(帮助信息WebView)
                            view?.evaluateJavascript(
                                "(function(){return document.documentElement.outerHTML;})();"
                            ) { result ->
                                try {
                                    // 移除JavaScript返回值的引号包装
                                    val htmlContent = result.removeSurrounding("\"")
                                        .replace("\\\"", "\"")  // 处理转义的引号
                                        .replace("\\n", "\n")     // 处理转义的换行符
                                        .replace("\\t", "\t")     // 处理转义的制表符
                                    
                                    println("=== Help WebView HTML Content Start ===")
                                    println("URL: $url")
                                    println("HTML Length: ${htmlContent.length} characters")
                                    println("--- Help HTML Content ---")
                                    println(htmlContent)
                                    println("=== Help WebView HTML Content End ===")
                                } catch (e: Exception) {
                                    println("Error printing help HTML content: ${e.message}")
                                }
                            }
                            
                            // 执行JavaScript获取网页内容高度
                            view?.evaluateJavascript(
                                "(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, 500);})();"
                            ) { result ->
                                try {
                                    val height = result.toFloatOrNull() ?: 500f
                                    val density = context.resources.displayMetrics.density
                                    // 限制最大高度为800dp，最小高度为500dp
                                    val newHeight = ((height / density).coerceIn(500f, 800f)).dp
                                    webViewHeight = newHeight
                                } catch (e: Exception) {
                                    webViewHeight = 500.dp
                                }
                            }
                        }
                    }
                    loadUrl("https://vip.hc2099.com/android/help/v2/dy-user-home.htm")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(webViewHeight)
        )
    }
}