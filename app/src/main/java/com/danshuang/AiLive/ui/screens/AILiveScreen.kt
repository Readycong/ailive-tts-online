package com.danshuang.AiLive.ui.screens

import android.util.Log
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.webkit.WebView
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.service.LiveChattingService
import com.danshuang.AiLive.data.model.ConnectionState
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.api.ServerApiUtilsV2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
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

// 添加新的数据类用于消息发送状态管理
enum class SendMode {
    TEXT,  // 原文模式
    CHAT     // AI CHAT模式
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AILiveScreen(
    navController: NavigationState? = null
) {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) } // 添加LoginRepository
    val userProfile by loginRepository.userProfile.collectAsState(initial = com.danshuang.AiLive.data.model.UserProfile()) // 获取UserProfile
    val tokenManager = remember { TokenManager(loginRepository) }
    val serverApiUtils = remember { ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository) }

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
    var showWebView by remember { mutableStateOf(false) } // 控制WebView显示状态
    var liveUrl by remember { mutableStateOf("about:blank") } // Webview默认显示空白页，只当启动监控才打开真实页面
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope() // 使用Compose的协程作用域
    var autoScrollEnabled by remember { mutableStateOf(true) } // 控制自动滚动
    var userScrolling by remember { mutableStateOf(false) } // 检测用户手动滚动
    var lastScrollTime by remember { mutableStateOf(0L) } // 防抖动时间戳
    var forceScrollToBottom by remember { mutableStateOf(false) } // 强制滚动到底部标记
    val messageCount = LiveMessageManager.messages.size // 监听消息数量变化
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

    // 消息发送状态管理
    var sendMode by remember { mutableStateOf(SendMode.TEXT) }
    var messageText by remember { mutableStateOf("") }
    var isSendEnabled by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 监听消息文本变化，更新发送按钮状态
    LaunchedEffect(messageText) {
        isSendEnabled = messageText.isNotBlank()
    }

    // 使用Scaffold来组织TopBar和内容
    Scaffold(
        topBar = {
            AILiveTopBar(
                onNavigateBack = { navController?.navigateUp() },
                onWebDebugClick = {
                    showWebView = !showWebView
                    if (showWebView) {
                        // 当开启直播监控时，设置WebView的URL为userProfile.boundAccount.homeUrl
                        userProfile.boundAccount?.homeUrl?.let { url ->
                            if (url.isNotEmpty()) {
                                liveUrl = url
                            }
                        }
                        
                        // 滚动到WebView位置
                        coroutineScope.launch {
                            // 延迟一小段时间确保UI更新完成
                            delay(1000)
                            // 滚动到LazyColumn的最后一项（WebView位置）
                            listState.animateScrollToItem(
                                index = 2, // 0: SwitchControlSection, 1: LiveMessageList, 2: WebViewControlSection
                                scrollOffset = 0
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(bottom = if (aiLiveEnabled) 80.dp else 16.dp) // 根据MessageSendBar是否显示调整底部padding
            ) {
                item {
                    // 上部：左右布局的两个Switch按钮
                    SwitchControlSection(
                        liveMonitorEnabled = liveMonitorEnabled,
                        onLiveMonitorChanged = { enabled ->
                            if (enabled) {
                                LiveRoomPullingService.startService(
                                    context,
                                    wssUrl = "",
                                    cookieString = ""
                                )
                            } else {
                                LiveRoomPullingService.stopService(context)
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
                    // 下部：实时刷新的直播间动态信息列表
                    LiveMessageList(
                        modifier = Modifier.fillMaxWidth(),
                        listState = listState,
                        isAtBottom = isAtBottom,
                        onScrollToBottom = {
                            // 立即启用自动滚动并重置用户滚动状态
                            autoScrollEnabled = true
                            userScrolling = false
                            forceScrollToBottom = true

                            // 立即滚动到底部
                            if (LiveMessageManager.messages.isNotEmpty()) {
                                coroutineScope.launch {
                                    try {
                                        listState.scrollToItem(
                                            index = LiveMessageManager.messages.size,
                                            scrollOffset = 0
                                        )
                                        // 等待一帧后再次确认位置
                                        delay(50)
                                        listState.animateScrollToItem(
                                            index = LiveMessageManager.messages.size,
                                            scrollOffset = 0
                                        )
                                        // 滚动完成后重置强制滚动标记
                                        delay(100)
                                        forceScrollToBottom = false
                                    } catch (e: Exception) {
                                        // 降级到普通滚动
                                        try {
                                            listState.scrollToItem(LiveMessageManager.messages.size)
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

                item {
                    // WebView控制区域
                    WebViewControlSection(
                        url = liveUrl,
                        isVisible = showWebView,
                        onVisibilityChange = { showWebView = it }
                    )
                }
            }

            // 消息发送控件 - 固定在底部
            // 只有当AI直播状态开启时才显示消息发送栏
            if (aiLiveEnabled) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    MessageSendBar(
                        sendMode = sendMode,
                        onSendModeChange = { sendMode = it },
                        messageText = messageText,
                        onMessageTextChange = { messageText = it },
                        isSendEnabled = isSendEnabled && !isSending,
                        isSending = isSending,
                        onSendClick = {
                            coroutineScope.launch {
                                // 获取当前绑定账户信息
                                val douyinId = userProfile.boundAccount?.userId ?: ""
                                // roomId 需要通过其他方式获取，暂时使用 userId 作为替代
                                val roomId = userProfile.boundAccount?.userId ?: ""
                                
                                // 检查必要信息
                                if (douyinId.isEmpty() || roomId.isEmpty()) {
                                    snackbarHostState.showSnackbar("请先绑定抖音账号并进入直播间")
                                    return@launch
                                }
                                
                                if (messageText.isBlank()) {
                                    snackbarHostState.showSnackbar("请输入要发送的消息")
                                    return@launch
                                }
                                
                                // 设置发送状态
                                isSending = true
                                
                                try {
                                    // 调用API发送消息
                                    val type = if (sendMode == SendMode.TEXT) "text" else "chat"
                                    val result = serverApiUtils.addHostComment(
                                        douyinId = douyinId,
                                        roomId = roomId,
                                        type = type,
                                        content = messageText
                                    )
                                    
                                    if (result.isSuccess) {
                                        snackbarHostState.showSnackbar("消息发送成功")
                                        
                                        // 将发送的消息添加到本地消息列表显示
                                        LiveMessageManager.insertMessage(
                                            type = MessageType.AI_RESPONSE,
                                            content = messageText,
                                            userName = if (sendMode == SendMode.TEXT) "直接播音" else "AI转播"
                                        )

                                        // 发送成功，清空输入框
                                        messageText = ""
                                    } else {
                                        // 发送失败，显示错误信息
                                        snackbarHostState.showSnackbar("发送失败: ${result.msg}")
                                    }
                                } catch (e: Exception) {
                                    // 网络或其他异常
                                    snackbarHostState.showSnackbar("发送失败: ${e.message}")
                                } finally {
                                    // 重置发送状态
                                    isSending = false
                                }
                            }
                        }
                    )
                }
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
        // 判断是否显示Loading状态：当开关开启但连接状态为CONNECTING时
        val showLoading = connectionState == ConnectionState.CONNECTING
        
        if (showLoading) {
            // 显示Loading动画
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 根据chattingState显示不同的标题
        val displayTitle = if (chattingState != null && chattingState == LiveChattingService.CHATTING_STATE_PLAYING) {
            title // "$title (播放中)"
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
        if (false)
        if (connectionState != null) {
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
                .height(600.dp)
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
                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
                            }
                        }
                    },
                    update = { webView ->
                        // 确保WebView的可见性与父组件同步
                        webView.visibility = if (isVisible) android.view.View.VISIBLE else android.view.View.GONE
                        val layoutParams = ViewGroup.LayoutParams(1920, 720)
                        webView.layoutParams = layoutParams
                        // 当URL变化时重新加载
                        if (isVisible)
                            webView.loadUrl(url)
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
) {
    val configuration = LocalConfiguration.current

    // 计算屏幕高度
    val screenHeightDp = configuration.screenHeightDp.dp

    // 根据实际UI元素高度计算：
    // TopAppBar: 64dp (标准Material Design高度)
    // SwitchControlSection: 
    //   - Card的padding: 16.dp (上下各16dp)
    //   - Row的padding: 16.dp (上下各8dp)
    //   - SwitchItem高度: 约48dp (Switch: 48dp + 文字高度 + 间距)
    //   - 总计约: 16*2 + 8*2 + 48 = 96dp
    // MessageSendBar: 
    //   - Surface的padding: 0.dp
    //   - Row的padding: 8.dp (上下各4dp)
    //   - IconButton高度: 48.dp
    //   - 总计约: 0 + 4*2 + 48 = 56dp
    val topAppBarHeight = 64.dp
    val switchControlSectionHeight = 96.dp
    val messageSendBarHeight = 56.dp
    val listHeight = screenHeightDp - topAppBarHeight - switchControlSectionHeight - messageSendBarHeight

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

@Composable
private fun MessageSendBar(
    sendMode: SendMode,
    onSendModeChange: (SendMode) -> Unit,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    isSendEnabled: Boolean,
    isSending: Boolean,
    onSendClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp) // 移除原有的padding
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) // 可选：添加细边框以增强视觉效果
            .shadow(4.dp), // 保持阴影效果
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp), // 保持内部padding以确保内容不贴边
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧图标按钮 - 切换发送模式
            IconButton(
                onClick = {
                    onSendModeChange(if (sendMode == SendMode.TEXT) SendMode.CHAT else SendMode.TEXT)
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cached,
                    contentDescription = "切换发送模式",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 中间文本输入框
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        text = "请输入要发送的消息...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 5,
                minLines = 1,
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (isSendEnabled && !isSending) {
                            onSendClick()
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp)
            )

            // 右侧发送按钮
            IconButton(
                onClick = onSendClick,
                enabled = isSendEnabled && !isSending,
                modifier = Modifier.size(48.dp)
            ) {
                if (isSending) {
                    // 发送中显示加载指示器
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    // 根据模式显示不同图标和文本
                    when (sendMode) {
                        SendMode.TEXT -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "发送原文",
                                    tint = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "发送",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        SendMode.CHAT -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = "AI发送",
                                    tint = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "AI发送",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
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

