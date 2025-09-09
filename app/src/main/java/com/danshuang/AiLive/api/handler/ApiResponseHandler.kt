package com.danshuang.AiLive.api.handler

import android.util.Log
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.util.GlobalEventManager
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
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
    inline fun <reified T> handleResponse(
        response: Response,
        apiName: String = "Unknown"
    ): ServerResponse<T> {
        return try {
            if (!response.isSuccessful) {
                Log.w(tag, "$apiName: HTTP ${response.code} - ${response.message}")
                return createErrorResponse("HTTP错误: ${response.code} ${response.message}")
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                Log.w(tag, "$apiName: Empty response body")
                return createErrorResponse("服务器返回空响应")
            }
            
            Log.d(tag, "$apiName Response: $responseBody")
            
            // 解析JSON响应
            val jsonMap = gson.fromJson(responseBody, Map::class.java)
            val code = (jsonMap["code"] as? Double)?.toInt() ?: 0
            val msg = jsonMap["msg"] as? String ?: "未知错误"
            val data = jsonMap["data"]
            
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
                            return createErrorResponse("数据解析失败: ${e.message}")
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
    fun handleSimpleResponse(
        response: Response,
        apiName: String = "Unknown"
    ): ServerResponse<Any> {
        return handleResponse<Any>(response, apiName)
    }
    
    /**
     * 处理返回布尔值的简单接口
     */
    fun handleBooleanResponse(
        response: Response,
        apiName: String = "Unknown"
    ): Boolean {
        val result = handleSimpleResponse(response, apiName)
        return result.isSuccess
    }
    
    /**
     * 处理返回Map类型的响应
     */
    fun handleMapResponse(
        response: Response,
        apiName: String = "Unknown"
    ): Map<String, Any>? {
        val result = handleResponse<Map<String, Any>>(response, apiName)
        return if (result.isSuccess) result.data else null
    }
    
    /**
     * 处理返回List类型的响应
     */
    fun handleListResponse(
        response: Response,
        apiName: String = "Unknown"
    ): List<Any>? {
        val result = handleResponse<List<Any>>(response, apiName)
        return if (result.isSuccess) result.data else null
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