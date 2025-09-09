package com.danshuang.AiLive

import com.danshuang.AiLive.util.UnauthorizedErrorHandler
import org.junit.Test
import org.junit.Assert.*

class UnauthorizedErrorHandlerTest {
    
    @Test
    fun testSingletonInstance() {
        val instance1 = UnauthorizedErrorHandler.getInstance()
        val instance2 = UnauthorizedErrorHandler.getInstance()
        
        assertSame(instance1, instance2)
    }
}