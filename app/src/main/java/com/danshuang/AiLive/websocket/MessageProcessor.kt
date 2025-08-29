package com.danshuang.AiLive.websocket

import com.danshuang.AiLive.data.model.*
import com.danshuang.AiLive.data.repository.LiveRoomRepository
import com.danshuang.AiLive.ui.screens.LiveMessageManager
import com.danshuang.AiLive.ui.screens.MessageType
import kotlinx.coroutines.*
import kotlinx.serialization.json.*
import kotlinx.serialization.SerializationException

/**
 * 消息处理器
 * 负责解析WebSocket消息并处理不同类型的直播间事件
 */
class MessageProcessor(
    private val liveRoomRepository: LiveRoomRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
) {
    
    companion object {
        private const val TAG = "MessageProcessor"
        
        // 消息类型常量
        private const val MSG_TYPE_LIVE_START = "live_start"
        private const val MSG_TYPE_LIVE_END = "live_end"
        private const val MSG_TYPE_VIEWER_UPDATE = "viewer_update"
        private const val MSG_TYPE_ROOM_INFO = "room_info"
        private const val MSG_TYPE_HEARTBEAT = "heartbeat"
        private const val MSG_TYPE_COMMENT = "comment"
        private const val MSG_TYPE_GIFT = "gift"
        private const val MSG_TYPE_FOLLOW = "follow"
        private const val MSG_TYPE_ENTER = "enter"
        private const val MSG_TYPE_SYSTEM = "system"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }
    
    /**
     * 处理WebSocket消息
     * @param rawMessage 原始消息字符串
     * @return 处理结果
     */
    suspend fun processMessage(rawMessage: String): ProcessResult {
        return try {
            logDebug("Processing message: $rawMessage")
            
            val messageData = parseWebSocketMessage(rawMessage)
            
            when (messageData.type) {
                MSG_TYPE_LIVE_START -> handleLiveStart(messageData)
                MSG_TYPE_LIVE_END -> handleLiveEnd(messageData)
                MSG_TYPE_VIEWER_UPDATE -> handleViewerUpdate(messageData)
                MSG_TYPE_ROOM_INFO -> handleRoomInfo(messageData)
                MSG_TYPE_HEARTBEAT -> handleHeartbeat(messageData)
                MSG_TYPE_COMMENT -> handleComment(messageData)
                MSG_TYPE_GIFT -> handleGift(messageData)
                MSG_TYPE_FOLLOW -> handleFollow(messageData)
                MSG_TYPE_ENTER -> handleEnter(messageData)
                MSG_TYPE_SYSTEM -> handleSystem(messageData)
                else -> ProcessResult.Ignored("Unknown message type: ${messageData.type}")
            }
        } catch (e: SerializationException) {
            logError("Failed to parse message as JSON: $rawMessage", e)
            ProcessResult.Error("Invalid JSON format: ${e.message}")
        } catch (e: Exception) {
            logError("Failed to process message: $rawMessage", e)
            ProcessResult.Error("Failed to process message: ${e.message}")
        }
    }
    
    // 日志方法，避免直接使用Android Log类
    private fun logDebug(message: String) {
        try {
            android.util.Log.d(TAG, message)
        } catch (e: Exception) {
            // 在测试环境中忽略
        }
    }
    
    private fun logInfo(message: String) {
        try {
            android.util.Log.i(TAG, message)
        } catch (e: Exception) {
            // 在测试环境中忽略
        }
    }
    
    private fun logError(message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                android.util.Log.e(TAG, message, throwable)
            } else {
                android.util.Log.e(TAG, message)
            }
        } catch (e: Exception) {
            // 在测试环境中忽略
        }
    }
    
    /**
     * 解析WebSocket消息
     */
    private fun parseWebSocketMessage(rawMessage: String): MessageData {
        val jsonElement = json.parseToJsonElement(rawMessage)
        val jsonObject = jsonElement.jsonObject
        
        val type = jsonObject["type"]?.jsonPrimitive?.content ?: "unknown"
        val timestamp = jsonObject["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        
        // 解析data字段为Map
        val data = jsonObject["data"]?.jsonObject?.let { dataObject ->
            val resultMap = mutableMapOf<String, Any>()
            for ((key, value) in dataObject) {
                when {
                    value.jsonPrimitive.isString -> resultMap[key] = value.jsonPrimitive.content
                    value.jsonPrimitive.longOrNull != null -> resultMap[key] = value.jsonPrimitive.long
                    value.jsonPrimitive.intOrNull != null -> resultMap[key] = value.jsonPrimitive.int
                    value.jsonPrimitive.booleanOrNull != null -> resultMap[key] = value.jsonPrimitive.boolean
                    value.jsonPrimitive.doubleOrNull != null -> resultMap[key] = value.jsonPrimitive.double
                    else -> resultMap[key] = value.toString()
                }
            }
            resultMap.toMap()
        }
        
        return MessageData(type, data, timestamp)
    }
    
    /**
     * 处理开播事件
     */
    private suspend fun handleLiveStart(messageData: MessageData): ProcessResult {
        return try {
            val roomId = extractRoomId(messageData) ?: return ProcessResult.Error("Missing room ID")
            
            logInfo("Processing live start event for room: $roomId")
            
            // 获取直播间详细信息
            val roomInfo = liveRoomRepository.fetchLiveRoomDetails(roomId)
            
            // 保存到本地数据库
            liveRoomRepository.saveLiveRoomInfo(roomInfo)
            
            // 插入系统消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.SYSTEM,
                    content = "${roomInfo.hostName} 开始直播：${roomInfo.title}",
                    userName = null
                )
            }
            
            ProcessResult.Success("Live room started: $roomId")
        } catch (e: Exception) {
            logError("Failed to handle live start event", e)
            ProcessResult.Error("Failed to handle live start: ${e.message}")
        }
    }
    
    /**
     * 处理下播事件
     */
    private suspend fun handleLiveEnd(messageData: MessageData): ProcessResult {
        return try {
            val roomId = extractRoomId(messageData) ?: return ProcessResult.Error("Missing room ID")
            
            logInfo("Processing live end event for room: $roomId")
            
            // 更新直播间状态
            liveRoomRepository.updateRoomStatus(roomId, RoomStatus.OFFLINE)
            
            // 获取主播名称
            val hostName = extractHostName(messageData) ?: "主播"
            
            // 插入系统消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.SYSTEM,
                    content = "$hostName 结束了直播",
                    userName = null
                )
            }
            
            ProcessResult.Success("Live room ended: $roomId")
        } catch (e: Exception) {
            logError("Failed to handle live end event", e)
            ProcessResult.Error("Failed to handle live end: ${e.message}")
        }
    }
    
    /**
     * 处理观众数更新事件
     */
    private suspend fun handleViewerUpdate(messageData: MessageData): ProcessResult {
        return try {
            val roomId = extractRoomId(messageData) ?: return ProcessResult.Error("Missing room ID")
            val viewerCount = extractViewerCount(messageData) ?: return ProcessResult.Error("Missing viewer count")
            
            logDebug("Processing viewer update for room $roomId: $viewerCount viewers")
            
            // 更新观众数
            liveRoomRepository.updateViewerCount(roomId, viewerCount)
            
            // 插入系统消息到UI（限制频率，避免消息过多）
            if (shouldShowViewerUpdate(viewerCount)) {
                withContext(Dispatchers.Main) {
                    LiveMessageManager.insertMessage(
                        type = MessageType.SYSTEM,
                        content = "直播间观众数更新：$viewerCount 人在线",
                        userName = null
                    )
                }
            }
            
            ProcessResult.Success("Viewer count updated: $viewerCount")
        } catch (e: Exception) {
            logError("Failed to handle viewer update", e)
            ProcessResult.Error("Failed to handle viewer update: ${e.message}")
        }
    }
    
    /**
     * 处理直播间信息事件
     */
    private suspend fun handleRoomInfo(messageData: MessageData): ProcessResult {
        return try {
            val roomId = extractRoomId(messageData) ?: return ProcessResult.Error("Missing room ID")
            
            logDebug("Processing room info update for room: $roomId")
            
            // 获取并更新直播间信息
            val roomInfo = liveRoomRepository.fetchLiveRoomDetails(roomId)
            liveRoomRepository.saveLiveRoomInfo(roomInfo)
            
            ProcessResult.Success("Room info updated: $roomId")
        } catch (e: Exception) {
            logError("Failed to handle room info update", e)
            ProcessResult.Error("Failed to handle room info: ${e.message}")
        }
    }
    
    /**
     * 处理心跳消息
     */
    private fun handleHeartbeat(messageData: MessageData): ProcessResult {
        logDebug("Received heartbeat at ${messageData.timestamp}")
        return ProcessResult.Success("Heartbeat received")
    }
    
    /**
     * 处理评论消息
     */
    private suspend fun handleComment(messageData: MessageData): ProcessResult {
        return try {
            val userName = extractUserName(messageData) ?: "匿名用户"
            val content = extractContent(messageData) ?: return ProcessResult.Error("Missing comment content")
            
            logDebug("Processing comment from $userName: $content")
            
            // 插入评论消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.COMMENT,
                    content = content,
                    userName = userName
                )
            }
            
            ProcessResult.Success("Comment processed from $userName")
        } catch (e: Exception) {
            logError("Failed to handle comment", e)
            ProcessResult.Error("Failed to handle comment: ${e.message}")
        }
    }
    
    /**
     * 处理礼物消息
     */
    private suspend fun handleGift(messageData: MessageData): ProcessResult {
        return try {
            val userName = extractUserName(messageData) ?: "匿名用户"
            val giftName = extractGiftName(messageData) ?: "礼物"
            val giftCount = extractGiftCount(messageData) ?: 1
            
            val content = if (giftCount > 1) {
                "送出了 $giftCount 个$giftName"
            } else {
                "送出了$giftName"
            }
            
            logDebug("Processing gift from $userName: $content")
            
            // 插入礼物消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.GIFT,
                    content = content,
                    userName = userName
                )
            }
            
            ProcessResult.Success("Gift processed from $userName")
        } catch (e: Exception) {
            logError("Failed to handle gift", e)
            ProcessResult.Error("Failed to handle gift: ${e.message}")
        }
    }
    
    /**
     * 处理关注消息
     */
    private suspend fun handleFollow(messageData: MessageData): ProcessResult {
        return try {
            val userName = extractUserName(messageData) ?: "匿名用户"
            
            logDebug("Processing follow from $userName")
            
            // 插入关注消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.FOLLOW,
                    content = "关注了主播",
                    userName = userName
                )
            }
            
            ProcessResult.Success("Follow processed from $userName")
        } catch (e: Exception) {
            logError("Failed to handle follow", e)
            ProcessResult.Error("Failed to handle follow: ${e.message}")
        }
    }
    
    /**
     * 处理进入直播间消息
     */
    private suspend fun handleEnter(messageData: MessageData): ProcessResult {
        return try {
            val userName = extractUserName(messageData) ?: "匿名用户"
            
            logDebug("Processing enter from $userName")
            
            // 插入进入消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.ENTER,
                    content = "进入了直播间",
                    userName = userName
                )
            }
            
            ProcessResult.Success("Enter processed from $userName")
        } catch (e: Exception) {
            logError("Failed to handle enter", e)
            ProcessResult.Error("Failed to handle enter: ${e.message}")
        }
    }
    
    /**
     * 处理系统消息
     */
    private suspend fun handleSystem(messageData: MessageData): ProcessResult {
        return try {
            val content = extractContent(messageData) ?: return ProcessResult.Error("Missing system message content")
            
            logDebug("Processing system message: $content")
            
            // 插入系统消息到UI
            withContext(Dispatchers.Main) {
                LiveMessageManager.insertMessage(
                    type = MessageType.SYSTEM,
                    content = content,
                    userName = null
                )
            }
            
            ProcessResult.Success("System message processed")
        } catch (e: Exception) {
            logError("Failed to handle system message", e)
            ProcessResult.Error("Failed to handle system message: ${e.message}")
        }
    }
    
    // 提取字段的辅助方法
    private fun extractRoomId(messageData: MessageData): String? {
        return messageData.data?.get("roomId") as? String
    }
    
    private fun extractHostName(messageData: MessageData): String? {
        return messageData.data?.get("hostName") as? String
    }
    
    private fun extractViewerCount(messageData: MessageData): Int? {
        return when (val count = messageData.data?.get("viewerCount")) {
            is Int -> count
            is Long -> count.toInt()
            is String -> count.toIntOrNull()
            else -> null
        }
    }
    
    private fun extractUserName(messageData: MessageData): String? {
        return messageData.data?.get("userName") as? String
    }
    
    private fun extractContent(messageData: MessageData): String? {
        return messageData.data?.get("content") as? String
    }
    
    private fun extractGiftName(messageData: MessageData): String? {
        return messageData.data?.get("giftName") as? String
    }
    
    private fun extractGiftCount(messageData: MessageData): Int? {
        return when (val count = messageData.data?.get("giftCount")) {
            is Int -> count
            is Long -> count.toInt()
            is String -> count.toIntOrNull()
            else -> null
        }
    }
    
    /**
     * 判断是否应该显示观众数更新消息
     * 避免过于频繁的更新消息
     */
    private fun shouldShowViewerUpdate(viewerCount: Int): Boolean {
        // 只在特殊时刻显示：整百、整千等
        return viewerCount % 100 == 0 || viewerCount % 500 == 0
    }
    
    /**
     * 清理资源
     */
    fun cleanup() {
        scope.cancel()
    }
}