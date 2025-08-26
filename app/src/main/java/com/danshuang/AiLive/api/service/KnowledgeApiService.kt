package com.danshuang.AiLive.api.service

import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.KnowledgeItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


/**
 * 知识库管理API服务
 * 负责知识库相关的CRUD操作
 */
class KnowledgeApiService(
    tokenManager: TokenManager,
    private val baseUrl: String
) : BaseApiClient(tokenManager) {
    
    /**
     * 获取知识库条目列表
     */
    suspend fun fetchKbItems(
        token: String,
        douyinId: String
    ): ServerResponse<List<KnowledgeItem>> = withContext(Dispatchers.IO) {
        val apiName = "FetchKbItems"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/fetch-kb-items"
            val postData = mapOf("douyin_id" to douyinId)
            
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
                        KnowledgeItem(
                            id = itemMap["id"] as? String ?: "",
                            typeId = itemMap["type_id"] as? String ?: "",
                            question = itemMap["question"] as? String ?: "",
                            answer = itemMap["answer"] as? String ?: "",
                            createTime = (itemMap["create_time"] as? Double)?.toLong() ?: System.currentTimeMillis(),
                            updateTime = (itemMap["update_time"] as? Double)?.toLong() ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
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