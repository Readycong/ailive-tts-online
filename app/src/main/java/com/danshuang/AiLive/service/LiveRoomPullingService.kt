package com.danshuang.AiLive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.view.View
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.app.NotificationCompat
import com.danshuang.AiLive.MainActivity
import com.danshuang.AiLive.R
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.model.*
import com.danshuang.AiLive.data.repository.LiveRoomRepository
import com.danshuang.AiLive.api.service.LiveRoomApiService
import com.danshuang.AiLive.websocket.WebSocketManager
import com.danshuang.AiLive.websocket.MessageProcessor
import com.danshuang.AiLive.ui.screens.LiveMessageManager
import com.danshuang.AiLive.ui.screens.MessageType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.core.view.isEmpty
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.util.DouyinHackUtil
import com.danshuang.AiLive.util.Utils
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.first
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.*
import java.util.concurrent.TimeUnit

var gLiveRoomLoaded: Boolean = false
var roomId = ""
var pushId = ""
var liveName = ""
var lastAudiencePostTime: Long = 0
var lastWSPostTime: Long = System.currentTimeMillis()/1000
/**
 * 直播间监控服务
 * 负责在后台运行WebSocket连接，监控直播间状态变化
 */
class LiveRoomPullingService : Service() {
    
    companion object {
        private const val TAG = "LiveRoomPullingService"
        
        // 监控状态常量
        const val PULLING_STATE_STOPPED = 0
        const val PULLING_STATE_MONITORING = 1
        const val PULLING_STATE_RECONNECT = 2
        
        // Intent extra keys
        const val EXTRA_WEBSOCKET_URL = "websocket_url"
        const val EXTRA_COOKIE_STRING = "cookie_string"
        const val EXTRA_SECONDARY_WEBSOCKET_URL = "secondary_websocket_url"
        const val EXTRA_ACTION = "action"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_UPDATE_CONNECTION = "update_connection"
        
        // 通知相关常量
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "live_room_monitoring"
        private const val CHANNEL_NAME = "直播间监控"
        private const val CHANNEL_DESCRIPTION = "直播间实时监控服务"
        
        // 单实例引用
        @Volatile
        private var instance: LiveRoomPullingService? = null

        // 服务状态流
        private val _pullingStateFlow = MutableStateFlow(PULLING_STATE_STOPPED)
        val pullingStateFlow: StateFlow<Int> = _pullingStateFlow.asStateFlow()
        
        // 连接状态流
        private val _connectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
        val connectionStateFlow: StateFlow<ConnectionState> = _connectionStateFlow.asStateFlow()
        
        // 第二个WebSocket连接状态流
        private val _secondaryConnectionStateFlow = MutableStateFlow(ConnectionState.DISCONNECTED)
        val secondaryConnectionStateFlow: StateFlow<ConnectionState> = _secondaryConnectionStateFlow.asStateFlow()
        
        // 错误信息流
        private val _errorMessageFlow = MutableStateFlow<String?>(null)
        val errorMessageFlow: StateFlow<String?> = _errorMessageFlow.asStateFlow()

        /**
         * 启动服务
         */
        fun startService(context: Context, wssUrl: String? = null, cookieString: String? = null, secondaryWssUrl: String? = null) {
            val intent = Intent(context, LiveRoomPullingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACTION, ACTION_START)
                wssUrl?.let { putExtra(EXTRA_WEBSOCKET_URL, it) }
                cookieString?.let { putExtra(EXTRA_COOKIE_STRING, it) }
                secondaryWssUrl?.let { putExtra(EXTRA_SECONDARY_WEBSOCKET_URL, it) }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 更新WebSocket连接
         */
        fun updateConnection(context: Context, wssUrl: String, cookieString: String, secondaryWssUrl: String? = null) {
            val intent = Intent(context, LiveRoomPullingService::class.java).apply {
                action = ACTION_UPDATE_CONNECTION
                putExtra(EXTRA_ACTION, ACTION_UPDATE_CONNECTION)
                putExtra(EXTRA_WEBSOCKET_URL, wssUrl)
                putExtra(EXTRA_COOKIE_STRING, cookieString)
                secondaryWssUrl?.let { putExtra(EXTRA_SECONDARY_WEBSOCKET_URL, it) }
            }
            context.startService(intent)
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, LiveRoomPullingService::class.java).apply {
                action = ACTION_STOP
                putExtra(EXTRA_ACTION, ACTION_STOP)
            }
            context.startService(intent)
        }
        
        /**
         * 获取当前监控状态
         */
        fun getCurrentPullingState(): Int = instance?.serviceState ?: PULLING_STATE_STOPPED
        
        /**
         * 获取当前连接状态
         */
        fun getCurrentConnectionState(): ConnectionState = instance?.connectionState ?: ConnectionState.DISCONNECTED
        
        /**
         * 获取第二个WebSocket连接状态
         */
        fun getSecondaryConnectionState(): ConnectionState = instance?.secondaryConnectionState ?: ConnectionState.DISCONNECTED
        
        /**
         * 获取服务实例
         */
        internal fun getInstance(): LiveRoomPullingService? = instance
    }
    
    // 核心组件
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var secondaryWebSocketManager: WebSocketManager
    private lateinit var messageProcessor: MessageProcessor
    private lateinit var liveRoomRepository: LiveRoomRepository
    private lateinit var apiService: LiveRoomApiService
    private lateinit var loginRepository: LoginRepository
    
    // 添加WebView实例
    private var webView: WebView? = null
    
    // 添加协程任务变量
    private var audienceCall: Job? = null
    private var checkLiveRoomJob: Job? = null
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 服务状态 - 使用单一状态变量
    private var serviceState = PULLING_STATE_STOPPED
        set(value) {
            field = value
            _pullingStateFlow.value = value
            updateNotification()
        }
    
    private var connectionState = ConnectionState.DISCONNECTED
        set(value) {
            field = value
            _connectionStateFlow.value = value
        }
    
    private var secondaryConnectionState = ConnectionState.DISCONNECTED
        set(value) {
            field = value
            _secondaryConnectionStateFlow.value = value
        }
    
    // 控制标志
    private val isServiceRunning = AtomicBoolean(false)
    private var isServiceStopped = AtomicBoolean(false) // 防止重复停止服务
    private var heartbeatJob: Job? = null
    private var secondaryHeartbeatJob: Job? = null
    private var configCheckJob: Job? = null
    
    // 配置缓存
    private var currentWebSocketUrl: String? = null
    private var currentSecondaryWebSocketUrl: String? = "wss://www.huichuang100.com/wss"
    private var currentCookieString: String? = null
    private var lastConfigCheckTime = 0L
    private val configCheckInterval = 5 * 60 * 1000L // 5分钟检查一次配置
    
    // 消息转发标志
    private var isMessageForwardingEnabled = true
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LiveRoomPullingService created")
        
        // 设置单实例引用
        instance = this
        
        initializeComponents()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "LiveRoomPullingService started with action: ${intent?.getStringExtra(EXTRA_ACTION)}, isServiceRunning: ${isServiceRunning.get()}")
        
        // 重置服务停止标志
        isServiceStopped.set(false)
        
        when (intent?.getStringExtra(EXTRA_ACTION)) {
            ACTION_START -> {
                // 获取传入的WebSocket URL和Cookie参数
                val wssUrl = intent.getStringExtra(EXTRA_WEBSOCKET_URL)
                val cookieString = intent.getStringExtra(EXTRA_COOKIE_STRING)
                val secondaryWssUrl = intent.getStringExtra(EXTRA_SECONDARY_WEBSOCKET_URL) ?: currentSecondaryWebSocketUrl

                if (!isServiceRunning.get()) {
                    isServiceRunning.set(true)
                    startForegroundService()
                    // 加载绑定账户的主页URL
                    loadBoundAccountHomeUrl()
                    //startPeriodicTasks()
                } else if (wssUrl != null && cookieString != null) {
                    // 如果服务已经在运行，但传入了新的URL和Cookie，则重新连接
                    connectWebSocket(wssUrl, cookieString)
                    if (connectionState == ConnectionState.CONNECTED) {
                        secondaryWssUrl?.let { connectSecondaryWebSocket(it) }
                    }
                }
            }
            ACTION_UPDATE_CONNECTION -> {
                // 更新WebSocket连接
                val wssUrl = intent.getStringExtra(EXTRA_WEBSOCKET_URL)
                val cookieString = intent.getStringExtra(EXTRA_COOKIE_STRING)
                val secondaryWssUrl = intent.getStringExtra(EXTRA_SECONDARY_WEBSOCKET_URL) ?: currentSecondaryWebSocketUrl
                
                if (wssUrl != null && cookieString != null) {
                    connectWebSocket(wssUrl, cookieString)
                    if (connectionState == ConnectionState.CONNECTED) {
                        secondaryWssUrl?.let { connectSecondaryWebSocket(it) }
                    }
                }
            }
            ACTION_STOP -> {
                stopServiceInternal()
            }
            else -> {
                // 默认行为
                if (!isServiceRunning.get()) {
                    isServiceRunning.set(true)
                    startForegroundService()
//                    initializeWebSocketConnection()
                    // 加载绑定账户的主页URL
                    loadBoundAccountHomeUrl()
                    //startPeriodicTasks()
                }
            }
        }
        
        return START_STICKY // 服务被杀死后自动重启
    }
    
    override fun onDestroy() {
        Log.i(TAG, "LiveRoomPullingService destroyed")
        stopServiceInternal()
        super.onDestroy()
    }
    
    /**
     * 统一服务停止方法
     */
    private fun stopServiceInternal() {
        // 防止重复停止服务
        if (isServiceStopped.getAndSet(true)) {
            return
        }
        
        isServiceRunning.set(false)
        serviceState = PULLING_STATE_STOPPED
        connectionState = ConnectionState.DISCONNECTED
        gLiveRoomLoaded = false
        webView?.tag = false
        
        Log.i(TAG, "LiveRoomPullingService stopped")
        
        // 清除单实例引用
        instance = null
        
        stopMonitoring()
        cleanupResources()

        try {
            audienceCall?.cancel()
            checkLiveRoomJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling audienceCall or checkLiveRoomJob", e)
        }

        // 安全地销毁WebView
//        webView?.let { view ->
//            try {
//                view.destroy()
//            } catch (e: Exception) {
//                Log.w(TAG, "Error destroying WebView", e)
//            }
//        }
//        webView = null
        
        // 移除通知
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        
        stopSelf()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // 不支持绑定
    }
    
    /**
     * 初始化核心组件
     */
    private fun initializeComponents() {
        try {
            apiService = LiveRoomApiService()
            liveRoomRepository = LiveRoomRepository(this, apiService)
            loginRepository = LoginRepository(this)
            webSocketManager = WebSocketManager()
            secondaryWebSocketManager = WebSocketManager()
            messageProcessor = MessageProcessor(liveRoomRepository, serviceScope)
            
            // 设置主WebSocket二进制消息监听器
            webSocketManager.setBinaryMessageListener { bytes ->
                serviceScope.launch {
                    try {
                        // 转发二进制消息到第二个WebSocket
                        if (isMessageForwardingEnabled && secondaryWebSocketManager.isConnected()) {
                            secondaryWebSocketManager.sendBinaryMessage(bytes)
//                            Log.d(TAG, "Forwarded binary message to platform websocket")
                            lastWSPostTime = System.currentTimeMillis()/1000
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to forward binary message to platform websocket", e)
                        addLogMessage("直播消息处理失败->P", true)
                        stopServiceInternal()
                    }
                }
            }
            
            // 设置第二个WebSocket消息监听器
            secondaryWebSocketManager.setTextMessageListener { message ->
                serviceScope.launch {
                    try {
                        // 解析JSON消息格式 {"id":"111","nickname":"nickname","message":"text"}
                        var userId: String = ""
                        var userNickname: String = ""
                        var userContent: String = ""
                        
                        try {
                            val jsonObject = org.json.JSONObject(message)
                            userId = jsonObject.optString("id")
                            userNickname = jsonObject.optString("nickname")
                            userContent = jsonObject.optString("message")
                        } catch (jsonException: org.json.JSONException) {
                            Log.w(TAG, "Failed to parse JSON message, using raw message", jsonException)
                            userContent = message
                            userNickname = "推送"
                        }
                        
                        // 将文本消息显示到AI直播屏幕的消息列表
                        try {
                            LiveMessageManager.insertMessage(
                                type = MessageType.COMMENT,
                                content = userContent,
                                userName = userNickname
                            )
                            Log.d(TAG, "Inserted platform websocket message to LiveMessageManager: $message")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to insert platform websocket message to LiveMessageManager", e)
                            addLogMessage("直播消息处理失败P->A", true)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process platform websocket message", e)
                        addLogMessage("直播消息处理失败P->A_W", true)
                        stopServiceInternal()
                    }
                }
            }
            
            // 设置第二个WebSocket二进制消息监听器
            secondaryWebSocketManager.setBinaryMessageListener { bytes ->
                serviceScope.launch {
                    try {
                        // 转发二进制消息到主WebSocket
                        if (isMessageForwardingEnabled && webSocketManager.isConnected()) {
                            webSocketManager.sendBinaryMessage(bytes)
//                            Log.d(TAG, "Forwarded binary message to primary WebSocket")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to forward binary message to primary WebSocket", e)
                        addLogMessage("直播消息处理失败P->D", true)
                        stopServiceInternal()
                    }
                }
            }

            Log.i(TAG, "Components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
            _errorMessageFlow.value = "初始化失败: ${e.message}"
            addLogMessage("初始化失败直播消息处理器", true)
            stopServiceInternal()
        }
    }
    
    /**
     * 初始化WebSocket连接
     */
    private fun initializeWebSocketConnection(wssUrl: String? = null, cookieString: String? = null, secondaryWssUrl: String? = null) {
        serviceScope.launch {
            try {
                Log.i(TAG, "Initializing WebSocket connections...")
                
                if (wssUrl != null && cookieString != null) {
                    // 使用传入的URL和Cookie连接主WebSocket
                    connectWebSocket(wssUrl, cookieString)
                    addLogMessage("直播间连接成功", true)
                } else {
                    addLogMessage("直播连接失败D", true)
                    Log.d(TAG, "Failed to initialize pulling WebSocket connection: wssUrl=%s, cookieString=%s".format(wssUrl, cookieString))
                    // 不再直接调用stopSelf()，而是通过状态管理
                }
                
                // 连接第二个WebSocket
                val secondaryUrl = secondaryWssUrl ?: currentSecondaryWebSocketUrl
                if (secondaryUrl != null) {
                    connectSecondaryWebSocket(secondaryUrl)
                }
                
                // 保存配置
                saveConfiguration(wssUrl, cookieString, secondaryUrl)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WebSocket connections", e)
                handleConnectionError(e)
                addLogMessage("直播间连接失败P", true)
                // 不再直接调用stopSelf()，而是通过状态管理
            }
        }
    }
    
    /**
     * 连接主WebSocket
     */
    private fun connectWebSocket(url: String, cookieString: String? = null) {
        // 添加URL有效性检查
        if (url.isBlank()) {
            Log.e(TAG, "WebSocket URL is empty or blank")
            addLogMessage("直播间连接失败：URL为空", true)
            serviceState = PULLING_STATE_STOPPED
            connectionState = ConnectionState.ERROR
            _errorMessageFlow.value = "连接URL为空"
            return
        }
        
        // 检查URL格式是否正确
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ws://") && !url.startsWith("wss://")) {
            Log.e(TAG, "Invalid WebSocket URL scheme: $url")
            addLogMessage("直播间连接失败：URL格式不正确", true)
            serviceState = PULLING_STATE_STOPPED
            connectionState = ConnectionState.ERROR
            _errorMessageFlow.value = "连接URL格式不正确"
            return
        }
        
        if (currentWebSocketUrl == url && webSocketManager.isConnected()) {
            Log.d(TAG, "Already connected to the same pulling URL: $url")
            return
        }
        
        currentWebSocketUrl = url
        currentCookieString = cookieString
        Log.i(TAG, "Connecting to primary WebSocket: $url")
        
        // 先断开现有连接
        webSocketManager.disconnect()
        
        // 保存服务状态
        serviceScope.launch {
            saveServiceState()
        }
        
        // 创建带Cookie的请求
        val requestBuilder = Request.Builder()
            .url(url)

        // 如果提供了Cookie，则添加到请求头中
        cookieString?.let {
            if (it.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", it)
                Log.d(TAG, "Added cookie to primary WebSocket request")
            }
        }
        
        val request = requestBuilder.build()
        
        // 使用自定义请求连接WebSocket
        webSocketManager.connectWithRequest(request) { state ->
            connectionState = state
            when (state) {
                ConnectionState.CONNECTING -> {
                    Log.d(TAG, "Pulling WebSocket connecting...")
                    _errorMessageFlow.value = null
                }
                ConnectionState.CONNECTED -> {
                    Log.i(TAG, "Pulling WebSocket connected successfully")
                    serviceState = PULLING_STATE_MONITORING
                    _errorMessageFlow.value = null
                }
                ConnectionState.DISCONNECTED -> {
                    Log.w(TAG, "Pulling WebSocket disconnected")
                    serviceState = PULLING_STATE_STOPPED
                    // 不再直接调用stopSelf()，而是通过状态管理
                    isServiceRunning.set(false)
                    isServiceStopped.set(true)
                    serviceScope.launch(Dispatchers.Main) {
                        webView?.loadUrl("about:blank")
                    }
                }
                ConnectionState.ERROR -> {
                    Log.e(TAG, "Pulling WebSocket connection error")
                    serviceState = PULLING_STATE_STOPPED
                    _errorMessageFlow.value = "主连接失败，正在重试..."
                    // 不再直接调用stopSelf()，而是通过状态管理
                }
            }
            
            serviceScope.launch {
                saveServiceState()
            }
        }
    }
    
    /**
     * 连接第二个WebSocket
     */
    private fun connectSecondaryWebSocket(url: String) {
        // 添加URL有效性检查
        if (url.isBlank()) {
            Log.e(TAG, "Secondary WebSocket URL is empty or blank")
            addLogMessage("副连接失败：URL为空", true)
            secondaryConnectionState = ConnectionState.ERROR
            return
        }
        
        // 检查URL格式是否正确
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("ws://") && !url.startsWith("wss://")) {
            Log.e(TAG, "Invalid secondary WebSocket URL scheme: $url")
            addLogMessage("副连接失败：URL格式不正确", true)
            secondaryConnectionState = ConnectionState.ERROR
            return
        }
        
        if (currentSecondaryWebSocketUrl == url && secondaryWebSocketManager.isConnected()) {
            Log.d(TAG, "Already connected to the same secondary URL: $url")
            return
        }
        
        currentSecondaryWebSocketUrl = url
        Log.i(TAG, "Connecting to platform websocket: $url")
        
        // 先断开现有连接
        secondaryWebSocketManager.disconnect()
        
        // 创建请求
        val requestBuilder = Request.Builder()
            .url(url)
        
        val request = requestBuilder.build()
        
        // 使用自定义请求连接WebSocket
        secondaryWebSocketManager.connectWithRequest(request) { state ->
            secondaryConnectionState = state
            when (state) {
                ConnectionState.CONNECTING -> {
                    Log.d(TAG, "Secondary WebSocket connecting...")
                }
                ConnectionState.CONNECTED -> {
                    Log.i(TAG, "Secondary WebSocket connected successfully")
                }
                ConnectionState.DISCONNECTED -> {
                    Log.w(TAG, "Secondary WebSocket disconnected")
                }
                ConnectionState.ERROR -> {
                    Log.e(TAG, "Secondary WebSocket connection error")
                }
            }
            
            serviceScope.launch {
                saveServiceState()
            }
        }
    }
    
    /**
     * 启动周期性任务
     */
    private fun startPeriodicTasks() {
        // 启动配置检查任务
        configCheckJob = serviceScope.launch {
            while (isActive) {
                try {
                    // 检查配置更新
                    serviceScope.launch {
                        checkConfigurationUpdate()
                    }
                    delay(configCheckInterval)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to check configuration", e)
                    delay(60000) // 出错时1分钟后重试
                }
            }
        }
    }
    
    /**
     * 检查配置更新
     */
    private suspend fun checkConfigurationUpdate() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastConfigCheckTime < configCheckInterval) {
            return
        }
        
        lastConfigCheckTime = currentTime
        
        try {
            Log.d(TAG, "Checking for configuration updates...")
            
            val config = liveRoomRepository.fetchWebSocketConfig()
            
            if (config.websocketUrl != currentWebSocketUrl) {
                Log.i(TAG, "WebSocket URL changed: ${currentWebSocketUrl} -> ${config.websocketUrl}")
                connectWebSocket(config.websocketUrl, currentCookieString ?: "")
            }

        } catch (e: Exception) {
            Log.w(TAG, "Failed to check configuration update", e)
        }
    }
    
    /**
     * 处理连接错误
     */
    private fun handleConnectionError(error: Throwable) {
        Log.e(TAG, "Connection error occurred", error)
        
        serviceState = PULLING_STATE_STOPPED
        _errorMessageFlow.value = "连接错误: ${error.message}"
        
        serviceScope.launch {
            saveServiceState()
        }
        
        // 不再直接调用stopSelf()，而是通过状态管理
    }
    
    /**
     * 保存配置到本地存储
     */
    private suspend fun saveConfiguration(primaryUrl: String?, cookieString: String?, secondaryUrl: String?) {
        try {
            // 保存到本地存储
            val config = WebSocketConfig(
                websocketUrl = primaryUrl ?: "",
                lastUpdateTime = System.currentTimeMillis(),
                isActive = true
            )
            
            liveRoomRepository.saveWebSocketConfig(config)
            
            // 更新当前配置
            currentWebSocketUrl = primaryUrl
            currentCookieString = cookieString
            currentSecondaryWebSocketUrl = secondaryUrl
            
            Log.i(TAG, "Configuration saved successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save configuration", e)
        }
    }
    
    /**
     * 保存服务状态
     */
    private suspend fun saveServiceState() {
        try {
            val state = ServiceState(
                pullingState = serviceState,
                lastWebSocketUrl = currentWebSocketUrl,
                lastConnectTime = if (connectionState == ConnectionState.CONNECTED) System.currentTimeMillis() else null,
                lastDisconnectTime = if (connectionState == ConnectionState.DISCONNECTED) System.currentTimeMillis() else null,
                errorMessage = _errorMessageFlow.value
            )
            
            liveRoomRepository.saveServiceState(state)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save service state", e)
        }
    }
    
    /**
     * 加载绑定账户的主页URL（PC模式）
     */
    private fun loadBoundAccountHomeUrl() {
        serviceScope.launch {
            try {
                connectionState = ConnectionState.CONNECTING
                // 获取用户配置信息
                val profile = loginRepository.userProfile
                val boundAccount = profile.first().boundAccount
                withContext(Dispatchers.Main) {
                    // 检查是否有绑定账户且有主页URL
                    if (boundAccount != null && boundAccount.homeUrl.isNotEmpty()) {
                        Log.i(TAG, "Loading bound account home URL: ${boundAccount.homeUrl}")
                        
                        // 安全地销毁现有的WebView（如果存在）
//                        webView?.let { view ->
//                            try {
//                                view.destroy()
//                            } catch (e: Exception) {
//                                Log.w(TAG, "Error destroying existing WebView", e)
//                            }
//                        }

                        // 创建新的WebView实例
                        if (webView == null)
                            webView = WebView(this@LiveRoomPullingService)

                        gLiveRoomLoaded = false
                        webView?.tag = false
                        
                        // 在主线程中创建和配置WebView
                        withContext(Dispatchers.Main) {
                            // 设置WebView尺寸为1920px * 720px
                            val layoutParams = ViewGroup.LayoutParams(1920, 720)
                            webView?.layoutParams = layoutParams

                            // 配置WebView设置
                            val settings = webView?.settings
                            settings?.javaScriptEnabled = true
                            settings?.domStorageEnabled = true
                            settings?.databaseEnabled = true
                            settings?.useWideViewPort = true
                            settings?.loadWithOverviewMode = true
                            settings?.setSupportZoom(true)
                            settings?.setGeolocationEnabled(true)
                            settings?.builtInZoomControls = false
                            settings?.displayZoomControls = false
                            settings?.allowContentAccess = true
                            settings?.allowFileAccess = true

                            // 设置PC模式User-Agent
                            val pcUserAgent =
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
                            settings?.userAgentString = pcUserAgent

                            webView?.addJavascriptInterface(
                                WebAppInterface(this@LiveRoomPullingService),
                                "Android"
                            )
                            // 设置WebViewClient以处理页面加载
                            webView?.webViewClient = object : WebViewClient() {
                                override fun onReceivedHttpError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    errorResponse: WebResourceResponse?
                                ) {
                                    super.onReceivedHttpError(view, request, errorResponse)
                                    Log.d("Webview", errorResponse.toString())
                                }
                                
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("WebView", "Page finished loading: $url")
                                    
                                    // 添加空值检查，避免空指针异常
                                    if (view == null || gLiveRoomLoaded) return

                                    // 防止无限循环调用
                                    if (view.tag == true) {
                                        view.tag = false
                                        return
                                    }
                                    
                                    // 添加安全检查
                                    if (url?.startsWith("https://www.douyin.com/user/", true) == true) {
                                        addLogMessage("正在尝试连接直播间...", false)
                                        // 确保WebView操作在主线程中执行
                                        serviceScope.launch(Dispatchers.Main) {
                                            view.evaluateJavascript(Utils.script) { html ->
                                                addLogMessage("检测开播信息JSON：$html")
                                                // 添加空值检查，避免空指针异常
                                                if (!html.isNullOrEmpty()) {
                                                    if (html == "null") {
                                                        addLogMessage("用户主页处理失败", true)
                                                        stopServiceInternal()
                                                    } else if (html.contains(
                                                            "user_detail_element",
                                                            true
                                                        )
                                                    ) {
                                                        addLogMessage("检测到主页信息，开始解析直播间信息...")
                                                        var gson = Gson()
                                                        gson.fromJson(html, Map::class.java)
                                                            ?.let { jsonMap ->
                                                                val liveUrl =
                                                                    jsonMap["url"]?.toString()
                                                                // 修复空指针异常：检查liveUrl是否为null，避免使用!!操作符
                                                                if (jsonMap["status"]?.toString() != "PLAYING" ||
                                                                    (liveUrl == null || liveUrl.indexOf(
                                                                        "https://live.douyin.com/"
                                                                    ) < 0)
                                                                ) {
                                                                    addLogMessage(
                                                                        "直播间未开播",
                                                                        true
                                                                    )
                                                                    stopServiceInternal()
                                                                } else {
                                                                    // 检查URL是否与当前URL相同，避免重复加载
                                                                    if (view.url != liveUrl) {
                                                                        // 设置标记以防止无限循环
                                                                        view.tag = true
                                                                        view.loadUrl(liveUrl)
                                                                    }
                                                                }
                                                            }
                                                    }
                                                }
                                            }
                                        }
                                    } else if (url?.startsWith("https://live.douyin.com/") == true) {
                                        try {
                                            view.tag = true
                                            JoinLiveRoom(view)
                                        } catch (e: Exception) {
                                            addLogMessage("直播间连接异常")
                                            connectionState = ConnectionState.ERROR
                                        }
                                    }
                                }
                            }

                            // 在后台加载URL
                            webView?.loadUrl(boundAccount.homeUrl)
                        }
                    } else {
                        Log.d(TAG, "No bound account or home URL is empty")
                        connectionState = ConnectionState.ERROR
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load bound account home URL", e)
                connectionState = ConnectionState.ERROR
            }
        }
    }
    
    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        isServiceRunning.set(false)
        serviceState = PULLING_STATE_STOPPED

        webSocketManager.disconnect()
        secondaryWebSocketManager.disconnect()
        connectionState = ConnectionState.DISCONNECTED

        serviceScope.launch {
            saveServiceState()
        }
        
        try {
            audienceCall?.cancel()
            checkLiveRoomJob?.cancel()
        } catch (e: Exception) {
            Log.w(TAG, "Error cancelling audienceCall or checkLiveRoomJob", e)
        }

        Log.i(TAG, "Monitoring stopped")
    }
    
    /**
     * 清理资源
     */
    private fun cleanupResources() {
        configCheckJob?.cancel()
        audienceCall?.cancel()
        checkLiveRoomJob?.cancel()
        serviceScope.cancel()
        messageProcessor.cleanup()
        
        Log.i(TAG, "Resources cleaned up")
    }
    
    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val statusText = when (serviceState) {
            PULLING_STATE_MONITORING -> "正在监控直播间动态..."
            else -> "直播间监控已停止"
        }
        
        val connectionText = when (connectionState) {
            ConnectionState.CONNECTED -> "主连接已连接"
            ConnectionState.CONNECTING -> "主连接连接中"
            ConnectionState.DISCONNECTED -> "主连接已断开"
            ConnectionState.ERROR -> "主连接错误"
        }
        
        val secondaryConnectionText = when (secondaryConnectionState) {
            ConnectionState.CONNECTED -> "副连接已连接"
            ConnectionState.CONNECTING -> "副连接连接中"
            ConnectionState.DISCONNECTED -> "副连接已断开"
            ConnectionState.ERROR -> "副连接错误"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI直播监控服务")
            .setContentText("$statusText ($connectionText, $secondaryConnectionText)")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        if (isServiceRunning.get()) {
            val notification = createNotification()
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun JoinLiveRoom(liveView: WebView) {
        // 不要重新设置webViewClient，这可能会导致循环或其他问题
        // 直接执行需要的操作，而不是重新设置webViewClient

        val url = liveView.url
        Log.d("JoinLiveRoom", "URL: $url")

        // 你可以在这里处理cookie，比如显示到TextView
        // 确保WebView操作在主线程中执行
        serviceScope.launch(Dispatchers.Main) {
            liveView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })();"
            ) { html ->
                val cleanHtml = html?.removeSurrounding("\"")?.replace("\\u003C", "<")
                    ?.replace("""\\\"""", "\"")
                Log.d("JoinLiveRoom", cleanHtml ?: "null")

                val docHtml = cleanHtml ?: "null"
                var utilObj = DouyinHackUtil(docHtml)

                try {
                    // 添加空值检查，避免空指针异常
                    val roomIdValue = utilObj.getRoomId()?.toString() ?: ""
                    val pushIdValue = utilObj.getPushID()?.toString() ?: ""
                    val liveNameValue = utilObj.getLiveName()?.toString() ?: ""

                    // 添加空值检查，避免空指针异常
                    if (roomIdValue.isNotEmpty() && roomIdValue != "null") {
                        // 更新全局变量
                        roomId = roomIdValue
                        pushId = pushIdValue
                        liveName = liveNameValue
                        
                        // 保存roomId到DataStore
                        serviceScope.launch {
                            try {
                                loginRepository.saveLiveRoomId(roomId)
                                Log.d(TAG, "Saved roomId to DataStore: $roomId")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to save roomId to DataStore", e)
                            }
                        }

                        addLogMessage("Room ID: $roomId")
                        addLogMessage("Push ID: $pushId")
                        addLogMessage("Live Name: $liveName")


                        addLogMessage("已绑定抖音账号与直播间: $liveName $roomId")

                        // 添加空值检查，避免空指针异常
                        val userAgent = liveView.settings?.userAgentString
                        var wsUri = ""
                        if (userAgent != null) {
                            wsUri = utilObj.getWssURL(userAgent, roomId, pushId)
                            Log.d("DouyinHackUtil", "WebSocket URI: $wsUri")

                            val cookie = android.webkit.CookieManager.getInstance().getCookie(url)
                            // 连接WebSocket wsUri，并传入cookie
                            // 启动LiveRoomPullingService服务，传入websocket地址及cookie参数
                            val context = liveView.context
                            // 使用更新连接的方式，确保单实例模式下能更新连接参数

                            // 调用bindDouyinLive方法绑定直播间信息到服务器
                            serviceScope.launch {
                                try {
                                    val tokenManager = TokenManager(loginRepository)
                                    val serverApiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
                                    
                                    // 获取当前用户信息
                                    val currentUser = serverApiUtils.getCurrentUser()
                                    val boundAccount = currentUser?.userProfile?.boundAccount
                                    
                                    if (boundAccount != null && boundAccount.userId.isNotEmpty()) {
                                        // 调用bindDouyinLive方法绑定直播间信息
                                        val bindResult = serverApiUtils.bindDouyinLive(
                                            token = currentUser.token,
                                            douyinId = boundAccount.userId,
                                            nickname = boundAccount.nickname,
                                            avatar = boundAccount.avatar,
                                            roomId = roomId,
                                            userHomeUrl = boundAccount.homeUrl
                                        )
                                        
                                        if (bindResult.isSuccess) {
                                            addLogMessage("直播间信息绑定成功")
                                        } else {
                                            addLogMessage("直播间信息绑定失败: ${bindResult.msg}")
                                        }
                                    } else {
                                        addLogMessage("绑定账户信息不完整，无法绑定直播间信息")
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to bind douyin live room", e)
                                    addLogMessage("绑定直播间信息时发生错误: ${e.message}")
                                }
                            }

                            addLogMessage("已更新直播间监控服务连接")
                            gLiveRoomLoaded = true
                            isServiceRunning.set(true)
                            initializeWebSocketConnection(wsUri, cookie)
                            hookWebView(liveView)
//
//                        // 通过收到消息间隔判断直播间是否卡停
//                        checkLiveRoomJob = MainScope().launch {
//                            while (true) {
//                                try {
//                                    delay(10000) // 每10秒检查一次
//                                    val currentTime = System.currentTimeMillis()/1000
//                                    if (currentTime - lastWSPostTime > 20) {
//                                        addLogMessage("直播间消息长时间未更新，尝试重连直播间", true)
//                                        pullingState = PULLING_STATE_RECONNECT
//                                        Log.w(TAG, "No messages received from WebSocket for over 30 seconds")
//                                        break
//                                    }
//                                } catch (e: Exception) {
//                                    Log.e(TAG, "Error while checking live room status", e)
//                                    break
//                                }
//                            }
//                            stopSelf()
//                        }
                        }
                    }
                } catch (e: Exception) {
                    addLogMessage("直播间连接失败，请重新启动直播连接状态检测。")
                    return@evaluateJavascript
                }
            }
        }
    }

    private fun hookWebView(liveView: WebView) {
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

        // 确保WebView操作在主线程中执行
        serviceScope.launch(Dispatchers.Main) {
            liveView.evaluateJavascript(jsCode, null)
            Log.d("AudienceAjax", "WebView hook injected")
        }

        // 先取消现有的audienceCall（如果存在）
        audienceCall?.cancel()
        
        audienceCall = serviceScope.launch {
            while (isActive) {
                try {
                    // 每60秒执行一次
                    delay(60000)
                    // 确保WebView操作在主线程中执行
                    serviceScope.launch(Dispatchers.Main) {
                        liveView.evaluateJavascript("document.querySelector('div.pip-anchor').outerText.trim()") { html ->
                            if (html != null && html.indexOf("直播已结束") >= 0) {
                                addLogMessage("直播间已结束，停止直播间监控")
                                stopServiceInternal()
                                return@evaluateJavascript
                            }
                            liveView.evaluateJavascript("document.querySelector('.chatroom_close').click()") { html ->
                                liveView.evaluateJavascript("setTimeout(()=>{document.querySelector('div.chat_room_fold').click()},1000)") { html ->
//                                    liveView.evaluateJavascript("setTimeout(()=>{document.querySelector('#chatroom > div.c9Poqbe4.unset-border > div.LyAdeVIF.sBRqUw32 > div.vYlYuXg3 > div.vuY0PPXy.LR4EQtPh > p').click()}, 1500)") { html ->
//
//                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("AudienceAjax", "Failed to execute periodic task", e)
                    delay(10000) // 出错时10秒后重试
                }
            }
        }
    }
}

// 添加WebAppInterface类定义
class WebAppInterface(private val context: android.content.Context) {
    @JavascriptInterface
    fun onXhrResponse(method: String?, url: String?, status: Int, response: String?) {
//                Log.d("XHR", "Method: " + method)
//                Log.d("XHR", "URL: " + url)
//                Log.d("XHR", "Status: " + status)
//                Log.d("XHR", "Response: " + response)
        if (url?.indexOf("https://live.douyin.com/webcast/ranklist/audience") != -1) {
            if (url != null && response != null) {
                val currentTime = System.currentTimeMillis()/1000
                if ((currentTime - lastAudiencePostTime) < 50) { // 执行有时间误差，50秒内只允许执行一次，实际是60秒一次或更多
                    Log.d("AudienceAjax", "请求过于频繁，跳过本次请求")
                    return
                }
                lastAudiencePostTime = currentTime
                val audienceUrl = url
                Log.d("AudienceAjax", "audienceUrl: $audienceUrl")
                try {
                    val gson = Gson()
                    val audienceData = gson.fromJson(response, JsonObject::class.java)
                    Log.d("AudienceAjax", "Audience Data: $audienceData")
                    val data = audienceData.getAsJsonObject("data")
                    val ranks = data.getAsJsonArray("ranks")
                    val resultMap = mutableMapOf<String, String>()

                    for (rank in ranks) {
                        val user = rank.asJsonObject.getAsJsonObject("user")
                        val nickname = user.get("nickname").asString
                        val webcastUid = user.get("webcast_uid").asString
                        resultMap[webcastUid] = nickname
                    }
                    Log.d("AudienceAjax", "观众数据: $resultMap")
                    try {
                        // 获取当前用户信息和roomId
                        val loginRepository = LoginRepository(context)
                        val tokenManager = TokenManager(loginRepository)
                        val serverApiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)

                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val currentUser = serverApiUtils.getCurrentUser()
                                val douyinId = currentUser?.userProfile?.boundAccount?.userId ?: ""

                                // 使用局部变量确保获取最新的roomId值
                                val currentRoomId = roomId

                                if (douyinId.isNotEmpty() && currentRoomId.isNotEmpty()) {
                                    val result = serverApiUtils.onlineUsers(douyinId, currentRoomId, gson.toJson(resultMap))
                                    if (!result.isSuccess) {
                                        addLogMessage("上传观众数据失败: ${result.msg}")
                                    }
                                } else {
                                    addLogMessage("上传观众数据失败: 用户未绑定抖音账号或roomId为空")
                                }
                            } catch (e: Exception) {
                                addLogMessage("上传观众数据失败: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        addLogMessage("上传观众数据失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    addLogMessage("解析观众数据失败: ${e.message}")
                }
            }
        }
    }
}

fun addLogMessage(message: String, showList: Boolean = false) {
    Log.d("LiveRoomPullingService", message)
    if (showList) {
        LiveMessageManager.insertMessage(
            type = MessageType.SYSTEM,
            content = message,
            userName = "系统"
        )
    }
}