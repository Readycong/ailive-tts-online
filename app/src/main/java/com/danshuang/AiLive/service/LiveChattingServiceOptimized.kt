package com.danshuang.AiLive.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.SoundPool
import android.media.AudioManager
import android.media.AudioAttributes
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
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 基于SoundPool的直播语音播放服务优化版
 * 负责管理直播过程中的语音播放流程，支持音量、音调、速度的随机化设置
 */
class LiveChattingServiceOptimized : Service() {
    
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
    
    // 音频播放器和音频管理器
    private var soundPool: SoundPool? = null
    private var audioManager: AudioManager? = null
    private var hasAudioFocus = false
    
    // 音频资源管理
    private val soundIds = ConcurrentHashMap<String, Int>() // 文件ID到SoundID的映射
    private val soundDurations = ConcurrentHashMap<String, Long>() // 文件ID到音频时长的映射（毫秒）
    
    // 语音设置
    private var currentVoiceSettings: VoiceSettingsData? = null
    
    // 广播接收器
    private lateinit var settingsUpdateReceiver: BroadcastReceiver
    
    // 副屏服务通知相关
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "LiveChattingServiceChannel"
    
    companion object {
        const val CHATTING_STATE_STOPPED = 0
        const val CHATTING_STATE_PLAYING = 1
        
        private const val TAG = "LiveChattingServiceOpt"
        
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
                Log.d(TAG, "LiveChattingServiceOptimized 已经在运行，不重复启动")
                return
            }
            
            Log.d(TAG, "启动 LiveChattingServiceOptimized")
            val intent = Intent(context, LiveChattingServiceOptimized::class.java)
            context.startService(intent)
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            if (!_isServiceRunning.value) {
                Log.d(TAG, "LiveChattingServiceOptimized 未运行，无需停止")
                return
            }
            
            Log.d(TAG, "停止 LiveChattingServiceOptimized")
            val intent = Intent(context, LiveChattingServiceOptimized::class.java)
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
        
        // 初始化SoundPool
        initializeSoundPool()
        
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
            Log.d(TAG, "LiveChattingServiceOptimized 已经在运行中，忽略重复启动")
            return START_STICKY
        }
        
        // 启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())
        
        isServiceRunning = true
        // 更新全局服务运行状态
        updateServiceRunningState(true)
        // 更新状态流
        _chattingStateFlow.value = CHATTING_STATE_PLAYING
        Log.d(TAG, "LiveChattingServiceOptimized 启动")
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
        
        // 释放SoundPool资源
        releaseAllSounds()
        soundPool?.release()
        soundPool = null
        
        // 停止前台服务
        stopForeground(true)
        
        Log.d(TAG, "LiveChattingServiceOptimized 已停止")
        serviceScope.cancel()
    }
    
    /**
     * 初始化SoundPool
     */
    private fun initializeSoundPool() {
        soundPool = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            SoundPool.Builder()
                .setMaxStreams(1) // 最多同时播放1个音频流，确保音频按顺序播放
                .setAudioAttributes(attributes)
                .build()
        } else {
            SoundPool(1, AudioManager.STREAM_MUSIC, 0) // 最多同时播放1个音频流
        }
        
        // 设置加载完成监听器
        soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                Log.d(TAG, "音频加载成功，sampleId: $sampleId")
            } else {
                Log.e(TAG, "音频加载失败，sampleId: $sampleId, status: $status")
            }
        }
    }
    
    // 添加播放完成回调处理
    private fun onPlaybackCompleted(streamId: Int) {
        Log.d(TAG, "收到播放完成回调: streamId=$streamId")
        // 由于SoundPool没有直接的播放完成回调，这个方法目前不会被调用
        // 保留此方法以备将来可能的扩展
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
                    playVoiceFileWithSoundPool(it)
                    // 检查服务状态，如果服务已停止则退出
                    if (!isServiceRunning) return
                }
                // 获取AI评论列表，即时并依次播放回复评论的语音
                val voiceFiles = fetchAiComments(roomId)
                playVoiceFilesWithSoundPool(voiceFiles)

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
     * 使用SoundPool播放单个语音文件
     */
    private suspend fun playVoiceFileWithSoundPool(voiceFile: VoiceFileInfo) {
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
        
        // 如果没有获取到文件路径，使用URL
        if (filePath == null || !File(filePath).exists()) {
            filePath = voiceFile.url
        }
        
        // 获取音频时长
        val duration = getAudioDuration(filePath, voiceFile.id, voiceFile)
        
        // 加载音频到SoundPool并等待加载完成
        val soundId = loadSoundAndWait(voiceFile.id, filePath)
        if (soundId != -1) {
            // 生成随机音频参数
            val audioParams = generateRandomParameters()
            
            // 播放音频
            val streamId = playSound(soundId, audioParams.volume, audioParams.pitch, audioParams.speed)
            
            if (streamId != -1) {
                // 等待播放完成
                waitForPlaybackCompletion(voiceFile.id, duration, audioParams.speed)
            }
        }
        
        // 更新播放状态
        chattingState = CHATTING_STATE_STOPPED
        _chattingStateFlow.value = CHATTING_STATE_STOPPED
    }
    
    /**
     * 获取音频文件时长（毫秒）
     */
    private fun getAudioDuration(filePath: String, fileId: String, voiceFile: VoiceFileInfo): Long {
        // 首先尝试从VoiceFileInfo中获取时长
        voiceFile.duration?.let { 
            soundDurations[fileId] = it
            Log.d(TAG, "从VoiceFileInfo获取时长: fileId=$fileId, duration=$it ms")
            return it 
        }
        
        // 然后尝试从缓存中获取时长
        soundDurations[fileId]?.let { 
            Log.d(TAG, "从缓存获取时长: fileId=$fileId, duration=$it ms")
            return it 
        }
        
        // 使用MediaMetadataRetriever获取音频时长
        var duration: Long = 0
        val retriever = android.media.MediaMetadataRetriever()
        try {
            if (filePath.startsWith("http")) {
                // 网络文件
                retriever.setDataSource(filePath, HashMap<String, String>())
            } else {
                // 本地文件
                retriever.setDataSource(filePath)
            }
            
            val durationString = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            duration = durationString?.toLongOrNull() ?: 0
            
            if (duration > 0) {
                soundDurations[fileId] = duration
                Log.d(TAG, "通过MediaMetadataRetriever获取时长成功: fileId=$fileId, duration=$duration ms")
            } else {
                // 使用默认时长
                duration = 5000L
                soundDurations[fileId] = duration
                Log.d(TAG, "无法获取音频时长，使用默认时长: fileId=$fileId, duration=$duration ms")
            }
        } catch (e: Exception) {
            // 使用默认时长
            duration = 5000L
            soundDurations[fileId] = duration
            Log.e(TAG, "获取音频时长时出错，使用默认时长: fileId=$fileId", e)
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "释放MediaMetadataRetriever时出错: fileId=$fileId", e)
            }
        }
        
        return duration
    }
    
    /**
     * 使用SoundPool播放语音文件列表
     */
    private suspend fun playVoiceFilesWithSoundPool(voiceFiles: List<VoiceFileInfo>?) {
        voiceFiles?.forEach { voiceFile ->
            // 在播放每个文件前检查服务状态和网络状态
            if (!isServiceRunning) return
            checkNetworkStatus() // 检查当前网络状态
            if (!networkAvailable) return
            
            // 播放当前文件并等待完成
            playVoiceFileWithSoundPool(voiceFile)
            
            // 检查服务状态，如果服务已停止则退出
            if (!isServiceRunning) return
        }
    }
    
    /**
     * 加载音频文件到SoundPool
     */
    private fun loadSound(fileId: String, filePath: String): Int {
        return try {
            // 检查是否已经加载过
            soundIds[fileId]?.let { return it }
            
            // 加载音频文件
            val soundId = soundPool?.load(filePath, 1) ?: -1
            if (soundId != -1) {
                soundIds[fileId] = soundId
                Log.d(TAG, "音频文件加载成功: $fileId, soundId: $soundId")
            } else {
                Log.e(TAG, "音频文件加载失败: $fileId")
            }
            soundId
        } catch (e: Exception) {
            Log.e(TAG, "加载音频文件时出错: $fileId", e)
            -1
        }
    }
    
    /**
     * 加载音频文件到SoundPool并等待加载完成
     */
    private suspend fun loadSoundAndWait(fileId: String, filePath: String): Int {
        return try {
            // 检查是否已经加载过
            soundIds[fileId]?.let { return it }
            
            // 保存原来的加载完成监听器
            var originalListener: SoundPool.OnLoadCompleteListener? = null
            soundPool?.let { sp ->
                // 使用反射获取当前的监听器（注意：这只是一个示例，实际使用中可能需要其他方式保存原始监听器）
                // 这里我们直接重新设置监听器，因为SoundPool只能设置一个监听器
            }
            
            // 创建等待加载完成的回调
            val loadCompleteDeferred = kotlinx.coroutines.CompletableDeferred<Int>()
            
            // 设置一次性加载完成监听器
            val loadCompleteListener = SoundPool.OnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    Log.d(TAG, "音频加载成功，sampleId: $sampleId")
                    loadCompleteDeferred.complete(sampleId)
                } else {
                    Log.e(TAG, "音频加载失败，sampleId: $sampleId, status: $status")
                    loadCompleteDeferred.completeExceptionally(Exception("音频加载失败，status: $status"))
                }
            }
            
            soundPool?.setOnLoadCompleteListener(loadCompleteListener)
            
            // 加载音频文件
            val soundId = soundPool?.load(filePath, 1) ?: -1
            if (soundId != -1) {
                soundIds[fileId] = soundId
                Log.d(TAG, "开始加载音频文件: $fileId, soundId: $soundId")
                
                // 等待加载完成，最多等待5秒
                withTimeoutOrNull(5000) {
                    loadCompleteDeferred.await()
                } ?: run {
                    Log.e(TAG, "音频加载超时: $fileId")
                    soundPool?.unload(soundId)
                    soundIds.remove(fileId)
                    // 恢复原来的监听器
                    soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            Log.d(TAG, "音频加载成功，sampleId: $sampleId")
                        } else {
                            Log.e(TAG, "音频加载失败，sampleId: $sampleId, status: $status")
                        }
                    }
                    return -1
                }
                
                Log.d(TAG, "音频文件加载完成: $fileId, soundId: $soundId")
                
                // 恢复原来的监听器
                soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        Log.d(TAG, "音频加载成功，sampleId: $sampleId")
                    } else {
                        Log.e(TAG, "音频加载失败，sampleId: $sampleId, status: $status")
                    }
                }
            } else {
                Log.e(TAG, "音频文件加载失败: $fileId")
                // 恢复原来的监听器
                soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                    if (status == 0) {
                        Log.d(TAG, "音频加载成功，sampleId: $sampleId")
                    } else {
                        Log.e(TAG, "音频加载失败，sampleId: $sampleId, status: $status")
                    }
                }
            }
            soundId
        } catch (e: Exception) {
            Log.e(TAG, "加载音频文件时出错: $fileId", e)
            // 恢复原来的监听器
            soundPool?.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == 0) {
                    Log.d(TAG, "音频加载成功，sampleId: $sampleId")
                } else {
                    Log.e(TAG, "音频加载失败，sampleId: $sampleId, status: $status")
                }
            }
            -1
        }
    }
    
    /**
     * 播放音频（支持参数控制）
     */
    private fun playSound(soundId: Int, volume: Float, pitch: Float, speed: Float): Int {
        return try {
            val streamId = soundPool?.play(
                soundId,
                volume, // 左声道音量
                volume, // 右声道音量
                1,      // 优先级
                0,      // 循环模式，0表示不循环
                pitch * speed // 播放速率（SoundPool通过音调控制播放速率）
            ) ?: -1
            
            if (streamId != -1) {
                Log.d(TAG, "音频播放成功: soundId: $soundId, streamId: $streamId, volume: $volume, pitch: $pitch, speed: $speed")
            } else {
                Log.e(TAG, "音频播放失败: soundId: $soundId")
            }
            streamId
        } catch (e: Exception) {
            Log.e(TAG, "播放音频时出错: soundId: $soundId", e)
            -1
        }
    }
    
    /**
     * 等待播放完成
     * 根据音频时长和播放速度计算等待时间
     */
    private suspend fun waitForPlaybackCompletion(fileId: String, duration: Long, speed: Float) {
        // 计算实际播放时间（考虑播放速度）
        val adjustedDuration = (duration / speed).toLong()
        // 添加一些缓冲时间确保播放完成
        val waitTime = max(adjustedDuration + 500, 1000) // 至少等待1秒
        
        Log.d(TAG, "等待音频播放完成: fileId=$fileId, duration=$duration, speed=$speed, waitTime=$waitTime ms")
        
        // 等待计算出的时间
        kotlinx.coroutines.delay(waitTime)
        
        Log.d(TAG, "音频播放完成: fileId=$fileId")
    }
    
    /**
     * 释放所有音频资源
     */
    private fun releaseAllSounds() {
        soundPool?.let { sp ->
            soundIds.values.forEach { soundId ->
                sp.unload(soundId)
            }
            soundIds.clear()
            soundDurations.clear()
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
     * 音频参数数据类
     */
    data class AudioParameters(val volume: Float, val pitch: Float, val speed: Float)
    
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