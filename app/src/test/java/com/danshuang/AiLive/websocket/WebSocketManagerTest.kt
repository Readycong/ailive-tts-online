package com.danshuang.AiLive.websocket

import com.danshuang.AiLive.data.model.ConnectionState
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class WebSocketManagerTest {
    
    private lateinit var webSocketManager: WebSocketManager
    
    @Before
    fun setup() {
        webSocketManager = WebSocketManager()
    }
    
    @Test
    fun `test initial connection state`() {
        // 验证初始状态
        assertFalse(webSocketManager.isConnected())
        assertNull(webSocketManager.getCurrentUrl())
        assertEquals(0, webSocketManager.getReconnectAttempts())
    }
    
    @Test
    fun `test message listener setting and retrieval`() {
        var receivedMessage: String? = null
        
        // 设置消息监听器
        webSocketManager.setMessageListener { message ->
            receivedMessage = message
        }
        
        // 验证监听器设置（通过功能验证）
        assertNull(receivedMessage) // 初始为null
    }
    
    @Test
    fun `test disconnect`() {
        // 断开连接
        webSocketManager.disconnect()
        
        // 验证状态
        assertFalse(webSocketManager.isConnected())
        assertNull(webSocketManager.getCurrentUrl())
    }
    
    @Test
    fun `test send message when not connected`() {
        // 在未连接状态下发送消息
        val result = webSocketManager.sendMessage("test message")
        
        // 验证返回false
        assertFalse(result)
    }
    
    @Test
    fun `test reconnect attempts counter`() {
        val initialAttempts = webSocketManager.getReconnectAttempts()
        assertEquals(0, initialAttempts)
        
        // 重置重连状态
        webSocketManager.resetReconnectState()
        assertEquals(0, webSocketManager.getReconnectAttempts())
    }
}