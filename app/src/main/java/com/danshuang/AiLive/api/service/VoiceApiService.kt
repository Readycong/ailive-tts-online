package com.danshuang.AiLive.api.service

import android.content.Context
import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.VoiceFileInfo
import com.danshuang.AiLive.data.model.VoiceItem
import com.danshuang.AiLive.data.model.SelectedVoiceData
import com.danshuang.AiLive.data.model.SaveVoiceRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

/**
 * 语音API服务
 * 负责处理与语音文件相关的API调用
 */
class VoiceApiService(
    tokenManager: TokenManager,
    private val baseUrl: String,
    context: Context
) : BaseApiClient(tokenManager, context) {
    
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
                        VoiceFileInfo("welcome_1", "欢迎来到直播间", "https://example.com/voice/welcome_1.mp3", false, 3000L),
                        VoiceFileInfo("welcome_2", "欢迎新进直播间的朋友们", "https://example.com/voice/welcome_2.mp3", false, 4000L)
                    )
                    "room" -> listOf(
                        VoiceFileInfo("room_1", "现在是直播时间", "https://example.com/voice/room_1.mp3", false, 2500L),
                        VoiceFileInfo("room_2", "请大家关注主播", "https://example.com/voice/room_2.mp3", false, 3500L)
                    )
                    "product" -> listOf(
                        VoiceFileInfo("product_1", "今天的产品非常优惠", "https://example.com/voice/product_1.mp3", false, 4500L),
                        VoiceFileInfo("product_2", "不要错过这个机会", "https://example.com/voice/product_2.mp3", false, 3000L)
                    )
                    "keep" -> listOf(
                        VoiceFileInfo("keep_1", "请大家不要离开", "https://example.com/voice/keep_1.mp3", false, 2000L),
                        VoiceFileInfo("keep_2", "稍后还有更多惊喜", "https://example.com/voice/keep_2.mp3", false, 3500L)
                    )
                    "order" -> listOf(
                        VoiceFileInfo("order_1", "现在下单享受折扣", "https://example.com/voice/order_1.mp3", false, 4000L),
                        VoiceFileInfo("order_2", "限时优惠不要错过", "https://example.com/voice/order_2.mp3", false, 3000L)
                    )
                    "thanks" -> listOf(
                        VoiceFileInfo("thanks_1", "感谢大家的支持", "https://example.com/voice/thanks_1.mp3", false, 3500L),
                        VoiceFileInfo("thanks_2", "我们下次再见", "https://example.com/voice/thanks_2.mp3", false, 2500L)
                    )
                    "comment" -> listOf(
                        VoiceFileInfo("comment_1", "用户发了评论", "https://example.com/voice/comment_1.mp3", false, 2000L),
                        VoiceFileInfo("comment_2", "主播回复评论", "https://example.com/voice/comment_2.mp3", false, 2500L)
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
    
    /**
     * 获取当前选中的音色
     * GET /dsalive/voice/selected
     */
    suspend fun getSelectedVoice(token: String): ServerResponse<SelectedVoiceData> = withContext(Dispatchers.IO) {
        val apiName = "GetSelectedVoice"
        return@withContext try {
            val url = "${baseUrl}/dsalive/voice/selected"
            
            responseHandler.logApiCall(apiName, url, "GET", null)
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            responseHandler.handleResponse<SelectedVoiceData>(response, apiName, context)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取当前音色失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 获取全部音色列表
     * GET /dsalive/voice/lists
     */
    suspend fun getVoiceList(token: String): ServerResponse<List<VoiceItem>> = withContext(Dispatchers.IO) {
        val apiName = "GetVoiceList"
        return@withContext try {
            val url = "${baseUrl}/dsalive/voice/lists"
            
            responseHandler.logApiCall(apiName, url, "GET", null)
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            responseHandler.handleResponse<List<VoiceItem>>(response, apiName, context)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取音色列表失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 保存音色
     * POST /dsalive/voice/save
     */
    suspend fun saveVoice(
        token: String,
        voiceType: String,
        emotion: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "SaveVoice"
        return@withContext try {
            val url = "${baseUrl}/dsalive/voice/save"
            val postData = mapOf(
                "voice_type" to voiceType,
                "emotion" to emotion
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            responseHandler.handleResponse<Any>(response, apiName, context)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "保存音色失败: ${e.message}", data = null)
        }
    }
}