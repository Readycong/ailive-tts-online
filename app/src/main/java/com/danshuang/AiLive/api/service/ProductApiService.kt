package com.danshuang.AiLive.api.service

import android.content.Context
import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.ProductItem
import com.danshuang.AiLive.data.model.ProductListData
import com.danshuang.AiLive.data.model.AiDescriptionResult
import com.danshuang.AiLive.data.model.ProductAddResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 商品管理API服务
 * 负责商品卡相关的CRUD操作
 */
class ProductApiService(
    tokenManager: TokenManager,
    private val baseUrl: String,
    context: Context
) : BaseApiClient(tokenManager, context) {
    
    /**
     * 获取商品列表
     */
    suspend fun fetchProductList(
        token: String,
        page: Int
    ): ServerResponse<ProductListData> = withContext(Dispatchers.IO) {
        val apiName = "FetchProductList"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-list"
            val postData = mapOf(
                "page" to page
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val dataMap = serverResponse.data
                val total = (dataMap["total"] as? Number)?.toInt() ?: 0
                val itemsList = (dataMap["data"] as? List<*>)?.mapNotNull { item ->
                    parseProductItem(item as? Map<*, *>)
                } ?: emptyList()
                
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = ProductListData(total = total, data = itemsList)
                )
            } else {
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = ProductListData(total = 0, data = emptyList())
                )
            }
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取商品列表失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 获取商品详情
     */
    suspend fun fetchProductDetail(
        token: String,
        id: Int
    ): ServerResponse<ProductItem> = withContext(Dispatchers.IO) {
        val apiName = "FetchProductDetail"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-detail"
            val postData = mapOf(
                "id" to id
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val item = parseProductItem(serverResponse.data)
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = item
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
            ServerResponse(code = 0, msg = "获取商品详情失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 新增商品
     */
    suspend fun addProduct(
        token: String,
        productName: String,
        price: String,
        coverImage: String?,
        coverImageBase64: String?,
        description: String,
        isOnline: Int,
        enableAiVoice: Int,
        stock: Int? = null
    ): ServerResponse<ProductAddResult> = withContext(Dispatchers.IO) {
        val apiName = "AddProduct"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-add"
            val postData = mutableMapOf<String, Any>(
                "product_name" to productName,
                "price" to price,
                "description" to description,
                "is_online" to isOnline,
                "enable_ai_voice" to enableAiVoice
            )
            if (coverImageBase64 != null) {
                postData["cover_image_base64"] = coverImageBase64
            } else {
                coverImage?.let { postData["cover_image"] = it }
            }
            stock?.let { postData["stock"] = it }
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val id = (serverResponse.data["id"] as? Number)?.toInt() ?: 0
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = ProductAddResult(id = id)
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
            ServerResponse(code = 0, msg = "添加商品失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 编辑商品
     */
    suspend fun editProduct(
        token: String,
        id: Int,
        productName: String,
        price: String,
        coverImage: String?,
        coverImageBase64: String?,
        description: String,
        isOnline: Int,
        enableAiVoice: Int,
        weight: Int? = null,
        stock: Int? = null,
        sort: Int? = null,
        aiDescription: String? = null
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "EditProduct"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-edit"
            val postData = mutableMapOf<String, Any>(
                "id" to id,
                "product_name" to productName,
                "price" to price,
                "description" to description,
                "is_online" to isOnline,
                "enable_ai_voice" to enableAiVoice
            )
            if (coverImageBase64 != null) {
                postData["cover_image_base64"] = coverImageBase64
            } else {
                coverImage?.let { postData["cover_image"] = it }
            }
            weight?.let { postData["weight"] = it }
            stock?.let { postData["stock"] = it }
            sort?.let { postData["sort"] = it }
            aiDescription?.let { postData["ai_description"] = it }
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            responseHandler.handleResponse<Any>(response, apiName, context)
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "编辑商品失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 删除商品
     */
    suspend fun deleteProduct(
        token: String,
        id: Int
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "DeleteProduct"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-delete"
            val postData = mapOf(
                "id" to id
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
            ServerResponse(code = 0, msg = "删除商品失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 切换上架状态
     */
    suspend fun toggleOnline(
        token: String,
        id: Int,
        isOnline: Int
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "ToggleProductOnline"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-toggle-online"
            val postData = mapOf(
                "id" to id,
                "is_online" to isOnline
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
            ServerResponse(code = 0, msg = "切换上架状态失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 切换AI讲解开关
     */
    suspend fun toggleAiVoice(
        token: String,
        id: Int,
        enableAiVoice: Int
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "ToggleProductAiVoice"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-toggle-aivoice"
            val postData = mapOf(
                "id" to id,
                "enable_ai_voice" to enableAiVoice
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
            ServerResponse(code = 0, msg = "切换AI讲解状态失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 生成AI讲解文案
     */
    suspend fun generateAiDescription(
        token: String,
        id: Int
    ): ServerResponse<AiDescriptionResult> = withContext(Dispatchers.IO) {
        val apiName = "GenerateAiDescription"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-ai-description"
            val postData = mapOf(
                "id" to id
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val aiDescription = serverResponse.data["ai_description"] as? String ?: ""
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = AiDescriptionResult(aiDescription = aiDescription)
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
            ServerResponse(code = 0, msg = "生成AI讲解文案失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 更新商品排序编号
     */
    suspend fun updateProductSort(
        token: String,
        id: Int,
        sort: Int
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "UpdateProductSort"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-update-sort"
            val postData = mapOf(
                "id" to id,
                "sort" to sort
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
            ServerResponse(code = 0, msg = "更新商品排序失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 生成/更新商品卡AI讲品语音
     */
    suspend fun generateAiTts(
        token: String,
        id: Int,
        aiDescription: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "GenerateAiTts"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/product-ai-tts"
            val postData = mapOf(
                "id" to id,
                "ai_description" to aiDescription
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
            ServerResponse(code = 0, msg = "生成AI讲品语音失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 解析商品项数据
     */
    private fun parseProductItem(itemMap: Map<*, *>?): ProductItem? {
        if (itemMap == null) return null
        return try {
            ProductItem(
                id = (itemMap["id"] as? Number)?.toInt() ?: 0,
                productName = itemMap["product_name"] as? String ?: "",
                price = itemMap["price"] as? String ?: "0.00",
                coverImage = itemMap["cover_image"] as? String,
                description = itemMap["description"] as? String ?: "",
                isOnline = (itemMap["is_online"] as? Number)?.toInt() ?: 0,
                enableAiVoice = (itemMap["enable_ai_voice"] as? Number)?.toInt() ?: 0,
                weight = (itemMap["weight"] as? Number)?.toInt() ?: 1,
                stock = (itemMap["stock"] as? Number)?.toInt() ?: 0,
                aiDescription = itemMap["ai_description"] as? String,
                aiKbsId = (itemMap["ai_kbs_id"] as? Number)?.toInt() ?: 0,
                sort = (itemMap["sort"] as? Number)?.toInt() ?: 1,
                siteId = (itemMap["site_id"] as? Number)?.toInt() ?: 0,
                createTime = itemMap["create_time"] as? String ?: "",
                updateTime = itemMap["update_time"] as? String ?: ""
            )
        } catch (e: Exception) {
            responseHandler.logApiError("ParseProductItem", e)
            null
        }
    }
}
