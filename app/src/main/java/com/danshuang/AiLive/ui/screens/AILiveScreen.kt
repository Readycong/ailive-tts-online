package com.danshuang.AiLive.ui.screens

import android.util.Log
import android.webkit.JavascriptInterface
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Switch
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.service.LiveChattingService
import com.danshuang.AiLive.data.model.ConnectionState
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.util.Utils
import com.danshuang.AiLive.util.DouyinHackUtil
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.get
import kotlin.text.contains
import kotlin.text.isNotEmpty

// 直播间动态信息数据类
data class LiveMessage(
    val id: Long,
    val timestamp: String,
    val type: MessageType,
    val content: String,
    val userName: String? = null
)

// 消息类型枚举
enum class MessageType {
    COMMENT,        // 观众评论
    GIFT,           // 礼物
    FOLLOW,         // 关注
    ENTER,          // 进入直播间
    SYSTEM,         // 系统消息
    AI_RESPONSE     // AI回复
}

// 全局的消息插入接口
object LiveMessageManager {
    private val _messages = mutableStateListOf<LiveMessage>()
    val messages: List<LiveMessage> get() = _messages
    
    private var messageIdCounter = 0L
    
    // 插入新消息的接口
    fun insertMessage(
        type: MessageType,
        content: String,
        userName: String? = null
    ) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val newMessage = LiveMessage(
            id = ++messageIdCounter,
            timestamp = timestamp,
            type = type,
            content = content,
            userName = userName
        )
        _messages.add(newMessage)
        
        // 保持最多1000条消息
        if (_messages.size > 1000) {
            _messages.removeAt(0)
        }
    }
    
    // 清空消息
    fun clearMessages() {
        _messages.clear()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AILiveScreen(
    navController: NavigationState? = null
) {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) } // 添加LoginRepository
    val userProfile by loginRepository.userProfile.collectAsState(initial = com.danshuang.AiLive.data.model.UserProfile()) // 获取UserProfile
    
    // 监控服务状态
    val pullingState by LiveRoomPullingService.pullingStateFlow.collectAsState()
    val connectionState by LiveRoomPullingService.connectionStateFlow.collectAsState()
    val errorMessage by LiveRoomPullingService.errorMessageFlow.collectAsState()
    
    // 根据服务状态确定开关状态
    var liveMonitorEnabled = pullingState == LiveRoomPullingService.PULLING_STATE_MONITORING
    
    // AI直播语音播放状态
    val chattingState by LiveChattingService.chattingStateFlow.collectAsState()
    val liveChattingServiceRunning by LiveChattingService.isServiceRunning.collectAsState()
    var aiLiveEnabled by remember { mutableStateOf(false) }
    
    // 根据服务运行状态更新UI状态
    LaunchedEffect(liveChattingServiceRunning) {
        aiLiveEnabled = liveChattingServiceRunning
    }
    var showWebView by remember { mutableStateOf(true) } // 控制WebView显示状态
    var liveUrl by remember { mutableStateOf("about:blank") } // Webview默认显示空白页，只当启动监控才打开真实页面
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope() // 使用Compose的协程作用域
    var autoScrollEnabled by remember { mutableStateOf(true) } // 控制自动滚动
    var userScrolling by remember { mutableStateOf(false) } // 检测用户手动滚动
    var lastScrollTime by remember { mutableStateOf(0L) } // 防抖动时间戳
    var forceScrollToBottom by remember { mutableStateOf(false) } // 强制滚动到底部标记
    val messageCount = LiveMessageManager.messages.size // 监听消息数量变化
    var roomId by remember { mutableStateOf("") }
    var pushId by remember { mutableStateOf("") }
    var liveName by remember { mutableStateOf("") }
    // 检测是否在列表底部 - 优化精确度
    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = LiveMessageManager.messages.size
            
            if (visibleItems.isEmpty() || totalItems == 0) {
                true
            } else {
                val lastVisibleIndex = visibleItems.lastOrNull()?.index ?: 0
                val lastItemInfo = visibleItems.lastOrNull()
                
                // 检查是否显示最后一项，且最后一项完全可见
                if (lastVisibleIndex >= totalItems - 1) {
                    lastItemInfo?.let { item ->
                        val itemBottom = item.offset + item.size
                        val listBottom = layoutInfo.viewportEndOffset
                        // 确保最后一项的底部在可视区域内
                        itemBottom <= listBottom + 10 // 10dp容错
                    } ?: false
                } else {
                    false
                }
            }
        }
    }
    
    // 监听用户滚动行为 - 优化立即停止逻辑
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress && !forceScrollToBottom) {
            // 用户开始手动滚动时，立即停止所有自动行为
            userScrolling = true
            autoScrollEnabled = false // 立即停止自动滚动
            lastScrollTime = System.currentTimeMillis()
        } else if (!listState.isScrollInProgress) {
            // 滚动停止后等待一段时间确保稳定
            delay(500)
            userScrolling = false
            forceScrollToBottom = false // 重置强制滚动标记
            
            // 只有在用户滚动到底部时才重新开启自动滚动
            if (isAtBottom) {
                autoScrollEnabled = true
            }
        }
    }
    
    // 监听消息数量变化，实现新消息自动滚动
    LaunchedEffect(messageCount) {
        // 只有在自动滚动开启且不是用户手动滚动时才执行
        if (messageCount > 0 && autoScrollEnabled && !userScrolling && !listState.isScrollInProgress) {
            delay(100) // 等待UI更新完成
            
            // 再次检查状态，确保用户没有在此期间开始滚动
            if (autoScrollEnabled && !userScrolling && !listState.isScrollInProgress) {
                try {
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            index = messageCount - 1,
                            scrollOffset = 0
                        )
                    }
                } catch (e: Exception) {
                    // 忽略滚动异常
                }
            }
        }
    }

    // 模拟自动生成消息 - 仅在AI直播开启时生成模拟消息
    LaunchedEffect(aiLiveEnabled) {
        if (false) {
            while (true) {
                delay(3000) // 每3秒生成一条消息
                
                // 检查是否允许自动滚动：只有在未手动滚动且允许自动滚动时才执行
                if (!userScrolling && autoScrollEnabled && !listState.isScrollInProgress) {
                    generateMockMessage()
                    
                    // 再次检查用户是否开始滚动
                    if (!userScrolling && autoScrollEnabled && LiveMessageManager.messages.isNotEmpty()) {
                        // 添加短暂延迟让UI更新完成
                        delay(150)
                        
                        // 第三次检查，确保用户没有在此期间开始滚动
                        if (!userScrolling && autoScrollEnabled && !listState.isScrollInProgress) {
                            try {
                                // 使用更精确的滚动方式
                                listState.animateScrollToItem(
                                    index = LiveMessageManager.messages.size - 1,
                                    scrollOffset = 0
                                )
                            } catch (e: Exception) {
                                // 忽略滚动过程中的异常
                            }
                        }
                    }
                } else {
                    // 即使不能滚动，也要生成消息，保持直播实时性
                    generateMockMessage()
                }
            }
        }
    }
    
    // 使用Scaffold来组织TopBar和内容
    Scaffold(
        topBar = {
            AILiveTopBar(
                onNavigateBack = { navController?.navigateUp() },
                onWebDebugClick = { showWebView = !showWebView } // 切换WebView显示状态
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                // 上部：左右布局的两个Switch按钮
                SwitchControlSection(
                    liveMonitorEnabled = liveMonitorEnabled,
                    onLiveMonitorChanged = { enabled ->
                        if (enabled) {
                            LiveRoomPullingService.startService(context)
                            // 当开启直播监控时，设置WebView的URL为userProfile.boundAccount.homeUrl
                            userProfile.boundAccount?.homeUrl?.let { url ->
                                if (url.isNotEmpty()) {
                                    liveUrl = url
                                }
                            }
                        } else {
                            LiveRoomPullingService.stopService(context)
                            // 当关闭直播监控时，重置WebView的URL
                            liveUrl = "about:blank"
                        }
                    },
                    aiLiveEnabled = aiLiveEnabled,
                    onAiLiveChanged = { enabled ->
                        if (enabled) {
                            // 检查服务是否已经在运行
                            if (!liveChattingServiceRunning) {
                                LiveChattingService.startService(context)
                                aiLiveEnabled = true
                            } else {
                                // 服务已经在运行，只更新UI状态
                                aiLiveEnabled = true
                            }
                        } else {
                            LiveChattingService.stopService(context)
                            aiLiveEnabled = false
                        }
                    },
                    connectionState = connectionState,
                    errorMessage = errorMessage,
                    chattingState = chattingState
                )
            }
            
            item {
                // WebView控制区域
                WebViewControlSection(
                    url = liveUrl,
                    isVisible = showWebView,
                    liveMonitorEnabled = liveMonitorEnabled,
                    roomId = roomId,
                    pushId = pushId,
                    liveName = liveName,
                    onLiveRoomInfoReceived = { newRoomId, newPushId, newLiveName ->
                        roomId = newRoomId
                        pushId = newPushId
                        liveName = newLiveName
                    },
                    onVisibilityChange = { showWebView = it }
                )
            }
            
            item {
                // 下部：实时刷新的直播间动态信息列表
                LiveMessageList(
                    modifier = Modifier.fillMaxWidth(),
                    listState = listState,
                    isAtBottom = isAtBottom,
                    showWebView = showWebView,
                    onScrollToBottom = {
                        // 立即启用自动滚动并重置用户滚动状态
                        autoScrollEnabled = true
                        userScrolling = false
                        forceScrollToBottom = true
                        
                        // 立即滚动到底部
                        if (LiveMessageManager.messages.isNotEmpty()) {
                            coroutineScope.launch {
                                try {
                                    // 先用不带动画的方式快速滚动
                                    listState.scrollToItem(
                                        index = LiveMessageManager.messages.size - 1,
                                        scrollOffset = 0
                                    )
                                    // 等待一帧后再次确认位置
                                    delay(50)
                                    listState.animateScrollToItem(
                                        index = LiveMessageManager.messages.size - 1,
                                        scrollOffset = 0
                                    )
                                    // 滚动完成后重置强制滚动标记
                                    delay(100)
                                    forceScrollToBottom = false
                                } catch (e: Exception) {
                                    // 降级到普通滚动
                                    try {
                                        listState.scrollToItem(LiveMessageManager.messages.size - 1)
                                        delay(100)
                                        forceScrollToBottom = false
                                    } catch (e2: Exception) {
                                        // 忽略所有错误
                                        forceScrollToBottom = false
                                    }
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AILiveTopBar(
    onNavigateBack: () -> Unit = {},
    onWebDebugClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    TopAppBar(
        title = {
            Text(
                text = "AI直播监控",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        navigationIcon = {
            IconButton(
                onClick = onNavigateBack
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        actions = {
            Box {
                IconButton(
                    onClick = { showMenu = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "更多选项",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "网页调试",
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            showMenu = false
                            onWebDebugClick() // 调用WebView显示切换
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "网页调试",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "清空消息",
                                fontSize = 16.sp
                            )
                        },
                        onClick = {
                            LiveMessageManager.clearMessages()
                            showMenu = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = "清空消息",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun SwitchControlSection(
    liveMonitorEnabled: Boolean,
    onLiveMonitorChanged: (Boolean) -> Unit,
    aiLiveEnabled: Boolean,
    onAiLiveChanged: (Boolean) -> Unit,
    connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    errorMessage: String? = null,
    chattingState: Int = LiveChattingService.CHATTING_STATE_STOPPED
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface // 使用surface作为背景
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 直播间监控状态
            SwitchItem(
                title = "直播间监控状态",
                checked = liveMonitorEnabled,
                onCheckedChange = onLiveMonitorChanged,
                icon = Icons.Default.Videocam,
                modifier = Modifier.weight(1f),
                connectionState = connectionState,
                errorMessage = errorMessage
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // AI直播状态
            SwitchItem(
                title = "AI直播状态",
                checked = aiLiveEnabled,
                onCheckedChange = onAiLiveChanged,
                icon = Icons.Default.SmartToy,
                modifier = Modifier.weight(1f),
                chattingState = chattingState
            )
        }
    }
}

@Composable
private fun SwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    connectionState: ConnectionState? = null,
    errorMessage: String? = null,
    chattingState: Int? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                uncheckedThumbColor = MaterialTheme.colorScheme.outline, // unchecked拇指颜色
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant, // unchecked轨道颜色
                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) // 边框颜色
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 根据chattingState显示不同的标题
        val displayTitle = if (chattingState != null && chattingState == LiveChattingService.CHATTING_STATE_PLAYING) {
            "$title (播放中)"
        } else {
            title
        }
        
        Text(
            text = displayTitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            color = if (checked) 
                MaterialTheme.colorScheme.onSurface 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // unchecked时文字也更淡
        )
        
        // 显示连接状态（仅对直播间监控显示）
        if (false) {
//        if (connectionState != null) {
            Spacer(modifier = Modifier.height(4.dp))
            
            val statusText = when (connectionState) {
                ConnectionState.CONNECTED -> "已连接"
                ConnectionState.CONNECTING -> "连接中"
                ConnectionState.DISCONNECTED -> "已断开"
                ConnectionState.ERROR -> "连接错误"
                else -> "ERRRR"
            }
            
            val statusColor = when (connectionState) {
                ConnectionState.CONNECTED -> Color(0xFF4CAF50)
                ConnectionState.CONNECTING -> Color(0xFFFF9800)
                ConnectionState.DISCONNECTED -> Color(0xFF9E9E9E)
                ConnectionState.ERROR -> Color(0xFFFF5722)
                else -> Color(0xFFFFFFFF)
            }
            
            Text(
                text = statusText,
                fontSize = 10.sp,
                color = statusColor,
                textAlign = TextAlign.Center
            )
            
            // 显示错误信息
            if (errorMessage != null && connectionState == ConnectionState.ERROR) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = errorMessage,
                    fontSize = 9.sp,
                    color = Color(0xFFFF5722),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WebViewControlSection(
    url: String,
    isVisible: Boolean,
    liveMonitorEnabled: Boolean,
    roomId: String = "",
    pushId: String = "",
    liveName: String = "",
    onLiveRoomInfoReceived: (String, String, String) -> Unit = { _, _, _ -> },
    onVisibilityChange: (Boolean) -> Unit = {}
) {
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
                .fillMaxWidth()
                .height(800.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
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
                        text = "直播主页",
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
                
                // WebView内容
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)

                                    Log.d("WebView", "Page finished loading: $url")
                                    // 添加安全检查
                                    if (url?.startsWith("https://www.douyin.com/user/", true) == false) return
                                    
                                    // 添加空值检查，避免空指针异常
                                    if (view == null) return
                                    
                                    // 防止无限循环调用
                                    if (view.tag == true) {
                                        view.tag = false
                                        return
                                    }
                                    
                                    view.evaluateJavascript(Utils.script) { html ->
                                        addLogMessage("检测开播信息JSON：$html")
                                        // 添加空值检查，避免空指针异常
                                        if (html.isNotEmpty()) {
                                            if (html.contains("user_detail_element", true)) {
                                                addLogMessage("检测到主页信息，开始解析直播间信息...")
                                                var gson = Gson()
                                                gson.fromJson(html, Map::class.java)?.let { jsonMap ->
                                                    val liveUrl = jsonMap["url"]?.toString()
                                                    // 修复空指针异常：检查liveUrl是否为null，避免使用!!操作符
                                                    if (jsonMap["status"]?.toString() != "PLAYING" ||
                                                        (liveUrl == null || liveUrl.indexOf("https://live.douyin.com/") < 0)
                                                    ) {
                                                        addLogMessage("直播间未开播")

                                                        //swLiveMonitorEnabled = false
                                                    } else {
                                                        addLogMessage("进入直播间URL：$liveUrl...")
                                                        //swLiveMonitorEnabled = true

                                                        // 检查URL是否与当前URL相同，避免重复加载
                                                        if (view.url != liveUrl) {
                                                            // 设置标记以防止无限循环
                                                            view.tag = true
                                                            view.loadUrl(liveUrl)
                                                            try {
                                                                JoinLiveRoom(view, onLiveRoomInfoReceived)
                                                            } catch (e: Exception) {
                                                                addLogMessage("直播间连接异常")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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
                        }
                    },
                    update = { webView ->
                        // 当URL变化时重新加载
                        webView?.loadUrl(url)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun LiveMessageList(
    modifier: Modifier = Modifier,
    listState: LazyListState,
    isAtBottom: Boolean,
    onScrollToBottom: () -> Unit,
    showWebView: Boolean = false
) {
    val configuration = LocalConfiguration.current
    
    // 计算屏幕高度
    val screenHeightDp = configuration.screenHeightDp.dp
    
    // 固定高度计算：屏幕高度 - TopAppBar(64dp) - SwitchControlSection(100dp) - 安全边距(40dp)
    // 不再根据WebView状态调整，始终保持一屏高度
    val listHeight = screenHeightDp - 204.dp
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(listHeight)
    ) {
        // 列表标题
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // 固定高度，避免IconButton显示隐藏时的抖动
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize() // 填满Card的固定高度
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Forum,
                    contentDescription = "直播间动态",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "直播间实时动态",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "共 ${LiveMessageManager.messages.size} 条消息",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                
                // 滚动到底部按钮区域 - 固定宽度避免布局变化
                Box(
                    modifier = Modifier.width(40.dp), // 固定宽度
                    contentAlignment = Alignment.Center
                ) {
                    // 滚动到底部按钮（仅在不在底部时显示）
                    if (!isAtBottom && LiveMessageManager.messages.isNotEmpty()) {
                        IconButton(
                            onClick = onScrollToBottom,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerticalAlignBottom,
                                contentDescription = "滚动到底部",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // 占用剩余高度
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                bottom = 24.dp // 增加底部间距，确保最后一条消息完全可见
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp), // 增加消息间距
            // 添加平滑的滚动行为，禁用过度滚动
            userScrollEnabled = true,
            // 优化滚动性能
            reverseLayout = false
        ) {
            items(
                items = LiveMessageManager.messages,
                key = { it.id }
            ) { message ->
                // 添加动画效果
                AnimatedMessageItem(message = message)
            }
        }
    }
}

private fun JoinLiveRoom(liveView: WebView, onLiveRoomInfoReceived: (String, String, String) -> Unit) {
    addLogMessage("正在尝试连接直播WSS...")
    // 不要重新设置webViewClient，这可能会导致循环或其他问题
    // 直接执行需要的操作，而不是重新设置webViewClient
    
    val url = liveView.url
    Log.d("WebViewCookie", "URL: $url")

    // 你可以在这里处理cookie，比如显示到TextView
    liveView.evaluateJavascript(
        "(function() { return document.documentElement.outerHTML; })();"
    ) { html ->
        val cleanHtml = html?.removeSurrounding("\"")?.replace("\\u003C", "<")
            ?.replace("""\\\"""", "\"")
        Log.d("HTML", cleanHtml ?: "null")

        val docHtml = cleanHtml ?: "null"
        var utilObj = DouyinHackUtil(docHtml)

        try {
            // 添加空值检查，避免空指针异常
            val roomId = utilObj.getRoomId()?.toString() ?: ""
            val pushId = utilObj.getPushID()?.toString() ?: ""
            val liveName = utilObj.getLiveName()?.toString() ?: ""
            
            // 通过回调传递获取到的直播间信息
            onLiveRoomInfoReceived(roomId, pushId, liveName)
            
            // 添加空值检查，避免空指针异常
            if (roomId.isNotEmpty() && roomId != "null") {
                addLogMessage("Room ID: $roomId")
                addLogMessage("Push ID: $pushId")
                addLogMessage("Live Name: $liveName")

                //@TODO bindDouyinLive
                addLogMessage("已绑定抖音账号与直播间: $liveName $roomId")

                // 添加空值检查，避免空指针异常
                val userAgent = liveView.settings?.userAgentString
                if (userAgent != null) {
                    val wsUri = utilObj.getWssURL(userAgent, roomId, pushId)
                    Log.d("DouyinHackUtil", "WebSocket URI: $wsUri")
                }
                
                val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                //@TODO 连接WebSocket wsUri，并传入cookie
                hookWebView(liveView)
            }
        } catch (e: Exception) {
            addLogMessage("直播间连接失败，请重新启动直播连接状态检测。")
            return@evaluateJavascript
        }
    }
}

private fun hookWebView(liveView: WebView) {
    liveView.addJavascriptInterface(object : Any() {
        @JavascriptInterface
        fun onXhrResponse(method: String?, url: String?, status: Int, response: String?) {
//                Log.d("XHR", "Method: " + method)
//                Log.d("XHR", "URL: " + url)
//                Log.d("XHR", "Status: " + status)
//                Log.d("XHR", "Response: " + response)
            // 添加空值检查，避免空指针异常
            if (url != null && url.indexOf("https://live.douyin.com/webcast/ranklist/audience") != -1) {
                // 添加空值检查，避免空指针异常
                if (url.isNotEmpty() && response != null && response.isNotEmpty()) {
//                        val currentTime = System.currentTimeMillis()/1000
//                        if ((currentTime - lastUploadTime) < 60) {
//                            Log.d("AudienceAjax", "请求过于频繁，跳过本次请求")
//                            return
//                        }
//                        lastUploadTime = currentTime
                    val audienceUrl = url
                    Log.d("AudienceAjax", "audienceUrl: $audienceUrl")
                    try {
                        val gson = Gson()
                        val audienceData = gson.fromJson(response, JsonObject::class.java)
                        // 添加空值检查，避免空指针异常
                        if (audienceData != null && audienceData.has("data")) {
                            val data = audienceData.getAsJsonObject("data")
                            // 添加空值检查，避免空指针异常
                            if (data != null && data.has("ranks")) {
                                val ranks = data.getAsJsonArray("ranks")
                                val resultMap = mutableMapOf<String, String>()

                                for (rank in ranks) {
                                    // 添加空值检查，避免空指针异常
                                    if (rank.isJsonObject) {
                                        val user = rank.asJsonObject.getAsJsonObject("user")
                                        // 添加空值检查，避免空指针异常
                                        if (user != null) {
                                            val nickname = user.get("nickname")?.asString ?: ""
                                            val webcastUid = user.get("webcast_uid")?.asString ?: ""
                                            resultMap[webcastUid] = nickname
                                        }
                                    }
                                }
                                Log.d("AudienceAjax", "观众数据: $resultMap")
                                try {
                                    //@TODO 上传观众数据到服务器，调用onlineUsers
                                } catch (e: Exception) {
                                    addLogMessage("上传观众数据失败: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        addLogMessage("解析观众数据失败: ${e.message}")
                    }
                }
            }
        }
    }, "Android")

    // 注入 JavaScript 拦截代码
    val jsCode = "(function() {" +
            "originalOpen = XMLHttpRequest.prototype.open;" +
            "originalSend = XMLHttpRequest.prototype.send;" +
            "XMLHttpRequest.prototype.open = function(method, url) {" +
            "    this._url = url;" +
            "    this._method = method;" +
            "    return originalOpen.apply(this, arguments);" +
            "};" +
            "XMLHttpRequest.prototype.send = function(data) {" +
            "    var self = this;" +
            "    this.addEventListener('load', function() {" +
            "        var response = self.response;" +
            "        if (typeof response === 'object') {" +
            "            response = JSON.stringify(response);" +
            "        }" +
            "        Android.onXhrResponse(self._method, self._url, self.status, response);" +
            "    });" +
            "    return originalSend.apply(this, arguments);" +
            "};" +
            "})();"

    liveView.evaluateJavascript(jsCode, null)
    Log.d("AudienceAjax", "WebView hook injected")
}

@Composable
private fun AnimatedMessageItem(message: LiveMessage) {
    // 动画状态
    var visible by remember { mutableStateOf(false) }
    
    // 组件出现时触发动画
    LaunchedEffect(message.id) {
        visible = true
    }
    
    // 添加进入动画
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it / 3 }, // 从下方1/3处滑入
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it / 3 },
            animationSpec = tween(durationMillis = 200)
        ) + fadeOut(
            animationSpec = tween(durationMillis = 200)
        )
    ) {
        MessageItem(message = message)
    }
}

@Composable
private fun MessageItem(message: LiveMessage) {
    val isAIMessage = message.type == MessageType.AI_RESPONSE
    
    // 不同消息类型的颜色和样式
    val backgroundColor = when (message.type) {
        MessageType.AI_RESPONSE -> MaterialTheme.colorScheme.primary // AI主播使用主色
        MessageType.COMMENT -> MaterialTheme.colorScheme.surface
        MessageType.GIFT -> Color(0xFFFFE0B2)
        MessageType.FOLLOW -> Color(0xFFE8F5E8)
        MessageType.ENTER -> Color(0xFFE3F2FD)
        MessageType.SYSTEM -> Color(0xFFFFF3E0)
    }
    
    val textColor = when (message.type) {
        MessageType.AI_RESPONSE -> MaterialTheme.colorScheme.onPrimary // AI消息使用白色文字
        else -> MaterialTheme.colorScheme.onSurface
    }
    
    val iconColor = when (message.type) {
        MessageType.AI_RESPONSE -> MaterialTheme.colorScheme.onPrimary
        MessageType.COMMENT -> MaterialTheme.colorScheme.primary
        MessageType.GIFT -> Color(0xFFFF9800)
        MessageType.FOLLOW -> Color(0xFF4CAF50)
        MessageType.ENTER -> Color(0xFF2196F3)
        MessageType.SYSTEM -> Color(0xFFFF5722)
    }
    
    val icon = when (message.type) {
        MessageType.COMMENT -> Icons.Default.ChatBubble
        MessageType.GIFT -> Icons.Default.CardGiftcard
        MessageType.FOLLOW -> Icons.Default.Favorite
        MessageType.ENTER -> Icons.Default.Login
        MessageType.SYSTEM -> Icons.Default.Info
        MessageType.AI_RESPONSE -> Icons.Default.SmartToy
    }
    
    // 整体容器，AI消息右对齐，其他消息左对齐
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isAIMessage) Arrangement.End else Arrangement.Start
    ) {
        // AI消息在右侧，需要在左侧添加空白
        if (isAIMessage) {
            Spacer(modifier = Modifier.weight(0.2f)) // 左侧留白20%宽度
        }
        
        Card(
            modifier = Modifier
                .wrapContentWidth()
                .widthIn(max = 340.dp), // 限制最大宽度
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(
                topStart = if (isAIMessage) 16.dp else 4.dp,
                topEnd = if (isAIMessage) 4.dp else 16.dp,
                bottomStart = 16.dp,
                bottomEnd = 16.dp
            )
        ) {
            if (isAIMessage) {
                // AI消息布局（右对齐）
                AIMessageContent(
                    message = message,
                    textColor = textColor,
                    icon = icon,
                    iconColor = iconColor
                )
            } else {
                // 普通消息布局（左对齐）
                RegularMessageContent(
                    message = message,
                    textColor = textColor,
                    icon = icon,
                    iconColor = iconColor
                )
            }
        }
        
        // 非AI消息在左侧，需要在右侧添加空白
        if (!isAIMessage) {
            Spacer(modifier = Modifier.weight(0.2f)) // 右侧留白20%宽度
        }
    }
}

@Composable
private fun AIMessageContent(
    message: LiveMessage,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Column(
        modifier = Modifier.padding(12.dp),
        horizontalAlignment = Alignment.End // 右对齐
    ) {
        // 用户名和时间
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            message.userName?.let { userName ->
                Text(
                    text = userName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Icon(
                imageVector = icon,
                contentDescription = message.type.name,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // 消息内容
        Text(
            text = message.content,
            fontSize = 14.sp,
            color = textColor,
            lineHeight = 18.sp,
//            textAlign = TextAlign.End // 右对齐文本
        )
    }
}

@Composable
private fun RegularMessageContent(
    message: LiveMessage,
    textColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Row(
        modifier = Modifier.padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = message.type.name,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                message.userName?.let { userName ->
                    Text(
                        text = userName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = iconColor
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = message.timestamp,
                    fontSize = 10.sp,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
            
            Text(
                text = message.content,
                fontSize = 14.sp,
                color = textColor,
                lineHeight = 18.sp
            )
        }
    }
}

// 模拟生成消息的函数
private fun generateMockMessage() {
    val mockMessages = listOf(
        // 观众消息
        Triple(MessageType.COMMENT, "好棒的直播内容！", "用户001"),
        Triple(MessageType.COMMENT, "主播讲得真好", "小粉丝"),
        Triple(MessageType.COMMENT, "学到了很多东西", "学习达人"),
        Triple(MessageType.GIFT, "送出了一个火箭", "大学生小李"),
        Triple(MessageType.GIFT, "送出了鲜花", "路人甲"),
        Triple(MessageType.FOLLOW, "关注了主播", "新粉丝小王"),
        Triple(MessageType.ENTER, "进入了直播间", "观众小张"),
        Triple(MessageType.SYSTEM, "直播间在线人数达到了100人", null),
        
        // AI主播回复消息（增加频率和多样性）
        Triple(MessageType.AI_RESPONSE, "谢谢大家的支持！接下来为大家带来更精彩的内容", "AI主播"),
        Triple(MessageType.AI_RESPONSE, "感谢送礼物的朋友们，你们的支持是我最大的动力", "AI主播"),
        Triple(MessageType.AI_RESPONSE, "欢迎新朋友们的加入，希望大家在直播间玩得开心", "AI主播"),
        Triple(MessageType.AI_RESPONSE, "今天的内容大家还满意吗？有什么想了解的可以告诉我", "AI主播"),
        Triple(MessageType.AI_RESPONSE, "看到这么多朋友关注，真的很感动，我会继续努力的", "AI主播"),
        Triple(MessageType.AI_RESPONSE, "大家有什么问题都可以在评论区提出来哦", "AI主播"),
        Triple(MessageType.AI_RESPONSE, "感谢大家的陪伴，让我们一起度过愉快的时光", "AI主播")
    )
    
    val randomMessage = mockMessages.random()
    LiveMessageManager.insertMessage(
        type = randomMessage.first,
        content = randomMessage.second,
        userName = randomMessage.third
    )
}

private fun addLogMessage(content: String) {
    Log.d("LiveLog", content)
    LiveMessageManager.insertMessage(
        type = MessageType.SYSTEM,
        content = content,
        userName = null
    )
}

@Preview(showBackground = true)
@Composable
fun AILiveScreenPreview() {
    AiLiveTheme {
        AILiveScreen()
    }
}

// 用户消息预览
@Preview(showBackground = true, name = "用户消息预览")
@Composable
fun UserMessagePreview() {
    AiLiveTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "用户消息样式预览",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // 观众评论消息
            MessageItem(
                message = LiveMessage(
                    id = 1,
                    timestamp = "14:30:25",
                    type = MessageType.COMMENT,
                    content = "主播讲得真好，学到了很多！",
                    userName = "学习达人小明"
                )
            )
            
            // 礼物消息
            MessageItem(
                message = LiveMessage(
                    id = 2,
                    timestamp = "14:31:10",
                    type = MessageType.GIFT,
                    content = "送出了一个火箭",
                    userName = "土豪用户"
                )
            )
            
            // 关注消息
            MessageItem(
                message = LiveMessage(
                    id = 3,
                    timestamp = "14:31:45",
                    type = MessageType.FOLLOW,
                    content = "关注了主播",
                    userName = "新粉丝小王"
                )
            )
            
            // 进入直播间消息
            MessageItem(
                message = LiveMessage(
                    id = 4,
                    timestamp = "14:32:00",
                    type = MessageType.ENTER,
                    content = "进入了直播间",
                    userName = "观众小张"
                )
            )
            
            // 系统消息
            MessageItem(
                message = LiveMessage(
                    id = 5,
                    timestamp = "14:32:30",
                    type = MessageType.SYSTEM,
                    content = "直播间在线人数突破500人！感谢大家的支持！",
                    userName = null
                )
            )
        }
    }
}

// AI主播消息预览
@Preview(showBackground = true, name = "AI主播消息预览")
@Composable
fun AIHostMessagePreview() {
    AiLiveTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "AI主播消息样式预览",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // AI主播短消息
            MessageItem(
                message = LiveMessage(
                    id = 6,
                    timestamp = "14:33:00",
                    type = MessageType.AI_RESPONSE,
                    content = "谢谢大家的支持！",
                    userName = "AI主播"
                )
            )
            
            // AI主播中等长度消息
            MessageItem(
                message = LiveMessage(
                    id = 7,
                    timestamp = "14:33:30",
                    type = MessageType.AI_RESPONSE,
                    content = "感谢刚才送礼物的朋友们，你们的支持是我继续直播的动力！",
                    userName = "AI主播"
                )
            )
            
            // AI主播长消息
            MessageItem(
                message = LiveMessage(
                    id = 8,
                    timestamp = "14:34:15",
                    type = MessageType.AI_RESPONSE,
                    content = "欢迎所有新朋友的加入！今天我们要聊的话题非常有趣，希望大家在直播间能够学到有用的知识，同时也欢迎大家在评论区积极互动，分享自己的想法和经验。",
                    userName = "AI主播"
                )
            )
            
            // AI主播回复消息
            MessageItem(
                message = LiveMessage(
                    id = 9,
                    timestamp = "14:35:00",
                    type = MessageType.AI_RESPONSE,
                    content = "刚才有朋友问到关于学习方法的问题，这确实是一个很好的话题，我来为大家详细解答一下。",
                    userName = "AI主播"
                )
            )
        }
    }
}

// 混合消息流预览
@Preview(showBackground = true, name = "混合消息流预览")
@Composable
fun MixedMessageFlowPreview() {
    AiLiveTheme {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item {
                Text(
                    text = "混合消息流效果预览",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // 模拟真实的消息流
            items(10) { index ->
                val messages = listOf(
                    LiveMessage(index.toLong(), "14:30:${String.format("%02d", index * 5)}", MessageType.COMMENT, "这个内容真的很棒！", "用户${index + 1}"),
                    LiveMessage(index.toLong(), "14:30:${String.format("%02d", index * 5 + 1)}", MessageType.AI_RESPONSE, "谢谢大家的支持，接下来为大家带来更精彩的内容！", "AI主播"),
                    LiveMessage(index.toLong(), "14:30:${String.format("%02d", index * 5 + 2)}", MessageType.GIFT, "送出了鲜花", "粉丝${index + 1}"),
                    LiveMessage(index.toLong(), "14:30:${String.format("%02d", index * 5 + 3)}", MessageType.AI_RESPONSE, "感谢送礼物的朋友！", "AI主播"),
                    LiveMessage(index.toLong(), "14:30:${String.format("%02d", index * 5 + 4)}", MessageType.ENTER, "进入了直播间", "新观众${index + 1}")
                )
                
                AnimatedMessageItem(message = messages[index % messages.size])
            }
        }
    }
}