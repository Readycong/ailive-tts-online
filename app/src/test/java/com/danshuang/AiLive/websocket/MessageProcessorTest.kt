package com.danshuang.AiLive.websocket

import com.danshuang.AiLive.data.model.*
import com.danshuang.AiLive.data.repository.LiveRoomRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class MessageProcessorTest {
    
    @Mock
    private lateinit var mockRepository: LiveRoomRepository
    
    private lateinit var messageProcessor: MessageProcessor
    
    private val testDispatcher = StandardTestDispatcher()
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        messageProcessor = MessageProcessor(mockRepository)
    }
    
    @Test
    fun `test process heartbeat message`() = runTest(testDispatcher) {
        // 准备测试数据
        val message = """
        {
            "type": "heartbeat",
            "data": {},
            "timestamp": ${System.currentTimeMillis()}
        }
        """.trimIndent()
        
        // 执行测试
        val result = messageProcessor.processMessage(message)
        
        // 验证结果
        assertTrue(result is ProcessResult.Success)
        assertEquals("Heartbeat received", (result as ProcessResult.Success).message)
    }
    
    @Test
    fun `test process unknown message type`() = runTest(testDispatcher) {
        // 准备测试数据
        val message = """
        {
            "type": "unknown_type",
            "data": {},
            "timestamp": ${System.currentTimeMillis()}
        }
        """.trimIndent()
        
        // 执行测试
        val result = messageProcessor.processMessage(message)
        
        // 验证结果
        assertTrue(result is ProcessResult.Ignored)
        assertEquals("Unknown message type: unknown_type", (result as ProcessResult.Ignored).reason)
    }
    
    @Test
    fun `test process invalid JSON message`() = runTest(testDispatcher) {
        // 准备测试数据
        val invalidMessage = "invalid json content"
        
        // 执行测试
        val result = messageProcessor.processMessage(invalidMessage)
        
        // 验证结果
        assertTrue(result is ProcessResult.Error)
        assertTrue((result as ProcessResult.Error).error.contains("Invalid JSON format"))
    }
    
    @Test
    fun `test cleanup`() {
        // 执行清理
        messageProcessor.cleanup()
        
        // 验证清理后状态（这里主要是确保没有异常抛出）
        // 实际使用中，cleanup会取消协程作用域
    }
}