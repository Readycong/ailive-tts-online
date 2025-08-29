package com.danshuang.AiLive.api.service

import com.danshuang.AiLive.data.model.VoiceFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * 语音API服务
 * 负责处理与语音文件相关的API调用
 */
class VoiceApiService {
    
    companion object {
        private const val TAG = "VoiceApiService"
    }
    
    /**
     * 获取指定阶段的语音文件列表
     * @param stage 播放阶段
     * @return 语音文件信息列表
     */
    suspend fun getVoiceFilesForStage(stage: String): List<VoiceFileInfo> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching voice files for stage: $stage")
                
                // TODO: 实际使用时需要调用真实的API
                // 模拟API响应
                val result = when (stage.lowercase()) {
                    "welcome" -> listOf(
                        VoiceFileInfo("welcome_1", "https://example.com/voice/welcome_1.mp3", 5000, false),
                        VoiceFileInfo("welcome_2", "https://example.com/voice/welcome_2.mp3", 3500, false)
                    )
                    "room" -> listOf(
                        VoiceFileInfo("room_1", "https://example.com/voice/room_1.mp3", 8000, false),
                        VoiceFileInfo("room_2", "https://example.com/voice/room_2.mp3", 6000, false)
                    )
                    "product" -> listOf(
                        VoiceFileInfo("product_1", "https://example.com/voice/product_1.mp3", 10000, false),
                        VoiceFileInfo("product_2", "https://example.com/voice/product_2.mp3", 7500, false)
                    )
                    "keep" -> listOf(
                        VoiceFileInfo("keep_1", "https://example.com/voice/keep_1.mp3", 4000, false),
                        VoiceFileInfo("keep_2", "https://example.com/voice/keep_2.mp3", 5500, false)
                    )
                    "order" -> listOf(
                        VoiceFileInfo("order_1", "https://example.com/voice/order_1.mp3", 9000, false),
                        VoiceFileInfo("order_2", "https://example.com/voice/order_2.mp3", 6500, false)
                    )
                    "thanks" -> listOf(
                        VoiceFileInfo("thanks_1", "https://example.com/voice/thanks_1.mp3", 4500, false),
                        VoiceFileInfo("thanks_2", "https://example.com/voice/thanks_2.mp3", 3000, false)
                    )
                    "comment" -> listOf(
                        VoiceFileInfo("comment_1", "https://example.com/voice/comment_1.mp3", 3000, false),
                        VoiceFileInfo("comment_2", "https://example.com/voice/comment_2.mp3", 2500, false)
                    )
                    else -> emptyList()
                }
                
                Log.i(TAG, "Successfully fetched ${result.size} voice files for stage: $stage")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch voice files for stage: $stage", e)
                throw e
            }
        }
    }
}