package com.danshuang.AiLive.api.service

import android.content.Context
import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import java.net.URLEncoder

/**
 * 直播间API服务
 * 负责处理与直播间相关的API调用
 */
class LiveRoomApiService(
    tokenManager: TokenManager,
    private val baseUrl: String,
    context: Context
) : BaseApiClient(tokenManager, context) {
    
    companion object {
        private const val TAG = "LiveRoomApiService"
    }
    
    /**
     * 获取WebSocket配置
     * TODO: 实际使用时需要调用真实的API
     */
    suspend fun getWebSocketConfig(): WebSocketConfig {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching WebSocket config...")
                
                // TODO: 替换为真实的API调用
                // 模拟API响应
                val config = WebSocketConfig(
                    websocketUrl = "wss://example.com/live/websocket",
                    lastUpdateTime = System.currentTimeMillis(),
                    isActive = true
                )
                
                Log.i(TAG, "Successfully fetched WebSocket config: ${config.websocketUrl}")
                config
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch WebSocket config", e)
                throw e
            }
        }
    }
    
    /**
     * 获取直播间详细信息
     * TODO: 实际使用时需要调用真实的API
     */
    suspend fun getLiveRoomDetails(roomId: String): LiveRoomDetailsResponse {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching live room details for: $roomId")
                
                // TODO: 替换为真实的API调用
                // 模拟API响应
                val hostInfo = HostInfo(
                    hostId = "host_$roomId",
                    nickname = "主播_$roomId",
                    avatar = "https://example.com/avatar.jpg",
                    followerCount = 1000
                )
                
                val roomInfo = RoomInfo(
                    title = "直播间_$roomId",
                    status = 1, // 直播中
                    viewerCount = 500,
                    startTime = System.currentTimeMillis(),
                    coverUrl = "https://example.com/cover.jpg"
                )
                
                val result = LiveRoomDetailsResponse(
                    roomId = roomId,
                    hostInfo = hostInfo,
                    roomInfo = roomInfo,
                    streamInfo = null
                )
                
                Log.i(TAG, "Successfully fetched room details: ${hostInfo.nickname} - ${roomInfo.title}")
                result
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch live room details for $roomId", e)
                throw e
            }
        }
    }
    
    /**
     * 获取直播场次列表
     * @param token 用户token
     * @param startTime 开始时间 (格式: YYYY-MM-DD HH:mm:ss)，null表示不限制
     * @param endTime 结束时间 (格式: YYYY-MM-DD HH:mm:ss)，null表示不限制
     * @param page 页码
     * @param pageSize 每页数量
     * @return 直播场次列表响应
     */
    suspend fun getLiveSessionList(
        token: String,
        startTime: String? = null,
        endTime: String? = null,
        page: Int = 1,
        pageSize: Int = 10
    ): ServerResponse<LiveSessionList> = withContext(Dispatchers.IO) {
        val apiName = "GetLiveSessionList"
        return@withContext try {
            // 构建URL参数（使用create_time[0]和create_time[1]格式）
            val urlParams = buildString {
                append("?page=$page")
                append("&page_size=$pageSize")
                // 只有当时间参数不为null时才添加
                if (startTime != null && endTime != null) {
                    append("&create_time%5B0%5D=${URLEncoder.encode(startTime, "UTF-8")}")
                    append("&create_time%5B1%5D=${URLEncoder.encode(endTime, "UTF-8")}")
                }
            }
            val url = "${baseUrl}/dsalive/billing/lists$urlParams"
            
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val dataMap = serverResponse.data
                val sessionListMap = dataMap as? Map<*, *>
                
                if (sessionListMap != null) {
                    val sessions = (sessionListMap["data"] as? List<*>)?.mapNotNull { sessionItem ->
                        try {
                            val item = sessionItem as? Map<*, *>
                            item?.let {
                                LiveSession(
                                    localId = (it["local_id"] as? Number)?.toInt() ?: 0,
                                    roomId = it["room_id"] as? String ?: "",
                                    statusName = it["status_name"] as? String ?: "",
                                    status = (it["status"] as? Number)?.toInt() ?: 0,
                                    duration = (it["duration"] as? Number)?.toInt() ?: 0,
                                    startTime = it["start_time"] as? String ?: "",
                                    endTime = it["end_time"] as? String ?: "",
                                    createTime = it["create_time"] as? String ?: "",
                                    updateTime = it["update_time"] as? String ?: "",
                                    usePoint = (it["use_point"] as? Number)?.toInt() ?: 0,
                                    balanceData = it["balance_data"] as? String
                                )
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse session item", e)
                            null
                        }
                    } ?: emptyList()
                    
                    val liveSessionList = LiveSessionList(
                        total = (sessionListMap["total"] as? Number)?.toInt() ?: 0,
                        perPage = (sessionListMap["per_page"] as? Number)?.toInt() ?: 15,
                        currentPage = (sessionListMap["current_page"] as? Number)?.toInt() ?: 1,
                        lastPage = (sessionListMap["last_page"] as? Number)?.toInt() ?: 1,
                        data = sessions
                    )
                    
                    ServerResponse(
                        code = serverResponse.code,
                        msg = serverResponse.msg,
                        data = liveSessionList
                    )
                } else {
                    ServerResponse(code = 0, msg = "数据格式错误", data = null)
                }
            } else {
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = null
                )
            }
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取直播场次列表失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 获取直播统计数据
     * @param token 用户token
     * @param startTime 开始时间 (格式: YYYY-MM-DD HH:mm:ss)，null表示不限制
     * @param endTime 结束时间 (格式: YYYY-MM-DD HH:mm:ss)，null表示不限制
     * @return 直播统计数据响应
     */
    suspend fun getLiveStatistics(
        token: String,
        startTime: String? = null,
        endTime: String? = null
    ): ServerResponse<LiveStatistics> = withContext(Dispatchers.IO) {
        val apiName = "GetLiveStatistics"
        return@withContext try {
            // 构建URL参数（使用create_time[0]和create_time[1]格式）
            val urlParams = buildString {
                append("?")
                // 只有当时间参数不为null时才添加
                if (startTime != null && endTime != null) {
                    append("create_time%5B0%5D=${URLEncoder.encode(startTime, "UTF-8")}")
                    append("&create_time%5B1%5D=${URLEncoder.encode(endTime, "UTF-8")}")
                }
            }
            val url = "${baseUrl}/dsalive/billing/stat$urlParams"
            
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val dataMap = serverResponse.data
                val statistics = LiveStatistics(
                    totalCount = (dataMap["total_count"] as? Number)?.toInt() ?: 0,
                    totalUsePoint = (dataMap["total_use_point"] as? Number)?.toInt() ?: 0,
                    totalBalanceData = dataMap["total_balance_data"] as? String ?: "0.00"
                )
                
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = statistics
                )
            } else {
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = null
                )
            }
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取直播统计数据失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 获取直播间交互记录
     * @param token 用户token
     * @param roomId 直播间ID
     * @return 交互记录列表响应
     */
    suspend fun getLiveRoomHistory(
        token: String,
        roomId: String
    ): ServerResponse<List<LiveInteractionRecord>> = withContext(Dispatchers.IO) {
        val apiName = "GetLiveRoomHistory"
        return@withContext try {
            val url = "${baseUrl}/dsalive/billing/get-room-history"
            val postData = mapOf(
                "room_id" to roomId
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<List<Map<String, Any>>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val records = serverResponse.data.mapNotNull { recordMap ->
                    try {
                        LiveInteractionRecord(
                            method = recordMap["method"] as? String ?: "",
                            liveUserNickname = recordMap["live_user_nickname"] as? String ?: "",
                            question = recordMap["question"] as? String ?: "",
                            answer = recordMap["answer"] as? String ?: "",
                            usePoint = (recordMap["use_point"] as? Number)?.toInt(),
                            createTime = recordMap["create_time"] as? String ?: "",
                            playTime = (recordMap["play_time"] as? Number)?.toInt(),
                            roomId = recordMap["room_id"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse interaction record", e)
                        null
                    }
                }
                
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = records
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
            ServerResponse(code = 0, msg = "获取交互记录失败: ${e.message}", data = emptyList())
        }
    }
}