package com.danshuang.AiLive.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.danshuang.AiLive.R
import com.danshuang.AiLive.data.model.VoiceFileInfo
import com.danshuang.AiLive.data.model.PlaybackStage
import com.danshuang.AiLive.api.service.VoiceApiService
import com.danshuang.AiLive.util.FileDownloader
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AI直播语音播放服务
 * 负责在后台播放从服务端获取的语音文件
 */
class LiveChattingService : Service() {
    
    companion object {
        private const val TAG = "LiveChattingService"
        
        // 播放状态常量
        const val CHATTING_STATE_STOPPED = 0
        const val CHATTING_STATE_PLAYING = 1
        
        // 通知相关常量
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "live_chatting"
        private const val CHANNEL_NAME = "AI直播语音播放"
        private const val CHANNEL_DESCRIPTION = "AI直播语音播放服务"
        
        // 服务状态流
        private val _chattingStateFlow = MutableStateFlow(CHATTING_STATE_STOPPED)
        val chattingStateFlow: StateFlow<Int> = _chattingStateFlow.asStateFlow()
        
        /**
         * 启动服务
         */
        fun startService(context: Context) {
            val intent = Intent(context, LiveChattingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, LiveChattingService::class.java)
            context.stopService(intent)
        }
        
        /**
         * 获取当前播放状态
         */
        fun getCurrentChattingState(): Int = _chattingStateFlow.value
    }
    
    // 核心组件
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val voiceApiService = VoiceApiService()
    private val fileDownloader = FileDownloader()
    
    // 播放控制标志
    private var isServiceInitialized = false
    
    // 服务状态
    private var chattingState = CHATTING_STATE_STOPPED
        set(value) {
            field = value
            _chattingStateFlow.value = value
            updateNotification()
        }
    
    // 播放控制
    private var currentStage: PlaybackStage = PlaybackStage.WELCOME
    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false
    private var currentVoiceList: List<VoiceFileInfo> = emptyList()
    private var currentVoiceIndex = 0
    private var consecutiveFailures = 0
    private val maxConsecutiveFailures = 5
    
    // 控制标志
    private val isServiceRunning = AtomicBoolean(false)
    private var networkAvailable = true
    
    // 网络监听
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.d(TAG, "Network available")
            networkAvailable = true
            // 网络恢复时，如果服务应该运行则继续播放
            if (chattingState == CHATTING_STATE_PLAYING) {
                startVoicePlayback()
            }
        }
        
        override fun onLost(network: Network) {
            super.onLost(network)
            Log.d(TAG, "Network lost")
            networkAvailable = false
            // 网络断开时停止播放
            if (chattingState == CHATTING_STATE_PLAYING) {
                stopVoicePlayback()
                chattingState = CHATTING_STATE_STOPPED
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "LiveChattingService created")
        
        createNotificationChannel()
        registerNetworkCallback()
        isServiceInitialized = true
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "LiveChattingService started")
        
        if (!isServiceRunning.get()) {
            isServiceRunning.set(true)
            startForegroundService()
            // 如果服务状态为播放中，则开始播放
            if (chattingState == CHATTING_STATE_PLAYING) {
                startVoicePlayback()
            }
        }
        
        return START_STICKY // 服务被杀死后自动重启
    }
    
    override fun onDestroy() {
        Log.i(TAG, "LiveChattingService destroyed")
        
        stopVoicePlayback()
        unregisterNetworkCallback()
        cleanupResources()
        isServiceRunning.set(false)
        isServiceInitialized = false
        super.onDestroy()
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null // 不支持绑定
    }
    
    /**
     * 注册网络状态监听
     */
    private fun registerNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkRequest = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback", e)
        }
    }
    
    /**
     * 取消注册网络状态监听
     */
    private fun unregisterNetworkCallback() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister network callback", e)
        }
    }
    
    /**
     * 启动语音播放流程
     */
    fun startVoicePlayback() {
        if (!networkAvailable) {
            Log.w(TAG, "Network not available, cannot start playback")
            chattingState = CHATTING_STATE_STOPPED
            return
        }
        
        if (chattingState == CHATTING_STATE_PLAYING) {
            Log.d(TAG, "Already playing, ignoring start request")
            return
        }
        
        chattingState = CHATTING_STATE_PLAYING
        currentStage = PlaybackStage.WELCOME
        playNextStage()
    }
    
    /**
     * 停止语音播放
     */
    fun stopVoicePlayback() {
        chattingState = CHATTING_STATE_STOPPED
        stopMediaPlayer()
        currentVoiceList = emptyList()
        currentVoiceIndex = 0
        consecutiveFailures = 0
        Log.i(TAG, "Voice playback stopped")
    }
    
    /**
     * 播放下一阶段
     */
    private fun playNextStage() {
        if (chattingState != CHATTING_STATE_PLAYING) {
            return
        }
        
        // 根据当前阶段确定下一阶段
        currentStage = when (currentStage) {
            PlaybackStage.WELCOME -> PlaybackStage.ROOM
            PlaybackStage.ROOM -> PlaybackStage.PRODUCT
            PlaybackStage.PRODUCT -> PlaybackStage.KEEP
            PlaybackStage.KEEP -> PlaybackStage.ORDER
            PlaybackStage.ORDER -> PlaybackStage.THANKS
            PlaybackStage.THANKS -> PlaybackStage.COMMENT
            PlaybackStage.COMMENT -> PlaybackStage.WELCOME
        }
        
        // 获取该阶段的语音文件列表
        fetchAndPlayVoiceFiles()
    }
    
    /**
     * 获取并播放语音文件
     */
    private fun fetchAndPlayVoiceFiles() {
        serviceScope.launch {
            try {
                // 模拟从服务端获取语音文件URL列表
                // TODO: 实际使用时需要调用真实的API
                currentVoiceList = getVoiceFilesForStage(currentStage)
                currentVoiceIndex = 0
                
                if (currentVoiceList.isNotEmpty()) {
                    playNextVoiceFile()
                } else {
                    // 如果没有语音文件，直接进入下一阶段
                    playNextStage()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch voice files for stage: $currentStage", e)
                // 出错时尝试进入下一阶段
                playNextStage()
            }
        }
    }
    
    /**
     * 播放下一个语音文件
     */
    private fun playNextVoiceFile() {
        if (chattingState != CHATTING_STATE_PLAYING) {
            return
        }
        
        if (currentVoiceIndex >= currentVoiceList.size) {
            // 当前阶段的语音文件播放完毕，进入下一阶段
            if (currentStage == PlaybackStage.COMMENT) {
                // COMMENT阶段播放完毕后，进入下一阶段（WELCOME）
                playNextStage()
            } else {
                // 其他阶段播放完毕后，进入COMMENT阶段
                currentStage = PlaybackStage.COMMENT
                fetchAndPlayVoiceFiles()
            }
            return
        }
        
        val voiceFile = currentVoiceList[currentVoiceIndex]
        playVoiceFile(voiceFile)
    }
    
    /**
     * 播放单个语音文件
     */
    private fun playVoiceFile(voiceFile: VoiceFileInfo) {
        serviceScope.launch {
            try {
                // 检查是否需要缓存
                val filePath = if (voiceFile.cached) {
                    getCacheFilePath(voiceFile.id)
                } else {
                    // 对于COMMENT阶段的文件，不需要缓存，直接远程播放
                    if (currentStage == PlaybackStage.COMMENT) {
                        voiceFile.url
                    } else {
                        // 下载并缓存文件
                        val cachedFilePath = downloadAndCacheVoiceFile(voiceFile)
                        // 更新缓存标记
                        val updatedVoiceFile = voiceFile.copy(cached = true)
                        currentVoiceList = currentVoiceList.toMutableList().apply {
                            set(currentVoiceIndex, updatedVoiceFile)
                        }
                        cachedFilePath
                    }
                }
                
                // 播放文件
                playMediaFile(filePath)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to play voice file: ${voiceFile.id}", e)
                // 播放失败时尝试下一个文件
                currentVoiceIndex++
                playNextVoiceFile()
            }
        }
    }
    
    /**
     * 播放媒体文件
     */
    private fun playMediaFile(filePath: String) {
        serviceScope.launch(Dispatchers.Main) {
            try {
                stopMediaPlayer()
                
                val player = MediaPlayer()
                player.setDataSource(filePath)
                player.prepareAsync()
                
                player.setOnPreparedListener { mediaPlayer ->
                    player.start()
                    isPlaying = true
                    // 播放成功，重置失败计数
                    consecutiveFailures = 0
                }
                
                player.setOnCompletionListener { mediaPlayer ->
                    // 播放完成，播放下一个文件
                    isPlaying = false
                    // 播放成功，重置失败计数
                    consecutiveFailures = 0
                    currentVoiceIndex++
                    playNextVoiceFile()
                }
                
                player.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    isPlaying = false
                    
                    // 增加失败计数
                    consecutiveFailures++
                    
                    // 如果连续失败超过阈值，暂停服务
                    if (consecutiveFailures >= maxConsecutiveFailures) {
                        Log.e(TAG, "Too many consecutive failures, stopping service")
                        stopVoicePlayback()
                        chattingState = CHATTING_STATE_STOPPED
                        return@setOnErrorListener false
                    }
                    
                    // 尝试重置MediaPlayer并重新播放当前文件
                    try {
                        mp.reset()
                        // 延迟一段时间后重试
                        serviceScope.launch {
                            delay(1000)
                            playVoiceFile(currentVoiceList[currentVoiceIndex])
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to retry playing voice file: ${currentVoiceList[currentVoiceIndex].id}", e)
                        // 重试失败时尝试下一个文件
                        currentVoiceIndex++
                        playNextVoiceFile()
                    }
                    false
                }
                
                mediaPlayer = player
            } catch (e: Exception) {
                Log.e(TAG, "Failed to setup MediaPlayer for file: $filePath", e)
                // 出错时尝试下一个文件
                currentVoiceIndex++
                playNextVoiceFile()
            }
        }
    }
    
    /**
     * 停止媒体播放器
     */
    private fun stopMediaPlayer() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            isPlaying = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer", e)
        }
    }
    
    /**
     * 下载并缓存语音文件
     */
    private suspend fun downloadAndCacheVoiceFile(voiceFile: VoiceFileInfo): String {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Downloading and caching voice file: ${voiceFile.id} from ${voiceFile.url}")
                
                // 创建缓存文件路径
                val cacheFilePath = getCacheFilePath(voiceFile.id)
                val cacheFile = File(cacheFilePath)
                
                // 如果文件已存在，直接返回
                if (cacheFile.exists()) {
                    Log.d(TAG, "Voice file already cached: ${voiceFile.id}")
                    return@withContext cacheFilePath
                }
                
                // 使用FileDownloader下载文件
                val success = fileDownloader.downloadFile(voiceFile.url, cacheFilePath)
                
                if (success) {
                    Log.d(TAG, "Successfully downloaded and cached voice file: ${voiceFile.id} to $cacheFilePath")
                    cacheFilePath
                } else {
                    Log.e(TAG, "Failed to download voice file: ${voiceFile.id}")
                    throw Exception("Failed to download voice file: ${voiceFile.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download and cache voice file: ${voiceFile.id}", e)
                throw e
            }
        }
    }
    
    /**
     * 获取缓存文件路径
     */
    private fun getCacheFilePath(fileId: String): String {
        val cacheDir = File(cacheDir, "voice_files")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, "$fileId.mp3").absolutePath
    }
    
    /**
     * 获取指定阶段的语音文件列表
     */
    private suspend fun getVoiceFilesForStage(stage: PlaybackStage): List<VoiceFileInfo> {
        return try {
            // 从服务端获取数据
            voiceApiService.getVoiceFilesForStage(stage.name.lowercase())
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch voice files for stage: $stage, using mock data", e)
            // 出错时使用模拟数据
            when (stage) {
                PlaybackStage.WELCOME -> listOf(
                    VoiceFileInfo("welcome_1", "https://example.com/voice/welcome_1.mp3", 5000, false),
                    VoiceFileInfo("welcome_2", "https://example.com/voice/welcome_2.mp3", 3500, false)
                )
                PlaybackStage.ROOM -> listOf(
                    VoiceFileInfo("room_1", "https://example.com/voice/room_1.mp3", 8000, false),
                    VoiceFileInfo("room_2", "https://example.com/voice/room_2.mp3", 6000, false)
                )
                PlaybackStage.PRODUCT -> listOf(
                    VoiceFileInfo("product_1", "https://example.com/voice/product_1.mp3", 10000, false),
                    VoiceFileInfo("product_2", "https://example.com/voice/product_2.mp3", 7500, false)
                )
                PlaybackStage.KEEP -> listOf(
                    VoiceFileInfo("keep_1", "https://example.com/voice/keep_1.mp3", 4000, false),
                    VoiceFileInfo("keep_2", "https://example.com/voice/keep_2.mp3", 5500, false)
                )
                PlaybackStage.ORDER -> listOf(
                    VoiceFileInfo("order_1", "https://example.com/voice/order_1.mp3", 9000, false),
                    VoiceFileInfo("order_2", "https://example.com/voice/order_2.mp3", 6500, false)
                )
                PlaybackStage.THANKS -> listOf(
                    VoiceFileInfo("thanks_1", "https://example.com/voice/thanks_1.mp3", 4500, false),
                    VoiceFileInfo("thanks_2", "https://example.com/voice/thanks_2.mp3", 3000, false)
                )
                PlaybackStage.COMMENT -> listOf(
                    VoiceFileInfo("comment_1", "https://example.com/voice/comment_1.mp3", 3000, false),
                    VoiceFileInfo("comment_2", "https://example.com/voice/comment_2.mp3", 2500, false)
                )
            }
        }
    }
    
    /**
     * 清理资源
     */
    private fun cleanupResources() {
        stopMediaPlayer()
        serviceScope.cancel()
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
        val statusText = when (chattingState) {
            CHATTING_STATE_PLAYING -> "正在播放AI直播语音..."
            else -> "AI直播语音播放已停止"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI直播语音播放服务")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_media_play)
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