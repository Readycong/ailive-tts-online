package com.danshuang.AiLive.api

import com.danshuang.AiLive.api.service.AuthApiService
import com.danshuang.AiLive.api.service.DouyinApiService
import com.danshuang.AiLive.api.service.KnowledgeApiService
import com.danshuang.AiLive.api.service.ConfigApiService
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.model.LoginResult
import com.danshuang.AiLive.data.model.ServerResponse
import com.danshuang.AiLive.data.model.LoggedInUser
import com.danshuang.AiLive.data.model.DouyinAccount
import com.danshuang.AiLive.data.model.KnowledgeItem
import com.danshuang.AiLive.data.model.AiComment
import com.danshuang.AiLive.data.model.ConfigItem
import kotlinx.coroutines.flow.first

/**
 * 重构后的ServerApiUtils类
 * 作为门面类，组合各个专门的API服务，提供统一的接口
 * 同时集成用户数据持久化管理
 */
class ServerApiUtilsV2(
    private val authApiService: AuthApiService,
    private val douyinApiService: DouyinApiService,
    private val knowledgeApiService: KnowledgeApiService,
    private val configApiService: ConfigApiService,
    private val tokenManager: TokenManager,
    private val loginRepository: LoginRepository,
    private val baseUrl: String
) {
    
    // ============== 用户认证相关 ==============
    
    /**
     * 用户登录（集成持久化）
     */
    suspend fun login(username: String, password: String): LoginResult {
        return when (val result = authApiService.loginWithUserInfo(username, password)) {
            is LoginResult.Success -> {
                // 保存登录信息到本地存储
                loginRepository.saveLoggedInUser(result.user)
                result
            }
            else -> result
        }
    }
    
    /**
     * 用户注销（清除持久化数据）
     */
    suspend fun logout(): ServerResponse<Any> {
        val token = tokenManager.currentToken.first()
        val result = if (token != null) {
            authApiService.logout(token)
        } else {
            ServerResponse<Any>(code = 1, msg = "本地注销成功", data = null)
        }
        
        // 无论服务器注销是否成功，都清除本地数据
        loginRepository.clearLoginInfo()
        tokenManager.clearToken()
        
        return result
    }
    
    /**
     * 获取当前登录用户信息
     */
    suspend fun getCurrentUser(): LoggedInUser? {
        return loginRepository.loggedInUser.first()
    }
    
    /**
     * 刷新用户信息
     */
    suspend fun refreshUserInfo(): ServerResponse<LoggedInUser> {
        val currentUser = getCurrentUser()
        val token = currentUser?.token
        
        return if (token != null) {
            val memberInfoResult = authApiService.getMemberInfo(token)
            if (memberInfoResult.isSuccess && memberInfoResult.data != null) {
                val updatedUser = currentUser.copy(userProfile = memberInfoResult.data)
                loginRepository.saveLoggedInUser(updatedUser)
                ServerResponse(code = 1, msg = "用户信息刷新成功", data = updatedUser)
            } else {
                ServerResponse(code = 0, msg = "刷新用户信息失败: ${memberInfoResult.msg}", data = null)
            }
        } else {
            ServerResponse(code = 0, msg = "用户未登录", data = null)
        }
    }
    
    // ============== 抖音相关 ==============
    
    /**
     * 绑定抖音直播账号
     */
    suspend fun bindDouyinLive(
        douyinId: String,
        nickname: String,
        avatar: String,
        roomId: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return douyinApiService.bindDouyinLive(token, douyinId, nickname, avatar, roomId)
    }
    
    /**
     * 获取绑定的账号列表
     */
    suspend fun listAccounts(): ServerResponse<List<DouyinAccount>> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = emptyList())
        return douyinApiService.listAccounts(token)
    }
    
    /**
     * 获取AI知识库信息
     */
    suspend fun fetchAiKnowledges(
        douyinId: String,
        roomId: String
    ): Map<String, Any>? {
        val token = getTokenOrThrow() ?: return null
        return douyinApiService.fetchAiKnowledges(token, douyinId, roomId)
    }
    
    /**
     * 获取AI评论
     */
    suspend fun fetchAiComments(
        douyinId: String,
        roomId: String,
        step: String
    ): List<AiComment>? {
        val token = getTokenOrThrow() ?: return null
        return douyinApiService.fetchAiComments(token, douyinId, roomId, step)
    }
    
    /**
     * 添加主播评论
     */
    suspend fun addHostComment(
        douyinId: String,
        roomId: String,
        type: String,
        content: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return douyinApiService.addHostComment(token, douyinId, roomId, type, content)
    }
    
    /**
     * 上报在线用户信息
     */
    suspend fun onlineUsers(
        douyinId: String,
        roomId: String,
        users: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return douyinApiService.reportOnlineUsers(token, douyinId, roomId, users)
    }
    
    // ============== 知识库管理 ==============
    
    /**
     * 获取知识库条目
     */
    suspend fun fetchKbItems(douyinId: String): ServerResponse<List<KnowledgeItem>> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = emptyList())
        return knowledgeApiService.fetchKbItems(token, douyinId)
    }
    
    /**
     * 更新知识库条目
     */
    suspend fun updateKbItem(
        douyinId: String,
        typeId: String,
        id: String,
        question: String,
        answer: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return knowledgeApiService.updateKbItem(token, douyinId, typeId, id, question, answer)
    }
    
    /**
     * 删除知识库条目
     */
    suspend fun deleteKbItem(
        douyinId: String,
        id: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return knowledgeApiService.deleteKbItem(token, douyinId, id)
    }
    
    // ============== 配置管理 ==============
    
    /**
     * 获取配置项
     */
    suspend fun getConfig(
        douyinId: String,
        keyName: String
    ): ServerResponse<ConfigItem> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = null)
        return configApiService.getConfig(token, douyinId, keyName)
    }
    
    /**
     * 更新配置项
     */
    suspend fun updateConfig(
        douyinId: String,
        keyName: String,
        value: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return configApiService.updateConfig(token, douyinId, keyName, value)
    }
    
    /**
     * 上传音频文件
     */
    suspend fun uploadAudio(
        douyinId: String,
        filePath: String
    ): ServerResponse<Map<String, Any>> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = null)
        return configApiService.uploadAudioWithValidation(token, douyinId, filePath)
    }
    
    // ============== 工具方法 ==============
    
    /**
     * 获取当前token，如果未登录返回null
     */
    private suspend fun getTokenOrThrow(): String? {
        return tokenManager.currentToken.first()
    }
    
    /**
     * 创建未登录错误响应
     */
    private fun <T> createNotLoggedInResponse(): ServerResponse<T> {
        return ServerResponse(code = 0, msg = "用户未登录，请先登录", data = null)
    }
    
    /**
     * 检查用户是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return tokenManager.isLoggedIn.first()
    }
    
    /**
     * 检查token是否有效
     */
    suspend fun isTokenValid(): Boolean {
        return tokenManager.isTokenValid.first()
    }
    
    /**
     * 获取token剩余有效时间（秒）
     */
    suspend fun getTokenRemainingTime(): Long {
        return tokenManager.getTokenRemainingTime().first()
    }
    
    companion object {
        /**
         * 创建实例的工厂方法（用于非依赖注入的场景）
         */
        fun create(
            baseUrl: String,
            tokenManager: TokenManager,
            loginRepository: LoginRepository
        ): ServerApiUtilsV2 {
            val authApiService = AuthApiService(tokenManager, baseUrl)
            val douyinApiService = DouyinApiService(tokenManager, baseUrl)
            val knowledgeApiService = KnowledgeApiService(tokenManager, baseUrl)
            val configApiService = ConfigApiService(tokenManager, baseUrl)
            
            return ServerApiUtilsV2(
                authApiService = authApiService,
                douyinApiService = douyinApiService,
                knowledgeApiService = knowledgeApiService,
                configApiService = configApiService,
                tokenManager = tokenManager,
                loginRepository = loginRepository,
                baseUrl = baseUrl
            )
        }
        
        /**
         * 从 Context 中读取 API URL 并创建实例
         */
        fun createFromContext(
            context: android.content.Context,
            tokenManager: TokenManager,
            loginRepository: LoginRepository
        ): ServerApiUtilsV2 {
            val baseUrl = context.getString(com.danshuang.AiLive.R.string.api_base_url)
            return create(baseUrl, tokenManager, loginRepository)
        }
    }
}