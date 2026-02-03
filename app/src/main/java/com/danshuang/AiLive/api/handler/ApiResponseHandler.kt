package com.danshuang.AiLive.api.handler

import android.content.Context
import android.util.Log
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.util.GlobalEventManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Response
import java.io.IOException

/**
 * API响应处理器
 * 统一处理HTTP响应的解析、错误处理和日志记录
 */
class ApiResponseHandler {
    
    val gson = Gson()
    val tag = "ApiResponse"
    
    /**
     * 处理HTTP响应并解析为ServerResponse
     */
    suspend inline fun <reified T> handleResponse(
        response: Response,
        apiName: String = "Unknown",
        context: Context
    ): ServerResponse<T> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (!response.isSuccessful) {
                Log.w(tag, "$apiName: HTTP ${response.code} - ${response.message}")
                // 检查HTTP状态码是否为401
                if (response.code == 401) {
                    GlobalEventManager.getInstance().sendUnauthorizedErrorEvent(context)
                }
                return@withContext createErrorResponse("HTTP错误: ${response.code} ${response.message}")
            }
            
            val responseBody = try {
                response.body?.string() ?: ""
            } catch (e: IllegalStateException) {
                Log.e(tag, "Failed to read response body", e)
                return@withContext createErrorResponse("读取响应体失败: ${e.message}")
            }

            if (responseBody.isNullOrBlank()) {
                Log.w(tag, "$apiName: Empty response body")
                return@withContext createErrorResponse("服务器返回空响应")
            }
            
            Log.d(tag, "$apiName Response: $responseBody")
            
            // 解析JSON响应
            val jsonMap = gson.fromJson(responseBody, Map::class.java)
            val code = (jsonMap["code"] as? Double)?.toInt() ?: 0
            val msg = jsonMap["msg"] as? String ?: "未知错误"
            val data = jsonMap["data"]
            
            // 检查响应体中的code是否为401
            if (code == 401) {
                GlobalEventManager.getInstance().sendUnauthorizedErrorEvent(context)
            }
            
            // 根据业务逻辑判断是否成功
            if (code == 1 && data != null) {
                val parsedData = when (T::class) {
                    String::class -> data as T
                    Map::class -> data as T
                    List::class, ArrayList::class -> data as T
                    else -> {
                        try {
                            gson.fromJson(gson.toJson(data), T::class.java)
                        } catch (e: Exception) {
                            Log.e(tag, "$apiName: Data parsing failed", e)
                            return@withContext createErrorResponse("数据解析失败: ${e.message}")
                        }
                    }
                }
                ServerResponse(code = code, msg = msg, data = parsedData)
            } else {
                Log.w(tag, "$apiName: Business logic error - code: $code, msg: $msg")
                ServerResponse(code = code, msg = msg, data = null)
            }
            
        } catch (e: JsonSyntaxException) {
            Log.e(tag, "$apiName: JSON parsing error", e)
            createErrorResponse("JSON解析错误: ${e.message}")
        } catch (e: IOException) {
            Log.e(tag, "$apiName: IO error", e)
            createErrorResponse("网络错误: ${e.message}")
        } catch (e: Exception) {
            Log.e(tag, "$apiName: Unexpected error", e)
            createErrorResponse("未知错误: ${e.message}")
        }
    }
    
    /**
     * 处理简单的响应（不需要具体类型转换）
     */
    suspend fun handleSimpleResponse(
        response: Response,
        apiName: String = "Unknown",
        context: Context
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        return@withContext handleResponse<Any>(response, apiName, context)
    }
    
    /**
     * 处理返回布尔值的简单接口
     */
    suspend fun handleBooleanResponse(
        response: Response,
        apiName: String = "Unknown",
        context: Context
    ): Boolean = withContext(Dispatchers.IO) {
        val result = handleSimpleResponse(response, apiName, context)
        return@withContext result.isSuccess
    }
    
    /**
     * 处理返回Map类型的响应
     */
    suspend fun handleMapResponse(
        response: Response,
        apiName: String = "Unknown",
        context: Context
    ): Map<String, Any>? = withContext(Dispatchers.IO) {
        val result = handleResponse<Map<String, Any>>(response, apiName, context)
        return@withContext if (result.isSuccess) result.data else null
    }
    
    /**
     * 处理返回List类型的响应
     */
    suspend fun handleListResponse(
        response: Response,
        apiName: String = "Unknown",
        context: Context
    ): List<Any>? = withContext(Dispatchers.IO) {
        val result = handleResponse<List<Any>>(response, apiName, context)
        return@withContext if (result.isSuccess) result.data else null
    }
    
    /**
     * 创建错误响应
     */
    fun <T> createErrorResponse(errorMsg: String): ServerResponse<T> {
        return ServerResponse(code = 0, msg = errorMsg, data = null)
    }
    
    /**
     * 记录API调用日志
     */
    fun logApiCall(
        apiName: String,
        url: String,
        method: String = "GET",
        params: Map<String, Any>? = null
    ) {
        val paramsStr = params?.let { " with params: $it" } ?: ""
        Log.d(tag, "$apiName: $method $url$paramsStr")
    }
    
    /**
     * 记录API调用错误
     */
    fun logApiError(
        apiName: String,
        error: Throwable,
        additionalInfo: String = ""
    ) {
        Log.e(tag, "$apiName failed$additionalInfo", error)
    }
    
    companion object {
        /**
         * 创建单例实例
         */
        @Volatile
        private var INSTANCE: ApiResponseHandler? = null
        
        fun getInstance(): ApiResponseHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ApiResponseHandler().also { INSTANCE = it }
            }
        }
    }
}