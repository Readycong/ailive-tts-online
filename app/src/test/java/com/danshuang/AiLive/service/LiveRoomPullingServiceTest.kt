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
    
    @Test
    fun `test service constants`() {
        // 验证服务常量
        assertNotNull(LiveRoomPullingService.EXTRA_WEBSOCKET_URL)
        assertNotNull(LiveRoomPullingService.EXTRA_COOKIE_STRING)
        assertNotNull(LiveRoomPullingService.ACTION_START)
        assertNotNull(LiveRoomPullingService.ACTION_STOP)
        assertNotNull(LiveRoomPullingService.ACTION_UPDATE_CONNECTION)
        assertTrue(LiveRoomPullingService.EXTRA_WEBSOCKET_URL.isNotEmpty())
        assertTrue(LiveRoomPullingService.EXTRA_COOKIE_STRING.isNotEmpty())
        assertTrue(LiveRoomPullingService.ACTION_START.isNotEmpty())
        assertTrue(LiveRoomPullingService.ACTION_STOP.isNotEmpty())
        assertTrue(LiveRoomPullingService.ACTION_UPDATE_CONNECTION.isNotEmpty())
    }
    
    @Test
    fun `test service action constants`() {
        // 验证动作常量
        assertEquals("start", LiveRoomPullingService.ACTION_START)
        assertEquals("stop", LiveRoomPullingService.ACTION_STOP)
        assertEquals("update_connection", LiveRoomPullingService.ACTION_UPDATE_CONNECTION)
    }
}