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
import androidx.core.app.NotificationCompat
import com.danshuang.AiLive.MainActivity
import com.danshuang.AiLive.R
import com.danshuang.AiLive.data.model.*
import com.danshuang.AiLive.data.repository.LiveRoomRepository
import com.danshuang.AiLive.api.service.LiveRoomApiService
import com.danshuang.AiLive.websocket.WebSocketManager
import com.danshuang.AiLive.websocket.MessageProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.*
import java.util.concurrent.TimeUnit

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
        
        // Intent extra keys
        const val EXTRA_WEBSOCKET_URL = "websocket_url"
        const val EXTRA_COOKIE_STRING = "cookie_string"
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
        
        // 错误信息流
        private val _errorMessageFlow = MutableStateFlow<String?>(null)
        val errorMessageFlow: StateFlow<String?> = _errorMessageFlow.asStateFlow()
        
        /**
         * 启动服务
         */
        fun startService(context: Context, wssUrl: String? = null, cookieString: String? = null) {
            val intent = Intent(context, LiveRoomPullingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_ACTION, ACTION_START)
                wssUrl?.let { putExtra(EXTRA_WEBSOCKET_URL, it) }
                cookieString?.let { putExtra(EXTRA_COOKIE_STRING, it) }
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
        fun updateConnection(context: Context, wssUrl: String, cookieString: String) {
            val intent = Intent(context, LiveRoomPullingService::class.java).apply {
                action = ACTION_UPDATE_CONNECTION
                putExtra(EXTRA_ACTION, ACTION_UPDATE_CONNECTION)
                putExtra(EXTRA_WEBSOCKET_URL, wssUrl)
                putExtra(EXTRA_COOKIE_STRING, cookieString)
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
        fun getCurrentPullingState(): Int = _pullingStateFlow.value
        
        /**
         * 获取当前连接状态
         */
        fun getCurrentConnectionState(): ConnectionState = _connectionStateFlow.value
        
        /**
         * 获取服务实例
         */
        internal fun getInstance(): LiveRoomPullingService? = instance
    }
    
    // 核心组件
    private lateinit var webSocketManager: WebSocketManager
    private lateinit var messageProcessor: MessageProcessor
    private lateinit var liveRoomRepository: LiveRoomRepository
    private lateinit var apiService: LiveRoomApiService
    
    // 协程作用域
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 服务状态
    private var pullingState = PULLING_STATE_STOPPED
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
    
    // 控制标志
    private val isServiceRunning = AtomicBoolean(false)
    private var heartbeatJob: Job? = null
    private var configCheckJob: Job? = null
    
    // 配置缓存
    private var currentWebSocketUrl: String? = null
    private var currentCookieString: String? = null
    private var lastConfigCheckTime = 0L
    private val configCheckInterval = 5 * 60 * 1000L // 5分钟检查一次配置
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LiveRoomPullingService created")
        
        // 设置单实例引用
        instance = this
        
        initializeComponents()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "LiveRoomPullingService started with action: ${intent?.getStringExtra(EXTRA_ACTION)}")
        
        when (intent?.getStringExtra(EXTRA_ACTION)) {
            ACTION_START -> {
                // 获取传入的WebSocket URL和Cookie参数
                val wssUrl = intent.getStringExtra(EXTRA_WEBSOCKET_URL)
                val cookieString = intent.getStringExtra(EXTRA_COOKIE_STRING)
                
                if (!isServiceRunning.get()) {
                    isServiceRunning.set(true)
                    startForegroundService()
                    initializeWebSocketConnection(wssUrl, cookieString)
                    startPeriodicTasks()
                } else if (wssUrl != null && cookieString != null) {
                    // 如果服务已经在运行，但传入了新的URL和Cookie，则重新连接
                    connectWebSocket(wssUrl, cookieString)
                }
            }
            ACTION_UPDATE_CONNECTION -> {
                // 更新WebSocket连接
                val wssUrl = intent.getStringExtra(EXTRA_WEBSOCKET_URL)
                val cookieString = intent.getStringExtra(EXTRA_COOKIE_STRING)
                
                if (wssUrl != null && cookieString != null) {
                    connectWebSocket(wssUrl, cookieString)
                }
            }
            ACTION_STOP -> {
                // 停止服务
                stopSelf()
            }
            else -> {
                // 默认行为
                if (!isServiceRunning.get()) {
                    isServiceRunning.set(true)
                    startForegroundService()
                    initializeWebSocketConnection()
                    startPeriodicTasks()
                }
            }
        }
        
        return START_STICKY // 服务被杀死后自动重启
    }
    
    override fun onDestroy() {
        Log.i(TAG, "LiveRoomPullingService destroyed")
        
        // 清除单实例引用
        instance = null
        
        stopMonitoring()
        cleanupResources()
        super.onDestroy()
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
            webSocketManager = WebSocketManager()
            messageProcessor = MessageProcessor(liveRoomRepository, serviceScope)
            
            // 设置WebSocket消息监听器
            webSocketManager.setMessageListener { message ->
                serviceScope.launch {
                    try {
                        val result = messageProcessor.processMessage(message)
                        when (result) {
                            is ProcessResult.Success -> Log.d(TAG, "Message processed: ${result.message}")
                            is ProcessResult.Error -> Log.e(TAG, "Message processing error: ${result.error}")
                            is ProcessResult.Ignored -> Log.d(TAG, "Message ignored: ${result.reason}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to process WebSocket message", e)
                    }
                }
            }
            
            Log.i(TAG, "Components initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize components", e)
            _errorMessageFlow.value = "初始化失败: ${e.message}"
        }
    }
    
    /**
     * 初始化WebSocket连接
     */
    private fun initializeWebSocketConnection(wssUrl: String? = null, cookieString: String? = null) {
        serviceScope.launch {
            try {
                Log.i(TAG, "Initializing WebSocket connection...")
                
                if (wssUrl != null && cookieString != null) {
                    // 使用传入的URL和Cookie连接
                    connectWebSocket(wssUrl, cookieString)
                } else {
                    Log.d(TAG, "Failed to initialize WebSocket connection: wssUrl=%s, cookieString=%s".format(wssUrl, cookieString))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize WebSocket connection", e)
                handleConnectionError(e)
            }
        }
    }
    
    /**
     * 连接WebSocket
     */
    private fun connectWebSocket(url: String, cookieString: String? = null) {
        if (currentWebSocketUrl == url && webSocketManager.isConnected()) {
            Log.d(TAG, "Already connected to the same URL: $url")
            return
        }
        
        currentWebSocketUrl = url
        currentCookieString = cookieString
        Log.i(TAG, "Connecting to WebSocket: $url")
        
        // 先断开现有连接
        webSocketManager.disconnect()
        
        // 保存服务状态
        serviceScope.launch {
            saveServiceState()
        }
        
        // 创建带Cookie的请求
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "AiLive/1.0")
            .addHeader("Origin", "https://live.douyin.com")
        
        // 如果提供了Cookie，则添加到请求头中
        cookieString?.let {
            if (it.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", it)
                Log.d(TAG, "Added cookie to WebSocket request")
            }
        }
        
        val request = requestBuilder.build()
        
        // 使用自定义请求连接WebSocket
        webSocketManager.connectWithRequest(request) { state ->
            connectionState = state
            when (state) {
                ConnectionState.CONNECTING -> {
                    Log.d(TAG, "WebSocket connecting...")
                    _errorMessageFlow.value = null
                }
                ConnectionState.CONNECTED -> {
                    Log.i(TAG, "WebSocket connected successfully")
                    pullingState = PULLING_STATE_MONITORING
                    _errorMessageFlow.value = null
                    startHeartbeat()
                }
                ConnectionState.DISCONNECTED -> {
                    Log.w(TAG, "WebSocket disconnected")
                    pullingState = PULLING_STATE_STOPPED
                    stopHeartbeat()
                }
                ConnectionState.ERROR -> {
                    Log.e(TAG, "WebSocket connection error")
                    pullingState = PULLING_STATE_STOPPED
                    _errorMessageFlow.value = "连接失败，正在重试..."
                    stopHeartbeat()
                }
            }
            
            serviceScope.launch {
                saveServiceState()
            }
        }
    }
    
    /**
     * 启动心跳机制
     */
    private fun startHeartbeat() {
        stopHeartbeat()
        
        heartbeatJob = serviceScope.launch {
            while (isActive && webSocketManager.isConnected()) {
                try {
                    webSocketManager.sendHeartbeat()
                    delay(30000) // 30秒发送一次心跳
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to send heartbeat", e)
                    break
                }
            }
        }
    }
    
    /**
     * 停止心跳机制
     */
    private fun stopHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = null
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
                connectWebSocket(config.websocketUrl)
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
        
        pullingState = PULLING_STATE_STOPPED
        _errorMessageFlow.value = "连接错误: ${error.message}"
        
        serviceScope.launch {
            saveServiceState()
        }
    }
    
    /**
     * 保存服务状态
     */
    private suspend fun saveServiceState() {
        try {
            val state = ServiceState(
                pullingState = pullingState,
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
     * 停止监控
     */
    private fun stopMonitoring() {
        isServiceRunning.set(false)
        pullingState = PULLING_STATE_STOPPED
        
        stopHeartbeat()
        webSocketManager.disconnect()
        
        serviceScope.launch {
            saveServiceState()
        }
        
        Log.i(TAG, "Monitoring stopped")
    }
    
    /**
     * 清理资源
     */
    private fun cleanupResources() {
        heartbeatJob?.cancel()
        configCheckJob?.cancel()
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
        
        val statusText = when (pullingState) {
            PULLING_STATE_MONITORING -> "正在监控直播间动态..."
            else -> "直播间监控已停止"
        }
        
        val connectionText = when (connectionState) {
            ConnectionState.CONNECTED -> "已连接"
            ConnectionState.CONNECTING -> "连接中"
            ConnectionState.DISCONNECTED -> "已断开"
            ConnectionState.ERROR -> "连接错误"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI直播监控服务")
            .setContentText("$statusText ($connectionText)")
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
}