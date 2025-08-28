package com.danshuang.AiLive.api.service

import android.content.Context
import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.KnowledgeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*


/**
 * 知识库管理API服务
 * 负责知识库相关的CRUD操作
 */
class KnowledgeApiService(
    tokenManager: TokenManager,
    private val baseUrl: String,
    context: Context
) : BaseApiClient(tokenManager, context) {
    
    /**
     * 获取知识库条目列表
     */
    suspend fun fetchKbItems(
        token: String,
        typeId: String,
        kbId: String? = null
    ): ServerResponse<List<KnowledgeItem>> = withContext(Dispatchers.IO) {
        val apiName = "FetchKbItems"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/fetch-kb-items"
            val postData = mapOf(
                "type_id" to typeId,
                "kb_id" to (kbId ?: "0")
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequest(request)
            val serverResponse = responseHandler.handleResponse<List<Map<String, Any>>>(response, apiName)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val items = serverResponse.data.mapNotNull { itemMap ->
                    try {
                        // 解析时间字符串为时间戳
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        
                        val createTime = try {
                            val createTimeStr = itemMap["create_time"] as? String
                            if (createTimeStr != null) {
                                dateFormat.parse(createTimeStr)?.time ?: System.currentTimeMillis()
                            } else {
                                System.currentTimeMillis()
                            }
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        
                        val updateTime = try {
                            val updateTimeStr = itemMap["update_time"] as? String
                            if (updateTimeStr != null && updateTimeStr != "null") {
                                dateFormat.parse(updateTimeStr)?.time ?: createTime
                            } else {
                                createTime
                            }
                        } catch (e: Exception) {
                            createTime
                        }
                        
                        KnowledgeItem(
                            id = (itemMap["id"] as? Number)?.toString() ?: "",
                            typeId = typeId, // 使用传入的 typeId 参数
                            question = itemMap["question"] as? String ?: "",
                            answer = itemMap["answer"] as? String ?: "",
                            createTime = createTime,
                            updateTime = updateTime
                        )
                    } catch (e: Exception) {
                        responseHandler.logApiError("$apiName-ItemParsing", e)
                        null // 跳过解析失败的条目
                    }
                }
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = items
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
            ServerResponse(code = 0, msg = "获取知识库失败: ${e.message}", data = emptyList())
        }
    }
    
    /**
     * 更新知识库条目
     */
    suspend fun updateKbItem(
        token: String,
        douyinId: String,
        typeId: String,
        id: String,
        question: String,
        answer: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "UpdateKbItem"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/update-kb-item"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "type_id" to typeId,
                "id" to id,
                "question" to question,
                "answer" to answer
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequest(request)
            responseHandler.handleSimpleResponse(response, apiName)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "更新知识库条目失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 删除知识库条目
     */
    suspend fun deleteKbItem(
        token: String,
        douyinId: String,
        id: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "DeleteKbItem"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/delete-kb-item"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "id" to id
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequest(request)
            responseHandler.handleSimpleResponse(response, apiName)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "删除知识库条目失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 添加知识库条目（如果API支持的话）
     */
    suspend fun addKbItem(
        token: String,
        douyinId: String,
        typeId: String,
        question: String,
        answer: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "AddKbItem"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/add-kb-item"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "type_id" to typeId,
                "question" to question,
                "answer" to answer
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequest(request)
            responseHandler.handleSimpleResponse(response, apiName)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "添加知识库条目失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 批量操作知识库条目
     */
    suspend fun batchUpdateKbItems(
        token: String,
        douyinId: String,
        items: List<KnowledgeItem>
    ): List<ServerResponse<Any>> {
        return items.map { item ->
            updateKbItem(
                token = token,
                douyinId = douyinId,
                typeId = item.typeId,
                id = item.id,
                question = item.question,
                answer = item.answer
            )
        }
    }
    
    /**
     * 批量删除知识库条目
     */
    suspend fun batchDeleteKbItems(
        token: String,
        douyinId: String,
        itemIds: List<String>
    ): List<ServerResponse<Any>> {
        return itemIds.map { id ->
            deleteKbItem(token, douyinId, id)
        }
    }
}