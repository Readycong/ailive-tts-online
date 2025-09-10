package com.danshuang.AiLive.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.PlaybackParams
import android.os.IBinder
import android.util.Log
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.VoiceFileInfo
import com.danshuang.AiLive.data.model.ConnectionState
import com.danshuang.AiLive.data.model.VoiceSettingsData
import com.danshuang.AiLive.service.FileDownloader
import com.danshuang.AiLive.MainActivity
import com.danshuang.AiLive.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.danshuang.AiLive.ui.screens.LiveMessageManager
import com.danshuang.AiLive.ui.screens.MessageType
import java.io.File
import java.util.Random

/**
 * 直播语音播放服务
 * 负责管理直播过程中的语音播放流程
 */
class LiveChattingService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // 服务状态
    private var isServiceRunning = false
    private var isServiceInitialized = false
    private var networkAvailable = true
    
    // 播放状态
    private var chattingState = CHATTING_STATE_STOPPED
    
    // 依赖组件
    private lateinit var serverApiUtils: ServerApiUtilsV2
    private lateinit var fileDownloader: FileDownloader
    private lateinit var loginRepository: LoginRepository
    
    // 媒体播放器和音频管理器
    private var mediaPlayer: MediaPlayer? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    
    // 语音设置
    private var currentVoiceSettings: VoiceSettingsData? = null
    
    // 添加音频参数数据类
    data class AudioParameters(val volume: Float, val pitch: Float, val speed: Float)
    
    // 广播接收器
    private lateinit var settingsUpdateReceiver: BroadcastReceiver
    
    // 前台服务通知相关
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "LiveChattingServiceChannel"
    
    companion object {
        const val CHATTING_STATE_STOPPED = 0
        const val CHATTING_STATE_PLAYING = 1
        
        private const val TAG = "LiveChattingService"
        
        // 添加状态流，用于UI观察播放状态
        private val _chattingStateFlow = MutableStateFlow(CHATTING_STATE_STOPPED)
        val chattingStateFlow: StateFlow<Int> = _chattingStateFlow
        
        // 单例控制：记录服务运行状态
        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning
        
        /**
         * 启动服务（单例控制）
         */
        fun startService(context: Context) {
            if (_isServiceRunning.value) {
                Log.d(TAG, "LiveChattingService 已经在运行，不重复启动")
                return
            }
            
            Log.d(TAG, "启动 LiveChattingService")
            val intent = Intent(context, LiveChattingService::class.java)
            context.startService(intent)
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            if (!_isServiceRunning.value) {
                Log.d(TAG, "LiveChattingService 未运行，无需停止")
                return
            }
            
            Log.d(TAG, "停止 LiveChattingService")
            val intent = Intent(context, LiveChattingService::class.java)
            context.stopService(intent)
        }
        
        /**
         * 内部方法：更新服务运行状态
         */
        internal fun updateServiceRunningState(isRunning: Boolean) {
            _isServiceRunning.value = isRunning
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        isServiceInitialized = true
        
        // 创建通知渠道
        createNotificationChannel()
        
        // 初始化音频管理器
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // 初始化音频焦点管理
        initializeAudioFocus()
        
        // 初始化依赖组件
        loginRepository = LoginRepository(this)
        val tokenManager = TokenManager(loginRepository)
        serverApiUtils = ServerApiUtilsV2.createFromContext(
            this, tokenManager, loginRepository
        )
        fileDownloader = FileDownloader(this)
        
        // 初始化语音设置
        initVoiceSettings()
        
        // 注册广播接收器
        registerSettingsUpdateReceiver()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 防止重复启动
        if (isServiceRunning) {
            Log.d(TAG, "LiveChattingService 已经在运行中，忽略重复启动")
            return START_STICKY
        }
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        
        isServiceRunning = true
        // 更新全局服务运行状态
        updateServiceRunningState(true)
        // 更新状态流
        _chattingStateFlow.value = CHATTING_STATE_PLAYING
        Log.d(TAG, "LiveChattingService 启动")
        // 启动播放流程
        serviceScope.launch {
            // 启动时先检查一次网络状态
            checkNetworkStatus()
            Log.d(TAG, "初始网络状态: $networkAvailable")
            startPlaybackFlow()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
        // 更新全局服务运行状态
        updateServiceRunningState(false)
        // 更新状态流
        _chattingStateFlow.value = CHATTING_STATE_STOPPED
        
        // 释放MediaPlayer资源
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
            mediaPlayer = null
        }
        
        // 停止前台服务
        stopForeground(true)
        
        // 释放音频焦点
        abandonAudioFocus()
        
        Log.d(TAG, "LiveChattingService 已停止")
        serviceScope.cancel()
    }
    
    /**
     * 开始播放流程
     */
    private suspend fun startPlaybackFlow() {
        // 播放阶段循环
        val stages = listOf("WELCOME", "ROOM", "PRODUCT", "KEEP", "ORDER", "THANKS")
        var currentStageIndex = 0
        
        while (isServiceRunning) {
            // 从DataStore中读取roomId，如果为空则使用默认值
            val roomId = loginRepository.liveRoomId.first() ?: "default_room_id"
            // 检查网络状态
            checkNetworkStatus()
            
            if (!networkAvailable) {
                Log.w(TAG, "网络不可用，暂停播放流程")
                // 等待一段时间后重新检查
                kotlinx.coroutines.delay(5000)
                continue
            }
            
            val stage = stages[currentStageIndex]
            Log.d(TAG, "当前播放阶段: $stage")
            
            try {
                // 获取单个话术语音文件
                val voiceFile = fetchAiKnowledge(roomId, stage)
                voiceFile?.let {
                    playVoiceFile(it)
                    // 检查服务状态，如果服务已停止则退出
                    if (!isServiceRunning) return
                }
                // 获取AI评论列表，即时并依次播放回复评论的语音
                val voiceFiles = fetchAiComments(roomId)
                playVoiceFiles(voiceFiles)

                if (stage == "THANKS") {
                    currentStageIndex = 0
                } else {
                    // 移动到下一个阶段
                    currentStageIndex = (currentStageIndex + 1) % stages.size
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放阶段 $stage 出错", e)
                // 出错时移动到下一个阶段
                currentStageIndex = (currentStageIndex + 1) % stages.size
            }
            
            // 检查是否需要继续循环
            if (!isServiceRunning) break
            
            // 添加延迟以避免过于频繁的请求
            kotlinx.coroutines.delay(1000)
        }
    }
    
    /**
     * 获取AI知识库信息
     */
    private suspend fun fetchAiKnowledge(roomId: String, stage: String): VoiceFileInfo? {
        return try {
            serverApiUtils.fetchAiKnowledges(roomId, stage)
        } catch (e: Exception) {
            Log.e(TAG, "获取AI知识库信息失败: $stage", e)
            null
        }
    }
    
    /**
     * 获取AI评论列表
     */
    private suspend fun fetchAiComments(roomId: String): List<VoiceFileInfo>? {
        return try {
            serverApiUtils.fetchAiComments(roomId, "comment")
        } catch (e: Exception) {
            Log.e(TAG, "获取AI评论列表失败", e)
            null
        }
    }
    
    /**
     * 播放单个语音文件
     */
    private fun playVoiceFile(voiceFile: VoiceFileInfo) {
        chattingState = CHATTING_STATE_PLAYING
        _chattingStateFlow.value = CHATTING_STATE_PLAYING
        
        Log.d(TAG, "开始播放语音文件: ${voiceFile.id}, URL: ${voiceFile.url}")
        
        // 将AI主播的文字内容插入到实时动态列表中
        try {
            LiveMessageManager.insertMessage(
                type = MessageType.AI_RESPONSE,
                content = voiceFile.text,
                userName = "AI主播"
            )
            Log.d(TAG, "AI主播消息已插入动态列表: ${voiceFile.text}")
        } catch (e: Exception) {
            Log.e(TAG, "插入AI主播消息失败", e)
        }
        
        // 请求音频焦点
        if (!requestAudioFocus()) {
            Log.w(TAG, "无法获取音频焦点，可能影响播放效果")
        }
        
        // 检查是否需要缓存
        var filePath: String? = null
        if (voiceFile.cache) {
            // 检查文件是否已缓存
            filePath = fileDownloader.getCacheFilePath(voiceFile.id)
            
            // 如果未缓存，则下载并缓存文件
            if (filePath == null) {
                filePath = fileDownloader.downloadAndCacheFile(voiceFile)
            }
        }
        
        // 使用同步锁确保播放完成后再返回
        val lock = java.lang.Object()
        var isPlaybackFinished = false
        var playbackError = false
        
        try {
            // 释放之前的MediaPlayer实例
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            
            // 创建新的MediaPlayer实例
            val player = MediaPlayer()
            mediaPlayer = player
            
            // 设置音频属性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
                player.setAudioAttributes(audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                player.setAudioStreamType(AudioManager.STREAM_MUSIC)
            }
            
            // 设置音量（最大音量）
            player.setVolume(1.0f, 1.0f)
            
            // 设置系统音量
            audioManager?.let { am ->
                val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                val currentVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                Log.d(TAG, "当前系统音量: $currentVolume/$maxVolume")
                
                // 如果系统音量过低，设置为50%
                if (currentVolume < maxVolume * 0.3) {
                    Log.d(TAG, "系统音量过低，设置为50%")
                    am.setStreamVolume(AudioManager.STREAM_MUSIC, (maxVolume * 0.5).toInt(), 0)
                }
            }
            
            // 设置播放完成监听器
            player.setOnCompletionListener { mp ->
                synchronized(lock) {
                    Log.d(TAG, "语音文件播放完成: ${voiceFile.id}")
                    
                    // 播放完成后释放资源
                    mp.release()
                    mediaPlayer = null
                    
                    // 更新播放状态
                    chattingState = CHATTING_STATE_STOPPED
                    _chattingStateFlow.value = CHATTING_STATE_STOPPED
                    
                    // 添加随机静音延迟
                    val silenceTime = generateRandomSilenceTime()
                    Log.d(TAG, "随机静音时间: ${silenceTime}ms")
                    
                    // 在锁外执行延迟，避免阻塞锁
                    serviceScope.launch {
                        kotlinx.coroutines.delay(silenceTime)
                        Log.d(TAG, "静音延迟完成: ${silenceTime}ms")
                        synchronized(lock) {
                            isPlaybackFinished = true
                            lock.notify()
                        }
                    }
                }
            }
            
            // 设置错误监听器
            player.setOnErrorListener { mp, what, extra ->
                synchronized(lock) {
                    Log.e(TAG, "播放音频文件出错: ${voiceFile.id}, what: $what, extra: $extra")
                    
                    // 出错时释放资源
                    mp.release()
                    mediaPlayer = null
                    
                    // 更新播放状态
                    chattingState = CHATTING_STATE_STOPPED
                    _chattingStateFlow.value = CHATTING_STATE_STOPPED
                    
                    isPlaybackFinished = true
                    playbackError = true
                    lock.notify()
                }
                true // 返回true表示已处理错误
            }
            
            // 设置准备监听器
            player.setOnPreparedListener { mp ->
                Log.d(TAG, "语音文件准备完成，开始播放: ${voiceFile.id}")
                Log.d(TAG, "音频状态 - 是否有音频焦点: $hasAudioFocus")
                
                try {
                    // 生成随机音频参数
                    val audioParams = generateRandomParameters()
                    Log.d(TAG, "生成随机音频参数: 音量=${audioParams.volume}, 音调=${audioParams.pitch}, 速度=${audioParams.speed}")
                    
                    // 应用随机参数到MediaPlayer（需要API 23+）
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            val playbackParams = PlaybackParams()
                            playbackParams.setSpeed(audioParams.speed)
                            playbackParams.setPitch(audioParams.pitch)
                            mp.playbackParams = playbackParams
                            Log.d(TAG, "成功应用播放参数: 速度=${audioParams.speed}, 音调=${audioParams.pitch}")
                        } catch (e: Exception) {
                            Log.e(TAG, "设置播放参数失败", e)
                        }
                    } else {
                        Log.d(TAG, "Android版本低于API 23，无法设置播放参数")
                    }
                    
                    // 设置音量
                    mp.setVolume(audioParams.volume, audioParams.volume)
                    Log.d(TAG, "设置音量: ${audioParams.volume}")
                    
                    // 检查音频焦点
                    if (!hasAudioFocus) {
                        Log.w(TAG, "警告：没有音频焦点，尝试重新获取")
                        requestAudioFocus()
                    }
                    
                    mp.start()
                    Log.d(TAG, "播放状态: 正在播放=${mp.isPlaying}")
                    
                    // 检查系统音量
                    audioManager?.let { am ->
                        val volume = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        Log.d(TAG, "系统音量: $volume/$maxVolume")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "启动播放失败: ${voiceFile.id}", e)
                    synchronized(lock) {
                        mp.release()
                        mediaPlayer = null
                        chattingState = CHATTING_STATE_STOPPED
                        _chattingStateFlow.value = CHATTING_STATE_STOPPED
                        isPlaybackFinished = true
                        playbackError = true
                        lock.notify()
                    }
                }
            }
            
            // 设置数据源
            if (filePath != null && File(filePath).exists()) {
                Log.d(TAG, "播放本地文件: $filePath")
                player.setDataSource(filePath)
            } else {
                Log.d(TAG, "播放网络文件: ${voiceFile.url}")
                player.setDataSource(voiceFile.url)
            }
            
            // 异步准备播放器
            player.prepareAsync()
            
            // 等待播放完成
            synchronized(lock) {
                while (!isPlaybackFinished) {
                    try {
                        lock.wait(600000) // 最多等待10分钟=600秒，即音频超过10分钟会提前结束播放
                        if (!isPlaybackFinished) {
                            Log.w(TAG, "播放超时，停止播放: ${voiceFile.id}")
                            player.stop()
                            player.release()
                            mediaPlayer = null
                            break
                        }
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "等待播放完成时被中断", e)
                        break
                    }
                }
            }
            
            if (playbackError) {
                Log.e(TAG, "播放音频文件失败: ${voiceFile.id}")
            } else {
                Log.d(TAG, "播放音频文件成功: ${voiceFile.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放音频文件失败: ${voiceFile.id}", e)
            
            // 更新播放状态
            chattingState = CHATTING_STATE_STOPPED
            _chattingStateFlow.value = CHATTING_STATE_STOPPED
            
            // 释放资源
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                } catch (releaseException: Exception) {
                    Log.e(TAG, "释放MediaPlayer时出错", releaseException)
                }
                mediaPlayer = null
            }
        }
    }
    
    /**
     * 播放语音文件列表
     */
    private fun playVoiceFiles(voiceFiles: List<VoiceFileInfo>?) {
        voiceFiles?.forEach { voiceFile ->
            // 在播放每个文件前检查服务状态和网络状态
            if (!isServiceRunning) return
            checkNetworkStatus() // 检查当前网络状态
            if (!networkAvailable) return
            
            // 播放当前文件并等待完成
            playVoiceFile(voiceFile)
            
            // 检查服务状态，如果服务已停止则退出
            if (!isServiceRunning) return
        }
    }
    
    /**
     * 检查网络状态
     */
    private fun checkNetworkStatus() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
            // 使用新的API替代已弃用的方法
            val activeNetwork = connectivityManager.activeNetwork
            
            Log.d(TAG, "检查网络状态 - activeNetwork: $activeNetwork")
            
            // 如果没有活动网络，网络不可用
            if (activeNetwork == null) {
                Log.d(TAG, "没有活动网络连接")
                networkAvailable = false
                return
            }
            
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            
            Log.d(TAG, "网络能力信息: $networkCapabilities")
            
            // 检查网络能力
            val hasInternet = networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            val hasValidated = networkCapabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
            
            Log.d(TAG, "网络状态检查 - 有网络: $hasInternet, 已验证: $hasValidated")
            
            // 网络可用的条件：有网络连接即可，不一定需要已验证
            // 有些情况下网络连接存在但尚未验证，我们仍然认为网络可用
            networkAvailable = hasInternet
            
            if (!networkAvailable) {
                Log.w(TAG, "网络不可用 - 有网络: $hasInternet, 已验证: $hasValidated")
            } else {
                Log.d(TAG, "网络可用 - 有网络: $hasInternet, 已验证: $hasValidated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查网络状态时出错", e)
            networkAvailable = false // 出错时假设网络不可用
        }
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "AI直播语音播放服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI直播语音播放后台服务"
                enableVibration(false)
                enableLights(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("AI直播语音播放")
            .setContentText("正在播放AI直播语音内容")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSound(null)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * 初始化音频焦点管理
     */
    private fun initializeAudioFocus() {
        audioManager?.let { am ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
                
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
            }
        }
    }
    
    /**
     * 音频焦点变化监听器
     */
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "获取音频焦点")
                hasAudioFocus = true
                mediaPlayer?.let {
                    if (!it.isPlaying) {
                        it.start()
                    }
                    it.setVolume(1.0f, 1.0f)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "失去音频焦点")
                hasAudioFocus = false
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "短暂失去音频焦点")
                hasAudioFocus = false
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        it.pause()
                    }
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "短暂失去音频焦点（可降低音量）")
                mediaPlayer?.setVolume(0.3f, 0.3f)
            }
        }
    }
    
    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        audioManager?.let { am ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    am.requestAudioFocus(request)
                } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
            
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            Log.d(TAG, "请求音频焦点结果: $result, 是否获取: $hasAudioFocus")
            return hasAudioFocus
        }
        return false
    }
    
    /**
     * 释放音频焦点
     */
    private fun abandonAudioFocus() {
        audioManager?.let { am ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { request ->
                    am.abandonAudioFocusRequest(request)
                } ?: AudioManager.AUDIOFOCUS_REQUEST_FAILED
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
            
            hasAudioFocus = false
            Log.d(TAG, "释放音频焦点结果: $result")
        }
    }

    /**
     * 根据设置的范围生成随机参数
     */
    private fun generateRandomParameters(): AudioParameters {
        val voiceSettings = currentVoiceSettings ?: return AudioParameters(1.0f, 1.0f, 1.0f)
        
        val random = Random()
        val volume = voiceSettings.volumeRange.min + 
            random.nextFloat() * (voiceSettings.volumeRange.max - voiceSettings.volumeRange.min)
            
        val pitch = voiceSettings.pitchRange.min + 
            random.nextFloat() * (voiceSettings.pitchRange.max - voiceSettings.pitchRange.min)
            
        val speed = voiceSettings.speedRange.min + 
            random.nextFloat() * (voiceSettings.speedRange.max - voiceSettings.speedRange.min)
            
        // 限制参数范围，确保在合理范围内
        val clampedVolume = volume.coerceIn(0.0f, 1.0f)
        val clampedPitch = pitch.coerceIn(0.5f, 2.0f)
        val clampedSpeed = speed.coerceIn(0.5f, 2.0f)
        
        return AudioParameters(clampedVolume, clampedPitch, clampedSpeed)
    }

    /**
     * 生成随机静音时间
     */
    private fun generateRandomSilenceTime(): Long {
        val voiceSettings = currentVoiceSettings
        return if (voiceSettings != null) {
            val random = Random()
            val silenceTime = voiceSettings.silenceRange.min + 
                random.nextFloat() * (voiceSettings.silenceRange.max - voiceSettings.silenceRange.min)
            (silenceTime * 1000).toLong() // 转换为毫秒
        } else {
            1000L // 默认1秒静音
        }
    }

    // 注册广播接收器
    private fun registerSettingsUpdateReceiver() {
        settingsUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.danshuang.AiLive.VOICE_SETTINGS_CHANGED") {
                    // 重新加载参数
                    reloadVoiceSettings()
                }
            }
        }
        
        val filter = IntentFilter("com.danshuang.AiLive.VOICE_SETTINGS_CHANGED")
        // 根据Android版本使用适当的registerReceiver方法
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.registerReceiver(this, settingsUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(this, settingsUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }
    
    // 初始化语音设置
    private fun initVoiceSettings() {
        serviceScope.launch {
            try {
                currentVoiceSettings = loginRepository.getVoiceSettings()
                Log.d(TAG, "语音设置初始化完成")
            } catch (e: Exception) {
                Log.e(TAG, "初始化语音设置时出错", e)
            }
        }
    }
    
    // 重新加载语音设置
    private fun reloadVoiceSettings() {
        serviceScope.launch {
            try {
                // 从DataStore读取最新参数
                currentVoiceSettings = loginRepository.getVoiceSettings()
                
                Log.d(TAG, "语音设置已更新")
            } catch (e: Exception) {
                Log.e(TAG, "重新加载语音设置时出错", e)
            }
        }
    }
}