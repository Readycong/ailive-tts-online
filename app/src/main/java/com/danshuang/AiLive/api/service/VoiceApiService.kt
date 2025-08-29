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
                        VoiceFileInfo("welcome_1", "欢迎来到直播间", "https://example.com/voice/welcome_1.mp3", false),
                        VoiceFileInfo("welcome_2", "欢迎新进直播间的朋友们", "https://example.com/voice/welcome_2.mp3", false)
                    )
                    "room" -> listOf(
                        VoiceFileInfo("room_1", "现在是直播时间", "https://example.com/voice/room_1.mp3", false),
                        VoiceFileInfo("room_2", "请大家关注主播", "https://example.com/voice/room_2.mp3", false)
                    )
                    "product" -> listOf(
                        VoiceFileInfo("product_1", "今天的产品非常优惠", "https://example.com/voice/product_1.mp3", false),
                        VoiceFileInfo("product_2", "不要错过这个机会", "https://example.com/voice/product_2.mp3", false)
                    )
                    "keep" -> listOf(
                        VoiceFileInfo("keep_1", "请大家不要离开", "https://example.com/voice/keep_1.mp3", false),
                        VoiceFileInfo("keep_2", "稍后还有更多惊喜", "https://example.com/voice/keep_2.mp3", false)
                    )
                    "order" -> listOf(
                        VoiceFileInfo("order_1", "现在下单享受折扣", "https://example.com/voice/order_1.mp3", false),
                        VoiceFileInfo("order_2", "限时优惠不要错过", "https://example.com/voice/order_2.mp3", false)
                    )
                    "thanks" -> listOf(
                        VoiceFileInfo("thanks_1", "感谢大家的支持", "https://example.com/voice/thanks_1.mp3", false),
                        VoiceFileInfo("thanks_2", "我们下次再见", "https://example.com/voice/thanks_2.mp3", false)
                    )
                    "comment" -> listOf(
                        VoiceFileInfo("comment_1", "用户发了评论", "https://example.com/voice/comment_1.mp3", false),
                        VoiceFileInfo("comment_2", "主播回复评论", "https://example.com/voice/comment_2.mp3", false)
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