package com.danshuang.AiLive.api.service

import com.danshuang.AiLive.api.client.BaseApiClient
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.LoginResult
import com.danshuang.AiLive.data.model.LoggedInUser
import com.danshuang.AiLive.data.model.UserProfile
import com.danshuang.AiLive.data.model.ServerResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * 用户认证API服务
 * 负责登录、注销、用户信息相关的API调用
 */
class AuthApiService(
    tokenManager: TokenManager,
    private val baseUrl: String
) : BaseApiClient(tokenManager) {
    
    /**
     * 用户登录
     */
    suspend fun login(username: String, password: String): LoginResult {
        val apiName = "Login"
        return try {
            val url = "${baseUrl}/login?username=$username&password=$password&mobile=&mobile_code=&mobile_key="
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url, includeToken = false).build()
            val response = executeRequest(request)
            
            if (!response.isSuccessful) {
                return LoginResult.Error(IOException("登录失败: HTTP ${response.code}"))
            }
            
            val responseBody = response.body?.string()
            val jsonObject = com.google.gson.Gson().fromJson(responseBody, Map::class.java)
            val dataMap = jsonObject["data"] as? Map<*, *>
                ?: return LoginResult.Error(IOException("登录响应数据格式错误"))
            
            // 构建登录用户信息
            val loggedInUser = LoggedInUser(
                token = dataMap["token"] as String,
                mobile = dataMap["mobile"] as String,
                expiresTime = (dataMap["expires_time"] as Double).toUInt(),
                userProfile = UserProfile() // 初始为空，后续通过getMemberInfo填充
            )
            
            LoginResult.Success(loggedInUser)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            LoginResult.Error(e)
        }
    }
    
    /**
     * 获取用户详细信息
     */
    suspend fun getMemberInfo(token: String): ServerResponse<UserProfile> = withContext(Dispatchers.IO) {
        val apiName = "GetMemberInfo"
        return@withContext try {
            val url = "${baseUrl}/member/member"
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequest(request)
            val serverResponse = responseHandler.handleResponse<Map<String, Any>>(response, apiName)
            
            if (serverResponse.isSuccess && serverResponse.data != null) {
                val data = serverResponse.data
                val userProfile = UserProfile(
                    nickname = data["nickname"] as? String ?: "",
                    headimg = data["headimg"] as? String ?: "",
                    balance = data["balance"] as? String ?: "",
                    point = data["point"] as? String ?: ""
                )
                ServerResponse(
                    code = serverResponse.code,
                    msg = serverResponse.msg,
                    data = userProfile
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
            ServerResponse(code = 0, msg = "获取用户信息失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 用户注销
     */
    suspend fun logout(token: String): ServerResponse<Any> = withContext(Dispatchers.IO) {
        val apiName = "Logout"
        return@withContext try {
            val url = "${baseUrl}/auth/logout"
            responseHandler.logApiCall(apiName, url, "GET")
            
            val request = createGetRequest(url)
                .addTokenHeader(token)
                .build()
            
            val response = executeRequest(request)
            responseHandler.handleSimpleResponse(response, apiName)
            
        } catch (e: Exception) {
            responseHandler.logApiError(apiName, e)
            ServerResponse(code = 0, msg = "注销失败: ${e.message}", data = null)
        }
    }
    
    /**
     * 组合登录和获取用户信息的完整登录流程
     */
    suspend fun loginWithUserInfo(username: String, password: String): LoginResult {
        return when (val loginResult = login(username, password)) {
            is LoginResult.Success -> {
                // 获取用户详细信息
                val memberInfo = getMemberInfo(loginResult.user.token)
                if (memberInfo.isSuccess && memberInfo.data != null) {
                    // 更新用户信息
                    val updatedUser = loginResult.user.copy(userProfile = memberInfo.data)
                    LoginResult.Success(updatedUser)
                } else {
                    // 即使获取用户信息失败，也返回登录成功（使用空的用户信息）
                    loginResult
                }
            }
            is LoginResult.Error -> loginResult
            is LoginResult.Loading -> loginResult
        }
    }
}