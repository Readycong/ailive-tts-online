package com.danshuang.AiLive.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.danshuang.AiLive.data.model.*
import com.danshuang.AiLive.api.service.LiveRoomApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import android.util.Log

private val Context.liveRoomDataStore by preferencesDataStore(name = "live_room_prefs")

/**
 * 直播间数据仓库
 * 负责管理直播间信息的本地存储和API调用
 */
class LiveRoomRepository(
    private val context: Context,
    private val apiService: LiveRoomApiService
) {
    
    companion object {
        private const val TAG = "LiveRoomRepository"
        
        // 存储键常量
        private const val WEBSOCKET_CONFIG_PREFIX = "websocket_config_"
        private const val SERVICE_STATE_PREFIX = "service_state_"
        private const val LIVE_ROOM_PREFIX = "live_room_"
        private const val MONITORING_LOG_PREFIX = "monitoring_log_"
    }
    
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    
    // 基础配置键
    private val WEBSOCKET_URL_KEY = stringPreferencesKey("websocket_url")
    private val WEBSOCKET_UPDATE_TIME_KEY = longPreferencesKey("websocket_update_time")
    private val SERVICE_PULLING_STATE_KEY = intPreferencesKey("service_pulling_state")
    private val SERVICE_LAST_URL_KEY = stringPreferencesKey("service_last_url")
    private val SERVICE_LAST_CONNECT_KEY = longPreferencesKey("service_last_connect")
    private val SERVICE_LAST_DISCONNECT_KEY = longPreferencesKey("service_last_disconnect")
    private val SERVICE_ERROR_MESSAGE_KEY = stringPreferencesKey("service_error_message")
    
    /**
     * 获取WebSocket配置
     */
    suspend fun fetchWebSocketConfig(): WebSocketConfig {
        return try {
            // 首先尝试从API获取最新配置
            val apiConfig = apiService.getWebSocketConfig()
            
            // 保存到本地
            saveWebSocketConfig(apiConfig)
            
            Log.i(TAG, "Fetched WebSocket config from API: ${apiConfig.websocketUrl}")
            apiConfig
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch WebSocket config from API, using local cache", e)
            
            // API失败时使用本地缓存
            getLocalWebSocketConfig() ?: run {
                // 如果本地也没有，返回默认配置
                Log.w(TAG, "No local WebSocket config found, using default")
                WebSocketConfig(
                    websocketUrl = "wss://example.com/live/websocket",
                    isActive = false
                )
            }
        }
    }
    
    /**
     * 保存WebSocket配置到本地
     */
    private suspend fun saveWebSocketConfig(config: WebSocketConfig) {
        context.liveRoomDataStore.edit { preferences ->
            preferences[WEBSOCKET_URL_KEY] = config.websocketUrl
            preferences[WEBSOCKET_UPDATE_TIME_KEY] = config.lastUpdateTime
        }
    }
    
    /**
     * 获取本地WebSocket配置
     */
    private suspend fun getLocalWebSocketConfig(): WebSocketConfig? {
        val preferences = context.liveRoomDataStore.data.first()
        
        val url = preferences[WEBSOCKET_URL_KEY] ?: return null
        val updateTime = preferences[WEBSOCKET_UPDATE_TIME_KEY] ?: 0L
        
        return WebSocketConfig(
            websocketUrl = url,
            lastUpdateTime = updateTime,
            isActive = true
        )
    }
    
    /**
     * 获取直播间详细信息
     */
    suspend fun fetchLiveRoomDetails(roomId: String): LiveRoomInfo {
        return try {
            Log.i(TAG, "Fetching live room details for: $roomId")
            
            // 从API获取详细信息
            val response = apiService.getLiveRoomDetails(roomId)
            
            // 转换为LiveRoomInfo
            val roomInfo = LiveRoomInfo(
                roomId = response.roomId,
                hostName = response.hostInfo.nickname,
                title = response.roomInfo.title,
                viewerCount = response.roomInfo.viewerCount,
                startTime = response.roomInfo.startTime ?: System.currentTimeMillis(),
                status = when (response.roomInfo.status) {
                    1 -> RoomStatus.LIVE
                    2 -> RoomStatus.OFFLINE
                    else -> RoomStatus.PREPARING
                },
                thumbnailUrl = response.roomInfo.coverUrl,
                platform = "douyin"
            )
            
            Log.i(TAG, "Successfully fetched room info: ${roomInfo.hostName} - ${roomInfo.title}")
            roomInfo
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch live room details for $roomId", e)
            
            // 返回默认信息
            LiveRoomInfo(
                roomId = roomId,
                hostName = "未知主播",
                title = "直播间",
                viewerCount = 0,
                startTime = System.currentTimeMillis(),
                status = RoomStatus.PREPARING,
                thumbnailUrl = "",
                platform = "douyin"
            )
        }
    }
    
    /**
     * 保存直播间信息到本地
     */
    suspend fun saveLiveRoomInfo(roomInfo: LiveRoomInfo) {
        try {
            val key = stringPreferencesKey("${LIVE_ROOM_PREFIX}${roomInfo.roomId}")
            val jsonString = json.encodeToString(roomInfo)
            
            context.liveRoomDataStore.edit { preferences ->
                preferences[key] = jsonString
            }
            
            Log.d(TAG, "Saved room info for: ${roomInfo.roomId}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save room info for ${roomInfo.roomId}", e)
        }
    }
    
    /**
     * 获取本地存储的直播间信息
     */
    suspend fun getLocalLiveRoomInfo(roomId: String): LiveRoomInfo? {
        return try {
            val key = stringPreferencesKey("${LIVE_ROOM_PREFIX}$roomId")
            val preferences = context.liveRoomDataStore.data.first()
            val jsonString = preferences[key] ?: return null
            
            json.decodeFromString<LiveRoomInfo>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get local room info for $roomId", e)
            null
        }
    }
    
    /**
     * 获取所有本地直播间信息
     */
    fun getAllLiveRooms(): Flow<List<LiveRoomInfo>> {
        return context.liveRoomDataStore.data.map { preferences ->
            val roomInfoList = mutableListOf<LiveRoomInfo>()
            
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith(LIVE_ROOM_PREFIX) && value is String) {
                    try {
                        val roomInfo = json.decodeFromString<LiveRoomInfo>(value)
                        roomInfoList.add(roomInfo)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse room info for key: ${key.name}", e)
                    }
                }
            }
            
            // 按更新时间排序
            roomInfoList.sortedByDescending { it.updateTime }
        }
    }
    
    /**
     * 更新直播间状态
     */
    suspend fun updateRoomStatus(roomId: String, status: RoomStatus) {
        try {
            val existingRoom = getLocalLiveRoomInfo(roomId)
            if (existingRoom != null) {
                val updatedRoom = existingRoom.copy(
                    status = status,
                    updateTime = System.currentTimeMillis()
                )
                saveLiveRoomInfo(updatedRoom)
                Log.d(TAG, "Updated room status for $roomId: $status")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update room status for $roomId", e)
        }
    }
    
    /**
     * 更新观众数
     */
    suspend fun updateViewerCount(roomId: String, viewerCount: Int) {
        try {
            val existingRoom = getLocalLiveRoomInfo(roomId)
            if (existingRoom != null) {
                val updatedRoom = existingRoom.copy(
                    viewerCount = viewerCount,
                    updateTime = System.currentTimeMillis()
                )
                saveLiveRoomInfo(updatedRoom)
                Log.d(TAG, "Updated viewer count for $roomId: $viewerCount")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update viewer count for $roomId", e)
        }
    }
    
    /**
     * 保存服务状态
     */
    suspend fun saveServiceState(state: ServiceState) {
        context.liveRoomDataStore.edit { preferences ->
            preferences[SERVICE_PULLING_STATE_KEY] = state.pullingState
            state.lastWebSocketUrl?.let { preferences[SERVICE_LAST_URL_KEY] = it }
            state.lastConnectTime?.let { preferences[SERVICE_LAST_CONNECT_KEY] = it }
            state.lastDisconnectTime?.let { preferences[SERVICE_LAST_DISCONNECT_KEY] = it }
            state.errorMessage?.let { preferences[SERVICE_ERROR_MESSAGE_KEY] = it }
        }
    }
    
    /**
     * 获取服务状态
     */
    suspend fun getServiceState(): ServiceState {
        val preferences = context.liveRoomDataStore.data.first()
        
        return ServiceState(
            pullingState = preferences[SERVICE_PULLING_STATE_KEY] ?: 0,
            lastWebSocketUrl = preferences[SERVICE_LAST_URL_KEY],
            lastConnectTime = preferences[SERVICE_LAST_CONNECT_KEY],
            lastDisconnectTime = preferences[SERVICE_LAST_DISCONNECT_KEY],
            errorMessage = preferences[SERVICE_ERROR_MESSAGE_KEY]
        )
    }
    
    /**
     * 保存监控日志
     */
    suspend fun saveMonitoringLog(log: MonitoringLog) {
        try {
            val key = stringPreferencesKey("${MONITORING_LOG_PREFIX}${log.logId}")
            val jsonString = json.encodeToString(log)
            
            context.liveRoomDataStore.edit { preferences ->
                preferences[key] = jsonString
            }
            
            // 清理旧日志（保留最近1000条）
            cleanupOldLogs()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save monitoring log", e)
        }
    }
    
    /**
     * 获取监控日志
     */
    fun getMonitoringLogs(): Flow<List<MonitoringLog>> {
        return context.liveRoomDataStore.data.map { preferences ->
            val logs = mutableListOf<MonitoringLog>()
            
            preferences.asMap().forEach { (key, value) ->
                if (key.name.startsWith(MONITORING_LOG_PREFIX) && value is String) {
                    try {
                        val log = json.decodeFromString<MonitoringLog>(value)
                        logs.add(log)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse monitoring log for key: ${key.name}", e)
                    }
                }
            }
            
            // 按时间戳排序
            logs.sortedByDescending { it.timestamp }
        }
    }
    
    /**
     * 清理旧的监控日志
     */
    private suspend fun cleanupOldLogs() {
        try {
            val logs = getMonitoringLogs().first()
            if (logs.size > 1000) {
                val logsToRemove = logs.drop(1000) // 保留最新的1000条
                
                context.liveRoomDataStore.edit { preferences ->
                    logsToRemove.forEach { log ->
                        val key = stringPreferencesKey("${MONITORING_LOG_PREFIX}${log.logId}")
                        preferences.remove(key)
                    }
                }
                
                Log.d(TAG, "Cleaned up ${logsToRemove.size} old monitoring logs")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup old logs", e)
        }
    }
    
    /**
     * 清除所有数据
     */
    suspend fun clearAllData() {
        context.liveRoomDataStore.edit { preferences ->
            preferences.clear()
        }
        Log.i(TAG, "Cleared all live room data")
    }
}