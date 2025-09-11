package com.danshuang.AiLive.api.service

import android.content.Context
import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.ConfigItem
import com.danshuang.AiLive.data.model.GreetingConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File


/**
 * 配置管理和文件上传API服务
 * 负责系统配置和文件上传相关的API调用
 */
class ConfigApiService(
    tokenManager: TokenManager,
    private val baseUrl: String,
    context: Context
) : BaseApiClient(tokenManager, context) {
    
    /**
     * 获取配置项
     */
    suspend fun getConfig(
        token: String,
        douyinId: String,
        keyName: String
    ): ServerResponse<ConfigItem> = withContext(Dispatchers.IO) {
        val apiName = "GetConfig"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/config"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "key" to keyName
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<String>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val configItem = ConfigItem(
                    key = keyName,
                    value = serverResponse.data,
                    updateTime = System.currentTimeMillis()
                )
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = configItem
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
            ServerResponse(code = 0, msg = "获取配置失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 更新配置项
     */
    suspend fun updateConfig(
        token: String,
        douyinId: String,
        keyName: String,
        value: String
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "UpdateConfig"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/update-config"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "key" to keyName,
                "value" to value
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
            ServerResponse(code = 0, msg = "更新配置失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 获取招呼设置配置
     */
    suspend fun getGreetingConfig(
        token: String,
        douyinId: String
    ): ServerResponse<GreetingConfig> = withContext(Dispatchers.IO) {
        val apiName = "GetGreetingConfig"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/get-greeting-config"
            val postData = mapOf(
                "douyin_id" to douyinId
            )
            
            responseHandler.logApiCall(apiName, url, "POST", postData)
            
            val requestBody = createJsonRequestBody(postData)
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val data = serverResponse.data
                val greetingConfig = GreetingConfig(
                    greetStatus = (data["greet_status"] as? Number)?.toInt() ?: 0,
                    maxOnline = (data["max_online"] as? Number)?.toInt() ?: 0,
                    delaySeconds = (data["delay_seconds"] as? Number)?.toInt() ?: 0
                )
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = greetingConfig
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
            ServerResponse(code = 0, msg = "获取招呼设置失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 提交招呼设置配置
     */
    suspend fun updateGreetingConfig(
        token: String,
        douyinId: String,
        greetStatus: Int,
        maxOnline: Int,
        delaySeconds: Int
    ): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "UpdateGreetingConfig"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/update-greeting-config"
            val postData = mapOf(
                "douyin_id" to douyinId,
                "greet_status" to greetStatus,
                "max_online" to maxOnline,
                "delay_seconds" to delaySeconds
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
            ServerResponse(code = 0, msg = "更新招呼设置失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 上传音频文件
     */
    suspend fun uploadAudio(
        token: String,
        filePath: String
    ): ServerResponse<Boolean> = withContext(Dispatchers.IO) {
        val apiName = "UploadAudio"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/upload-audio"
            val file = File(filePath)
            
            if (!file.exists()) {
                return@withContext ServerResponse<Boolean>(
                    code = 0,
                    msg = "文件不存在: $filePath",
                    data = false
                )
            }
            
            responseHandler.logApiCall(apiName, url, "POST", mapOf(
                "file_path" to filePath,
                "file_size" to file.length()
            ))
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "audio",
                    file.name,
                    file.asRequestBody("audio/wav".toMediaTypeOrNull())
                )
                .build()
            
            val request = createPostRequest(url, requestBody)
                .addTokenHeader(token)
                .removeHeader("Content-Type") // MultipartBody会自动设置Content-Type
                .build()
            
            val response = executeRequestAndHandleResponse(request, apiName)
            val serverResponse = responseHandler.handleResponse<Boolean>(response, apiName, context)
            
            // 如果服务器返回成功但data为null，我们将其设置为true
            if (serverResponse.isSuccess && serverResponse.data == null) {
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = true
                )
            } else {
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = serverResponse.data ?: false
                )
            }
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "文件上传失败: ${e.message}", data = false)
        }
    }

    /**
     * 上传音频文件
     */
    suspend fun getMyAudio(
        token: String
    ): ServerResponse<Map<String, Any>> = withContext(Dispatchers.IO) {
        val apiName = "GetMyAudio"
        return@withContext try {
            val url = "${baseUrl}/dscrm/dylive-android/get-clone-audio"

            responseHandler.logApiCall(apiName, url, "GET", null)

            val request = createGetRequest(url, true)
                .addTokenHeader(token)
                .removeHeader("Content-Type") // MultipartBody会自动设置Content-Type
                .build()

            val response = executeRequestAndHandleResponse(request, apiName)
            responseHandler.handleResponse<Map<String, Any>>(response, apiName, context)

        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "获取克隆信息失败: ${e.message}", data = null)
        }
    }

    /**
     * 批量获取配置项
     */
    suspend fun getBatchConfigs(
        token: String,
        douyinId: String,
        keys: List<String>
    ): Map<String, ConfigItem> {
        val results = mutableMapOf<String, ConfigItem>()
        
        keys.forEach { key ->
            val response = getConfig(token, douyinId, key)
            if (response.isSuccess && response.data != null) {
                results[key] = response.data
            }
        }
        
        return results
    }
    
    /**
     * 批量更新配置项
     */
    suspend fun updateBatchConfigs(
        token: String,
        douyinId: String,
        configs: Map<String, String>
    ): List<ServerResponse<Any>> {
        return configs.map { (key, value) ->
            updateConfig(token, douyinId, key, value)
        }
    }
    
    /**
     * 验证文件格式和大小
     */
    private fun validateAudioFile(filePath: String): String? {
        val file = File(filePath)
        
        if (!file.exists()) {
            return "文件不存在"
        }
        
        if (file.length() == 0L) {
            return "文件为空"
        }
        
        // 检查文件大小（例如限制在50MB以内）
        val maxSizeMB = 50
        if (file.length() > maxSizeMB * 1024 * 1024) {
            return "文件过大，请选择小于${maxSizeMB}MB的文件"
        }
        return null
        // 文件格式不检查，交给服务器处理

        // 检查文件扩展名
        val allowedExtensions = listOf("wav", "mp3", "m4a", "aac")
        val extension = file.extension.lowercase()
        if (!allowedExtensions.contains(extension)) {
            return "不支持的文件格式，请选择: ${allowedExtensions.joinToString(", ")}"
        }
        
        return null // 验证通过
    }

    /**
     * 安全的文件上传（带验证）
     */
    suspend fun uploadAudioWithValidation(
        token: String,
        filePath: String
    ): ServerResponse<Boolean> {
        val validationError = validateAudioFile(filePath)
        if (validationError != null) {
            return ServerResponse(code = 0, msg = validationError, data = false)
        }

        return uploadAudio(token, filePath)
    }
}