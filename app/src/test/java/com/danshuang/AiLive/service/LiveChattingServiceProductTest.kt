package com.danshuang.AiLive.service

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danshuang.AiLive.ui.screens.LiveMessageManager
import com.danshuang.AiLive.ui.screens.MessageType
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [28])
class LiveChattingServiceProductTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // 清空消息列表
        LiveMessageManager.clearMessages()
    }

    @Test
    fun testProductListUpdateBroadcast() {
        // 创建商品列表
        val products = listOf(
            ProductInfo(
                productId = "1",
                name = "测试商品1",
                price = 10000L
            ),
            ProductInfo(
                productId = "2",
                name = "测试商品2",
                price = 20000L
            )
        )
        
        // 更新商品列表
        ProductListManager.updateProducts(products)
        
        // 发送广播
        val intent = Intent("com.danshuang.AiLive.PRODUCT_LIST_UPDATED")
        context.sendBroadcast(intent)
        
        // 等待一段时间让广播被处理
        Thread.sleep(100)
        
        // 验证消息是否正确插入
        val messages = LiveMessageManager.messages
        assertEquals(3, messages.size) // 应该有3条消息：商品总数和两个商品
        
        // 验证第一条消息（商品总数）
        val totalCountMessage = messages[0]
        assertEquals(MessageType.SYSTEM, totalCountMessage.type)
        assertEquals("系统", totalCountMessage.userName)
        assertEquals("当前直播间共有 2 件商品", totalCountMessage.content)
        
        // 验证第二条消息（第一个商品）
        val firstProductMessage = messages[1]
        assertEquals(MessageType.SYSTEM, firstProductMessage.type)
        assertEquals("系统", firstProductMessage.userName)
        assertEquals("1号商品: 测试商品1", firstProductMessage.content)
        
        // 验证第三条消息（第二个商品）
        val secondProductMessage = messages[2]
        assertEquals(MessageType.SYSTEM, secondProductMessage.type)
        assertEquals("系统", secondProductMessage.userName)
        assertEquals("2号商品: 测试商品2", secondProductMessage.content)
    }
}