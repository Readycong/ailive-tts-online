package com.danshuang.AiLive.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.IBinder
import android.util.Log
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.VoiceFileInfo
import com.danshuang.AiLive.service.FileDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

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
    
    // 媒体播放器
    private var mediaPlayer: MediaPlayer? = null
    
    companion object {
        const val CHATTING_STATE_STOPPED = 0
        const val CHATTING_STATE_PLAYING = 1
        
        private const val TAG = "LiveChattingService"
        
        // 添加状态流，用于UI观察播放状态
        private val _chattingStateFlow = MutableStateFlow(CHATTING_STATE_STOPPED)
        val chattingStateFlow: StateFlow<Int> = _chattingStateFlow
        
        /**
         * 启动服务
         */
        fun startService(context: Context) {
            val intent = Intent(context, LiveChattingService::class.java)
            context.startService(intent)
        }
        
        /**
         * 停止服务
         */
        fun stopService(context: Context) {
            val intent = Intent(context, LiveChattingService::class.java)
            context.stopService(intent)
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        isServiceInitialized = true
        
        // 初始化依赖组件
        val tokenManager = TokenManager(LoginRepository(this))
        serverApiUtils = ServerApiUtilsV2.createFromContext(
            this, tokenManager, LoginRepository(this)
        )
        fileDownloader = FileDownloader(this)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
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
        // 更新状态流
        _chattingStateFlow.value = CHATTING_STATE_STOPPED
        
        serviceScope.cancel()
    }
    
    /**
     * 开始播放流程
     */
    private suspend fun startPlaybackFlow() {
        // 播放阶段循环
        val stages = listOf("WELCOME", "ROOM", "PRODUCT", "KEEP", "ORDER", "THANKS")
        var currentStageIndex = 0
        val roomId = "default_room_id" // 需要从实际场景中获取
        
        while (isServiceRunning) {
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
            // 创建新的MediaPlayer实例
            val player = MediaPlayer()
            
            // 设置播放完成监听器
            player.setOnCompletionListener { mp ->
                synchronized(lock) {
                    // 播放完成后释放资源
                    mp.release()
                    
                    // 更新播放状态
                    chattingState = CHATTING_STATE_STOPPED
                    _chattingStateFlow.value = CHATTING_STATE_STOPPED
                    
                    isPlaybackFinished = true
                    lock.notify()
                    
                    Log.d(TAG, "语音文件播放完成: ${voiceFile.id}")
                }
            }
            
            // 设置错误监听器
            player.setOnErrorListener { mp, what, extra ->
                synchronized(lock) {
                    Log.e(TAG, "播放音频文件出错: ${voiceFile.id}, what: $what, extra: $extra")
                    // 出错时释放资源
                    mp.release()
                    
                    // 更新播放状态
                    chattingState = CHATTING_STATE_STOPPED
                    _chattingStateFlow.value = CHATTING_STATE_STOPPED
                    
                    isPlaybackFinished = true
                    playbackError = true
                    lock.notify()
                }
                false
            }
            
            // 如果有文件路径，则播放本地文件；否则直接从URL播放
            if (filePath != null && File(filePath).exists()) {
                Log.d(TAG, "播放本地文件: $filePath")
                player.setDataSource(filePath)
            } else {
                Log.d(TAG, "播放网络文件: ${voiceFile.url}")
                player.setDataSource(voiceFile.url)
            }
            
            // 同步准备播放器
            player.prepare()
            
            // 开始播放
            player.start()
            
            // 等待播放完成
            synchronized(lock) {
                while (!isPlaybackFinished) {
                    try {
                        lock.wait()
                    } catch (e: InterruptedException) {
                        Log.e(TAG, "等待播放完成时被中断", e)
                        break
                    }
                }
            }
            
            if (playbackError) {
                Log.e(TAG, "播放音频文件失败: ${voiceFile.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放音频文件失败: ${voiceFile.id}", e)
            
            // 更新播放状态
            chattingState = CHATTING_STATE_STOPPED
            _chattingStateFlow.value = CHATTING_STATE_STOPPED
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
}