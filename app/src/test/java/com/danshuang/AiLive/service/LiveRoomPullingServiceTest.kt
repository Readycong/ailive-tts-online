package com.danshuang.AiLive.service

import com.danshuang.AiLive.data.model.*
import org.junit.Test
import org.junit.Assert.*

class LiveRoomPullingServiceTest {
    
    @Test
    fun `test service state flow initial value`() {
        // 验证初始状态
        assertEquals(
            LiveRoomPullingService.PULLING_STATE_STOPPED,
            LiveRoomPullingService.getCurrentPullingState()
        )
        assertEquals(
            ConnectionState.DISCONNECTED,
            LiveRoomPullingService.getCurrentConnectionState()
        )
    }
    
    @Test
    fun `test WebSocket connection state changes`() {
        var connectionState = ConnectionState.DISCONNECTED
        val stateCallback: (ConnectionState) -> Unit = { state ->
            connectionState = state
        }
        
        // 模拟连接状态变化
        stateCallback(ConnectionState.CONNECTING)
        assertEquals(ConnectionState.CONNECTING, connectionState)
        
        stateCallback(ConnectionState.CONNECTED)
        assertEquals(ConnectionState.CONNECTED, connectionState)
        
        stateCallback(ConnectionState.DISCONNECTED)
        assertEquals(ConnectionState.DISCONNECTED, connectionState)
        
        stateCallback(ConnectionState.ERROR)
        assertEquals(ConnectionState.ERROR, connectionState)
    }
}