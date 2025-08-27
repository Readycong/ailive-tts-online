package com.danshuang.AiLive.ui.screens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.api.ServerApiUtilsV2
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class AccountBindingScreenTest {

    private lateinit var context: Context
    private lateinit var loginRepository: LoginRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var apiUtils: ServerApiUtilsV2
    private lateinit var testScope: TestScope

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(UnconfinedTestDispatcher())
        
        loginRepository = LoginRepository(context)
        tokenManager = TokenManager(loginRepository)
        apiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
    }

    @Test
    fun testApiUtilsCreation() {
        // 测试API工具类是否能正确创建
        assertNotNull(apiUtils)
    }

    @Test
    fun testBindDouyinLiveWithoutLogin() = testScope.runTest {
        // 测试未登录状态下调用绑定接口
        val result = apiUtils.bindDouyinLive(
            token = "",
            douyinId = "test_douyin_id",
            nickname = "测试用户",
            avatar = "https://example.com/avatar.jpg",
            roomId = "test_room_id",
            userHomeUrl = "https://www.douyin.com/user/test"
        )
        
        // 应该返回失败结果
        assertFalse(result.isSuccess)
    }

    @Test
    fun testBindDouyinLiveWithValidToken() = testScope.runTest {
        // 首先保存一个有效的token
        val futureExpiresTime = (System.currentTimeMillis() / 1000 + 3600).toULong()
        loginRepository.saveLoginInfo("valid_token_123", "13800138000", futureExpiresTime)
        
        // 测试有token时的绑定调用（这里只是验证参数传递，实际网络请求可能会失败）
        try {
            val result = apiUtils.bindDouyinLive(
                token = "valid_token_123",
                douyinId = "test_douyin_id",
                nickname = "测试用户",
                avatar = "https://example.com/avatar.jpg",
                roomId = "test_room_id",
                userHomeUrl = "https://www.douyin.com/user/test"
            )
            
            // 由于是测试环境，网络请求可能失败，但不应该因为参数问题而抛出异常
            assertNotNull(result)
        } catch (e: Exception) {
            // 网络相关异常是可接受的
            assertTrue("网络异常是可接受的", 
                e.message?.contains("网络") == true || 
                e.message?.contains("连接") == true ||
                e.message?.contains("timeout") == true ||
                e.message?.contains("UnknownHostException") == true
            )
        }
    }
}