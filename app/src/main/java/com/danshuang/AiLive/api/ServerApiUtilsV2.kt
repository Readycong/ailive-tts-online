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
import com.danshuang.AiLive.data.model.GreetingConfig
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
    
    /**
     * 修改密码
     */
    suspend fun changePassword(oldPassword: String, newPassword: String): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return authApiService.changePassword(token, oldPassword, newPassword)
    }
    
    // ============== 抖音相关 ==============
    
    /**
     * 绑定抖音直播账号
     */
    suspend fun bindDouyinLive(
        token: String,
        douyinId: String,
        nickname: String,
        avatar: String,
        roomId: String,
        userHomeUrl: String
    ): ServerResponse<Any> {
        return douyinApiService.bindDouyinLive(token, douyinId, nickname, avatar, roomId, userHomeUrl)
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
        roomId: String,
        stage: String
    ): Map<String, Any>? {
        val token = getTokenOrThrow() ?: return null
        return douyinApiService.fetchAiKnowledges(token, roomId, stage)
    }
    
    /**
     * 获取AI评论
     */
    suspend fun fetchAiComments(
        roomId: String,
        step: String
    ): List<AiComment>? {
        val token = getTokenOrThrow() ?: return null
        return douyinApiService.fetchAiComments(token, roomId, step)
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
    suspend fun fetchKbItems(typeId: String): ServerResponse<List<KnowledgeItem>> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = emptyList())
        return knowledgeApiService.fetchKbItems(token, typeId)
    }

    /**
     * 获取单条知识库条目
     */
    suspend fun fetchKbById(typeId: String, kbId: Number): ServerResponse<List<KnowledgeItem>> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = emptyList())
        return knowledgeApiService.fetchKbItems(token, typeId, kbId)
    }

    /**
     * 更新知识库条目
     */
    suspend fun updateKbItem(
        typeId: String,
        id: Number,
        question: String,
        answer: String
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return knowledgeApiService.updateKbItem(token, typeId, id, question, answer)
    }
    
    /**
     * 删除知识库条目
     */
    suspend fun deleteKbItem(
        id: Number
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return knowledgeApiService.deleteKbItem(token, id)
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
    
    /**
     * 获取招呼设置配置
     */
    suspend fun getGreetingConfig(
        douyinId: String
    ): ServerResponse<GreetingConfig> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = null)
        return configApiService.getGreetingConfig(token, douyinId)
    }
    
    /**
     * 提交招呼设置配置
     */
    suspend fun updateGreetingConfig(
        douyinId: String,
        greetStatus: Int,
        maxOnline: Int,
        delaySeconds: Int
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        return configApiService.updateGreetingConfig(token, douyinId, greetStatus, maxOnline, delaySeconds)
    }
    
    // ============== 限制配置管理 ==============
    
    /**
     * 获取限制配置
     * @param restrictionType 限制类型 ("stop_nickname" | "stop_comment")
     * @return 解析后的限制项列表
     */
    suspend fun getRestrictionConfig(
        douyinId: String,
        restrictionType: String
    ): ServerResponse<List<String>> {
        val token = getTokenOrThrow() ?: return ServerResponse(code = 0, msg = "用户未登录", data = emptyList())
        
        return try {
            val response = configApiService.getConfig(token, douyinId, restrictionType)
            if (response.isSuccess && response.data != null) {
                val restrictionItems = parseRestrictionItems(response.data.value)
                ServerResponse(
                    code = response.code,
                    msg = response.msg,
                    data = restrictionItems
                )
            } else {
                // 配置不存在时返回默认配置
                val defaultItems = getDefaultRestrictionItems(restrictionType)
                ServerResponse(
                    code = 1,
                    msg = "使用默认配置",
                    data = defaultItems
                )
            }
        } catch (e: Exception) {
            ServerResponse(
                code = 0,
                msg = "获取限制配置失败: ${e.message}",
                data = getDefaultRestrictionItems(restrictionType)
            )
        }
    }
    
    /**
     * 更新限制配置
     * @param restrictionType 限制类型
     * @param restrictionItems 限制项列表
     * @return 保存结果
     */
    suspend fun updateRestrictionConfig(
        douyinId: String,
        restrictionType: String,
        restrictionItems: List<String>
    ): ServerResponse<Any> {
        val token = getTokenOrThrow() ?: return createNotLoggedInResponse()
        
        return try {
            val configValue = formatRestrictionItems(restrictionItems)
            configApiService.updateConfig(token, douyinId, restrictionType, configValue)
        } catch (e: Exception) {
            ServerResponse(code = 0, msg = "保存限制配置失败: ${e.message}", data = null)
        }
    }
    
    // ============== 工具方法 ==============
    
    /**
     * 解析服务器返回的限制配置字符串
     * 支持逗号、空格、换行符分隔
     */
    private fun parseRestrictionItems(configValue: String): List<String> {
        return configValue
            .split(",", " ", "\n", "\r\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }
    
    /**
     * 将限制项列表转换为服务器格式
     * 使用逗号分隔
     */
    private fun formatRestrictionItems(items: List<String>): String {
        return items
            .filter { it.isNotBlank() }
            .map { it.trim() }
            .distinct()
            .joinToString(",")
    }
    
    /**
     * 获取默认限制配置
     */
    private fun getDefaultRestrictionItems(restrictionType: String): List<String> {
        return when (restrictionType) {
            "stop_nickname" -> listOf(
                "广告号", "机器人", "测试用户", "管理员", "客服号"
            )
            "stop_comment" -> listOf(
                "广告链接", "联系方式", "敏感词汇", "辱骂言论", "垃圾信息"
            )
            else -> emptyList()
        }.sorted()
    }
    
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
            loginRepository: LoginRepository,
            context: android.content.Context
        ): ServerApiUtilsV2 {
            val authApiService = AuthApiService(tokenManager, baseUrl, context)
            val douyinApiService = DouyinApiService(tokenManager, baseUrl, context)
            val knowledgeApiService = KnowledgeApiService(tokenManager, baseUrl, context)
            val configApiService = ConfigApiService(tokenManager, baseUrl, context)
            
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
            return create(baseUrl, tokenManager, loginRepository, context)
        }
    }
}