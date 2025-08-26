package com.danshuang.AiLive.api.service

import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.LoginResult
import com.danshuang.AiLive.data.model.ServerResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@ExperimentalCoroutinesApi
class AuthApiServiceTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var mockTokenManager: TokenManager
    private lateinit var authApiService: AuthApiService
    private lateinit var baseUrl: String

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        baseUrl = mockWebServer.url("/").toString().removeSuffix("/")
        
        mockTokenManager = mock(TokenManager::class.java)
        authApiService = AuthApiService(mockTokenManager, baseUrl)
    }

    @After
    fun tearDown() {
        mockWebServer.shutdown()
    }

    @Test
    fun testLoginSuccess() = runTest {
        // 模拟成功的登录响应
        val loginResponse = """{
            "code": 1,
            "msg": "登录成功",
            "data": {
                "token": "test_token_123",
                "mobile": "13800138000",
                "expires_time": 1234567890
            }
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(loginResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行登录
        val result = authApiService.login("testuser", "testpass")

        // 验证结果
        assertTrue(result is LoginResult.Success)
        val successResult = result as LoginResult.Success
        assertEquals("test_token_123", successResult.user.token)
        assertEquals("13800138000", successResult.user.mobile)
        assertEquals(1234567890uL, successResult.user.expiresTime)

        // 验证请求
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("/login"))
        assertTrue(request.path!!.contains("username=testuser"))
        assertTrue(request.path!!.contains("password=testpass"))
    }

    @Test
    fun testLoginHttpError() = runTest {
        // 模拟HTTP错误响应
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"))

        // 执行登录
        val result = authApiService.login("testuser", "testpass")

        // 验证结果
        assertTrue(result is LoginResult.Error)
        val errorResult = result as LoginResult.Error
        assertTrue(errorResult.exception.message!!.contains("登录失败: HTTP 500"))
    }

    @Test
    fun testLoginInvalidResponseFormat() = runTest {
        // 模拟无效的响应格式
        val invalidResponse = """{
            "code": 1,
            "msg": "登录成功"
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(invalidResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行登录
        val result = authApiService.login("testuser", "testpass")

        // 验证结果
        assertTrue(result is LoginResult.Error)
        val errorResult = result as LoginResult.Error
        assertTrue(errorResult.exception.message!!.contains("登录响应数据格式错误"))
    }

    @Test
    fun testGetMemberInfoSuccess() = runTest {
        // 模拟成功的用户信息响应
        val memberInfoResponse = """{
            "code": 1,
            "msg": "获取成功",
            "data": {
                "nickname": "测试用户",
                "headimg": "http://example.com/avatar.jpg",
                "balance": "100.50",
                "point": "200"
            }
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(memberInfoResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行获取用户信息
        val result = authApiService.getMemberInfo("test_token")

        // 验证结果
        assertTrue(result.isSuccess)
        assertNotNull(result.data)
        assertEquals("测试用户", result.data!!.nickname)
        assertEquals("http://example.com/avatar.jpg", result.data!!.headimg)
        assertEquals("100.50", result.data!!.balance)
        assertEquals("200", result.data!!.point)

        // 验证请求
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("/member/member"))
        assertEquals("test_token", request.getHeader("token"))
    }

    @Test
    fun testGetMemberInfoBusinessError() = runTest {
        // 模拟业务逻辑错误
        val errorResponse = """{
            "code": 0,
            "msg": "用户不存在",
            "data": null
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(errorResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行获取用户信息
        val result = authApiService.getMemberInfo("invalid_token")

        // 验证结果
        assertFalse(result.isSuccess)
        assertEquals(0, result.code)
        assertEquals("用户不存在", result.msg)
        assertNull(result.data)
    }

    @Test
    fun testLogoutSuccess() = runTest {
        // 模拟成功的注销响应
        val logoutResponse = """{
            "code": 1,
            "msg": "注销成功",
            "data": null
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(logoutResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行注销
        val result = authApiService.logout("test_token")

        // 验证结果
        assertTrue(result.isSuccess)
        assertEquals("注销成功", result.msg)

        // 验证请求
        val request = mockWebServer.takeRequest()
        assertTrue(request.path!!.contains("/auth/logout"))
        assertEquals("test_token", request.getHeader("token"))
    }

    @Test
    fun testLoginWithUserInfoSuccess() = runTest {
        // 模拟成功的登录响应
        val loginResponse = """{
            "code": 1,
            "msg": "登录成功",
            "data": {
                "token": "test_token_123",
                "mobile": "13800138000",
                "expires_time": 1234567890
            }
        }"""
        
        // 模拟成功的用户信息响应
        val memberInfoResponse = """{
            "code": 1,
            "msg": "获取成功",
            "data": {
                "nickname": "测试用户",
                "headimg": "http://example.com/avatar.jpg",
                "balance": "100.50",
                "point": "200"
            }
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(loginResponse)
            .setHeader("Content-Type", "application/json"))
            
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(memberInfoResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行完整登录流程
        val result = authApiService.loginWithUserInfo("testuser", "testpass")

        // 验证结果
        assertTrue(result is LoginResult.Success)
        val successResult = result as LoginResult.Success
        assertEquals("test_token_123", successResult.user.token)
        assertEquals("13800138000", successResult.user.mobile)
        assertEquals("测试用户", successResult.user.userProfile.nickname)
        assertEquals("http://example.com/avatar.jpg", successResult.user.userProfile.headimg)
        assertEquals("100.50", successResult.user.userProfile.balance)
        assertEquals("200", successResult.user.userProfile.point)

        // 验证两个请求都被发送
        assertEquals(2, mockWebServer.requestCount)
    }

    @Test
    fun testLoginWithUserInfoMemberInfoFailed() = runTest {
        // 模拟成功的登录响应
        val loginResponse = """{
            "code": 1,
            "msg": "登录成功",
            "data": {
                "token": "test_token_123",
                "mobile": "13800138000",
                "expires_time": 1234567890
            }
        }"""
        
        // 模拟失败的用户信息响应
        val memberInfoErrorResponse = """{
            "code": 0,
            "msg": "获取用户信息失败",
            "data": null
        }"""
        
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(loginResponse)
            .setHeader("Content-Type", "application/json"))
            
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(memberInfoErrorResponse)
            .setHeader("Content-Type", "application/json"))

        // 执行完整登录流程
        val result = authApiService.loginWithUserInfo("testuser", "testpass")

        // 验证结果：即使获取用户信息失败，登录仍然成功，但用户资料为空
        assertTrue(result is LoginResult.Success)
        val successResult = result as LoginResult.Success
        assertEquals("test_token_123", successResult.user.token)
        assertEquals("", successResult.user.userProfile.nickname) // 应该为空的默认值
    }
}