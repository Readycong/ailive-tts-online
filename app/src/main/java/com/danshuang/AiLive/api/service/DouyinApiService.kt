package com.danshuang.AiLive.api.service

import android.content.Context
import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.DouyinAccount
import com.danshuang.AiLive.data.model.AiComment
import com.danshuang.AiLive.data.model.VoiceFileInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * 抖音直播相关API服务
 * 负责抖音账号绑定、直播间管理等相关API调用
 */
class DouyinApiService(
    tokenManager: TokenManager,
    private val baseUrl: String,
    context: Context
) : BaseApiClient(tokenManager, context) {
    
    /**
     * 绑定抖音直播账号
     */
    suspend fun bindDouyinLive(
        token: String,
        douyinId: String,
        nickname: String,
        avatar: String,
        roomId: String,
        userHomeUrl: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "BindDouyinLive"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/bind"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "nickname" to nickname,
                "avatar" to avatar,
                "room_id" to roomId,
                "user_home_url" to userHomeUrl
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
            ServerResponse(code = 0, msg = "绑定失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 获取当前绑定的账户信息
     */
    suspend fun getBinding(token: String): ServerResponse<Map<String, Any>> = withContext(Dispatchers.IO) {
        val apiName = "GetBinding"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/get-binding"
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取绑定信息失败: ${e.message}", data = emptyMap<String, Any>())
        }
    }
    
    /**
     * 获取绑定的账号列表
     */
    suspend fun listAccounts(token: String): ServerResponse<List<DouyinAccount>> = withContext(Dispatchers.IO) {
        val apiName = "ListAccounts"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/list-binding"
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<List<Map<String, Any>>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val accounts = serverResponse.data.mapNotNull { accountMap ->
                    try {
                        DouyinAccount(
                            douyinId = accountMap["douyin_id"] as? String ?: "",
                            nickname = accountMap["nickname"] as? String ?: "",
                            avatar = accountMap["avatar"] as? String ?: "",
                            roomId = accountMap["room_id"] as? String ?: "",
                            bindTime = (accountMap["bind_time"] as? Double)?.toLong() ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null // 跳过解析失败的条目
                    }
                }
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = accounts
                )
            } else {
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = emptyList()
                )
            }
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取账号列表失败: ${e.message}", data = emptyList())
        }
    }
    
    /**
     * 获取AI知识库信息
     * 返回语音文件信息用于播放
     */
    suspend fun fetchAiKnowledges(
        token: String,
        roomId: String,
        stage: String
    ): VoiceFileInfo? = withContext(Dispatchers.IO) {
        val apiName = "FetchAiKnowledges"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/fetch-kb-ai-list"
            val postData = mapOf(
                "stage" to stage,
                "room_id" to roomId
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            serverResponse.data?.let { map ->
                VoiceFileInfo(
                    id = map["id"].toString(),
                    text = map["text"] as? String ?: "",
                    url = map["url"] as? String ?: "",
                    cache = map["cache"] as? Boolean ?: false
                )
            }
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            null
        }
    }
    
    /**
     * 获取AI评论
     * 返回AI评论音频内容列表
     */
    suspend fun fetchAiComments(
        token: String,
        roomId: String,
        stage: String
    ): List<VoiceFileInfo>? = withContext(Dispatchers.IO) {
        val apiName = "FetchAiComments"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/fetch-play-comments"
            val postData = mapOf(
                "room_id" to roomId,
                "step" to stage
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<List<Map<String, Any>>>(response, apiName, context)
            
            serverResponse.data?.mapNotNull { item ->
                try {
                    val commentMap = item as Map<String, Any>
                    VoiceFileInfo(
                        id = commentMap["id"].toString(),
                        text = commentMap["text"] as? String ?: "",
                        url = commentMap["url"] as? String ?: "",
                        cache = commentMap["cache"] as? Boolean ?: false
                    )
                } catch (e: Exception) {
                    null // 跳过解析失败的条目
                }
            }
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            null
        }
    }
    
    /**
     * 添加主播评论
     */
    suspend fun addHostComment(
        token: String,
        douyinId: String,
        roomId: String,
        type: String,
        content: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "AddHostComment"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/add-host-comment"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "room_id" to roomId,
                "type" to type,
                "content" to content
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
            ServerResponse(code = 0, msg = "添加评论失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 上报在线用户信息
     */
    suspend fun reportOnlineUsers(
        token: String,
        douyinId: String,
        roomId: String,
        users: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "ReportOnlineUsers"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/onlineusers"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "room_id" to roomId,
                "users" to users
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
            ServerResponse(code = 0, msg = "上报用户信息失败: ${e.message}", data = null)
        }
    }
}