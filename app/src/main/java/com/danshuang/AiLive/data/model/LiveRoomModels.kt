package com.danshuang.AiLive.data.model

import androidx.annotation.Keep

/**
 * 直播间信息数据类
 */
@Keep
data class LiveRoomInfo(
    val roomId: String,
    val hostName: String,
    val title: String,
    val viewerCount: Int,
    val startTime: Long,
    val status: RoomStatus,
    val thumbnailUrl: String,
    val platform: String = "douyin",
    val createTime: Long = System.currentTimeMillis(),
    val updateTime: Long = System.currentTimeMillis()
)

/**
 * 直播间状态枚举
 */
enum class RoomStatus {
    LIVE,       // 直播中
    OFFLINE,    // 已下播
    PREPARING   // 准备中
}

/**
 * WebSocket配置数据类
 */
@Keep
data class WebSocketConfig(
    val configId: String = "default",
    val websocketUrl: String,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

/**
 * 服务状态数据类
 */
@Keep
data class ServiceState(
    val serviceId: String = "live_room_pulling",
    val pullingState: Int,
    val lastWebSocketUrl: String?,
    val lastConnectTime: Long?,
    val lastDisconnectTime: Long?,
    val errorMessage: String?
)

/**
 * WebSocket消息数据类
 */
@Keep
data class WebSocketMessage(
    val type: String,
    val data: Any?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * WebSocket连接状态枚举
 */
enum class ConnectionState {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    ERROR
}

/**
 * 消息处理结果
 */
sealed class ProcessResult {
    data class Success(val message: String) : ProcessResult()
    data class Error(val error: String) : ProcessResult()
    data class Ignored(val reason: String) : ProcessResult()
}

/**
 * 监控日志数据类
 */
@Keep
data class MonitoringLog(
    val logId: String,
    val roomId: String,
    val eventType: String,
    val eventData: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 解析后的WebSocket消息数据
 */
@Keep
data class MessageData(
    val type: String,
    val data: Map<String, Any>?,
    val timestamp: Long
)

/**
 * 直播间详细信息API响应
 */
@Keep
data class LiveRoomDetailsResponse(
    val roomId: String,
    val hostInfo: HostInfo,
    val roomInfo: RoomInfo,
    val streamInfo: StreamInfo?
)

/**
 * 主播信息
 */
@Keep
data class HostInfo(
    val hostId: String,
    val nickname: String,
    val avatar: String,
    val followerCount: Int
)

/**
 * 房间信息
 */
@Keep
data class RoomInfo(
    val title: String,
    val status: Int, // 0: 未开播, 1: 直播中, 2: 已结束
    val viewerCount: Int,
    val startTime: Long?,
    val coverUrl: String
)

/**
 * 流信息
 */
@Keep
data class StreamInfo(
    val streamUrl: String,
    val quality: String,
    val bitrate: Int
)