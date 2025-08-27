package com.danshuang.AiLive.api.client

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import com.danshuang.AiLive.api.handler.ApiResponseHandler
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 基础API客户端
 * 提供HTTP请求的基础功能和通用配置
 */
open class BaseApiClient(
    private val tokenManager: TokenManager,
    private val context: Context
) {
    
    protected val client = OkHttpClient.Builder().apply {
        val timeoutSeconds = AppConfig.Api.getTimeout(context)
        connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
        writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
        readTimeout(timeoutSeconds, TimeUnit.SECONDS)
    }.build()
    
    protected val responseHandler = ApiResponseHandler.getInstance()
    
    companion object {
        const val SITE_ID = "100000"
        const val CONTENT_TYPE_JSON = "application/json"
        const val CONTENT_TYPE_FORM = "application/x-www-form-urlencoded"
    }
    
    /**
     * 创建GET请求构建器
     */
    protected fun createGetRequest(
        url: String,
        includeToken: Boolean = true
    ): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .get()
            .addHeader("site-id", SITE_ID)
            .addHeader("Content-Type", CONTENT_TYPE_JSON)
        
        // TODO: 在实际使用中需要通过TokenManager获取token
        // 这里暂时保持原有逻辑，后续集成时需要修改
        return builder
    }
    
    /**
     * 创建POST请求构建器
     */
    protected fun createPostRequest(
        url: String,
        requestBody: RequestBody,
        includeToken: Boolean = true
    ): Request.Builder {
        val builder = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("site-id", SITE_ID)
            .addHeader("Content-Type", CONTENT_TYPE_JSON)
        
        // TODO: 在实际使用中需要通过TokenManager获取token
        // 这里暂时保持原有逻辑，后续集成时需要修改
        return builder
    }
    
    /**
     * 创建JSON请求体
     */
    protected fun createJsonRequestBody(data: Map<String, Any>): RequestBody {
        val json = com.google.gson.Gson().toJson(data)
        return RequestBody.create(
            "$CONTENT_TYPE_JSON; charset=utf-8".toMediaTypeOrNull(),
            json
        )
    }
    
    /**
     * 执行HTTP请求
     */
    protected suspend fun executeRequest(request: Request) = withContext(Dispatchers.IO) {
        client.newCall(request).execute()
    }
    
    /**
     * 添加token到请求头（临时方法，后续需要通过TokenManager处理）
     */
    protected fun Request.Builder.addTokenHeader(token: String?): Request.Builder {
        token?.let { addHeader("token", it) }
        return this
    }
}