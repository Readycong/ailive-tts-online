package com.danshuang.AiLive.websocket

import com.danshuang.AiLive.data.model.ConnectionState
import kotlinx.coroutines.*
import okhttp3.*
import java.util.concurrent.TimeUnit

/**
 * WebSocket管理器
 * 负责WebSocket连接的建立、维护、消息监听和自动重连
 */
class WebSocketManager {
    
    private var webSocket: WebSocket? = null
    private var currentUrl: String? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectBaseDelay = 5000L // 5秒基础延迟
    private var messageListener: ((String) -> Unit)? = null
    private var stateCallback: ((ConnectionState) -> Unit)? = null
    private var isManualDisconnect = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "WebSocketManager"
        private const val NORMAL_CLOSURE_STATUS = 1000
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 0L // 保持长连接
        private const val WRITE_TIMEOUT = 10L
    }
    
    /**
     * 连接WebSocket
     * @param url WebSocket服务器地址
     * @param stateCallback 连接状态回调
     */
    fun connect(url: String, stateCallback: (ConnectionState) -> Unit) {
        this.currentUrl = url
        this.stateCallback = stateCallback
        this.isManualDisconnect = false
        
        try {
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "AiLive/1.0")
                .addHeader("Origin", "https://live.douyin.com")
                .build()
            
            connectWithRequest(request, stateCallback)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create WebSocket connection", e)
            stateCallback(ConnectionState.ERROR)
            handleConnectionFailure(e)
        }
    }
    
    /**
     * 使用自定义请求连接WebSocket
     * @param request 自定义的请求对象
     * @param stateCallback 连接状态回调
     */
    fun connectWithRequest(request: Request, stateCallback: (ConnectionState) -> Unit) {
        this.currentUrl = request.url.toString()
        this.stateCallback = stateCallback
        this.isManualDisconnect = false
        
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()
            
            stateCallback(ConnectionState.CONNECTING)
            
            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    reconnectAttempts = 0
                    stateCallback(ConnectionState.CONNECTED)
                    android.util.Log.d(TAG, "WebSocket connected to: ${request.url}")
                }
                
                override fun onMessage(webSocket: WebSocket, text: String) {
                    android.util.Log.d(TAG, "Received message: $text")
                    messageListener?.invoke(text)
                }
                
                override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                    // 处理二进制消息（如果需要）
                    android.util.Log.d(TAG, "Received binary message: ${bytes.hex()}")
                }
                
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    android.util.Log.e(TAG, "WebSocket connection failed", t)
                    handleConnectionFailure(t)
                }
                
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    android.util.Log.d(TAG, "WebSocket closed: code=$code, reason=$reason")
                    if (!isManualDisconnect) {
                        // 非手动断开，尝试重连
                        handleConnectionFailure(Exception("Connection closed unexpectedly"))
                    } else {
                        stateCallback(ConnectionState.DISCONNECTED)
                    }
                }
            })
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create WebSocket connection", e)
            stateCallback(ConnectionState.ERROR)
            handleConnectionFailure(e)
        }
    }
    
    /**
     * 设置消息监听器
     * @param listener 消息回调函数
     */
    fun setMessageListener(listener: (String) -> Unit) {
        messageListener = listener
    }
    
    /**
     * 发送消息
     * @param message 要发送的消息
     * @return 是否发送成功
     */
    fun sendMessage(message: String): Boolean {
        return try {
            webSocket?.send(message) ?: false
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send message", e)
            false
        }
    }
    
    /**
     * 手动断开连接
     */
    fun disconnect() {
        isManualDisconnect = true
        webSocket?.close(NORMAL_CLOSURE_STATUS, "Service stopped")
        webSocket = null
        currentUrl = null
        scope.cancel()
    }
    
    /**
     * 检查连接状态
     */
    fun isConnected(): Boolean {
        return webSocket != null
    }
    
    /**
     * 获取当前连接的URL
     */
    fun getCurrentUrl(): String? {
        return currentUrl
    }
    
    /**
     * 处理连接失败
     */
    private fun handleConnectionFailure(error: Throwable) {
        if (isManualDisconnect) {
            return
        }
        
        if (reconnectAttempts < maxReconnectAttempts) {
            scheduleReconnect(error)
        } else {
            android.util.Log.e(TAG, "Max reconnect attempts reached, giving up")
            stateCallback?.invoke(ConnectionState.ERROR)
        }
    }
    
    /**
     * 安排重连
     */
    private fun scheduleReconnect(error: Throwable) {
        reconnectAttempts++
        val delay = calculateReconnectDelay()
        
        android.util.Log.i(TAG, "Scheduling reconnect attempt $reconnectAttempts/$maxReconnectAttempts in ${delay}ms")
        
        scope.launch {
            try {
                delay(delay)
                
                if (!isManualDisconnect && currentUrl != null) {
                    android.util.Log.i(TAG, "Attempting to reconnect (attempt $reconnectAttempts)")
                    currentUrl?.let { url ->
                        stateCallback?.let { callback ->
                            connect(url, callback)
                        }
                    }
                }
            } catch (e: CancellationException) {
                android.util.Log.d(TAG, "Reconnect cancelled")
            }
        }
    }
    
    /**
     * 计算重连延迟（指数退避算法）
     */
    private fun calculateReconnectDelay(): Long {
        return minOf(
            reconnectBaseDelay * (1L shl minOf(reconnectAttempts - 1, 6)), // 最大64倍延迟
            60000L // 最大60秒
        )
    }
    
    /**
     * 重置重连状态
     */
    fun resetReconnectState() {
        reconnectAttempts = 0
    }
    
    /**
     * 获取重连尝试次数
     */
    fun getReconnectAttempts(): Int {
        return reconnectAttempts
    }
    
    /**
     * 发送心跳包
     */
    fun sendHeartbeat() {
        val heartbeatMessage = """{"type":"heartbeat","timestamp":${System.currentTimeMillis()}}"""
        if (!sendMessage(heartbeatMessage)) {
            android.util.Log.w(TAG, "Failed to send heartbeat")
        }
    }
}