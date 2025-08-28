package com.danshuang.AiLive.data.manager

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.LoggedInUser
import com.danshuang.AiLive.data.model.UserProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class TokenManagerTest {

    private lateinit var context: Context
    private lateinit var loginRepository: LoginRepository
    private lateinit var tokenManager: TokenManager
    private lateinit var testScope: TestScope
    private lateinit var dataStoreFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(UnconfinedTestDispatcher())
        
        // 创建测试用的DataStore文件
        dataStoreFile = context.preferencesDataStoreFile("test_token_manager_prefs")
        
        loginRepository = LoginRepository(context)
        tokenManager = TokenManager(loginRepository)
    }

    @After
    fun teardown() {
        // 清理测试文件
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    @Test
    fun testCurrentTokenFlow() = testScope.runTest {
        // 初始状态应该没有token
        assertNull(tokenManager.currentToken.first())

        // 保存token
        val testToken = "test_token_123"
        loginRepository.saveLoginInfo(testToken, "13800138000", 1234567890uL)

        // 验证token可以获取
        assertEquals(testToken, tokenManager.currentToken.first())
    }

    @Test
    fun testIsTokenValid() = testScope.runTest {
        // 初始状态token应该无效
        assertFalse(tokenManager.isTokenValid.first())

        // 保存有效token（未来过期时间）
        val futureExpiresTime = (System.currentTimeMillis() / 1000 + 3600).toULong()
        loginRepository.saveLoginInfo("valid_token", "13800138000", futureExpiresTime)

        // token应该有效
        assertTrue(tokenManager.isTokenValid.first())

        // 保存已过期token
        val pastExpiresTime = (System.currentTimeMillis() / 1000 - 3600).toULong()
        loginRepository.saveLoginInfo("expired_token", "13800138000", pastExpiresTime)

        // token应该无效
        assertFalse(tokenManager.isTokenValid.first())
    }

    @Test
    fun testIsLoggedIn() = testScope.runTest {
        // 初始状态应该未登录
        assertFalse(tokenManager.isLoggedIn.first())

        // 保存完整的登录信息
        val futureExpiresTime = (System.currentTimeMillis() / 1000 + 3600).toULong()
        val user = LoggedInUser(
            token = "test_token",
            mobile = "13800138000",
            expiresTime = futureExpiresTime,
            userProfile = UserProfile()
        )
        loginRepository.saveLoggedInUser(user)

        // 应该显示已登录
        assertTrue(tokenManager.isLoggedIn.first())
    }

    @Test
    fun testTokenExpiresTime() = testScope.runTest {
        // 初始状态应该没有过期时间
        assertNull(tokenManager.tokenExpiresTime.first())

        // 保存过期时间
        val expiresTime = 1234567890L
        loginRepository.saveLoginInfo("token", "phone", expiresTime.toULong())

        // 验证过期时间
        assertEquals(expiresTime, tokenManager.tokenExpiresTime.first())
    }

    @Test
    fun testIsTokenExpiringSoon() = testScope.runTest {
        // 设置30分钟后过期的token
        val expiresIn30Minutes = System.currentTimeMillis() / 1000 + 30 * 60
        loginRepository.saveLoginInfo("token", "phone", expiresIn30Minutes.toULong())

        // 检查是否即将过期（阈值为60分钟）
        assertTrue(tokenManager.isTokenExpiringSoon(60).first())
        
        // 检查是否即将过期（阈值为15分钟）
        assertFalse(tokenManager.isTokenExpiringSoon(15).first())

        // 设置2小时后过期的token
        val expiresIn2Hours = System.currentTimeMillis() / 1000 + 2 * 60 * 60
        loginRepository.saveLoginInfo("token", "phone", expiresIn2Hours.toULong())

        // 应该不会即将过期
        assertFalse(tokenManager.isTokenExpiringSoon(30).first())
    }

    @Test
    fun testGetTokenRemainingTime() = testScope.runTest {
        // 没有token时剩余时间应该为0
        assertEquals(0L, tokenManager.getTokenRemainingTime().first())

        // 设置1小时后过期的token
        val expiresIn1Hour = System.currentTimeMillis() / 1000 + 3600
        loginRepository.saveLoginInfo("token", "phone", expiresIn1Hour.toULong())

        // 剩余时间应该约为3600秒（允许一些误差）
        val remainingTime = tokenManager.getTokenRemainingTime().first()
        assertTrue("剩余时间应该在3590-3600秒之间", remainingTime in 3590..3600)

        // 设置已过期的token
        val expiredTime = System.currentTimeMillis() / 1000 - 3600
        loginRepository.saveLoginInfo("token", "phone", expiredTime.toULong())

        // 剩余时间应该为0
        assertEquals(0L, tokenManager.getTokenRemainingTime().first())
    }

    @Test
    fun testClearToken() = testScope.runTest {
        // 先保存一些数据
        val user = LoggedInUser(
            token = "test_token",
            mobile = "13800138000",
            expiresTime = 1234567890uL,
            userProfile = UserProfile("测试用户", "avatar.jpg", "100", "200", 1735632000L)
        )
        loginRepository.saveLoggedInUser(user)

        // 验证数据已保存
        assertNotNull(tokenManager.currentToken.first())
        assertTrue(tokenManager.isLoggedIn.first())

        // 清除token
        tokenManager.clearToken()

        // 验证数据已清除
        assertNull(tokenManager.currentToken.first())
        assertFalse(tokenManager.isLoggedIn.first())
        assertFalse(tokenManager.isTokenValid.first())
    }

    @Test
    fun testIsTokenFormatValid() {
        // 测试有效格式
        assertTrue(tokenManager.isTokenFormatValid("valid_token_123456"))
        assertTrue(tokenManager.isTokenFormatValid("abcdefghijklmnop"))

        // 测试无效格式
        assertFalse(tokenManager.isTokenFormatValid(null))
        assertFalse(tokenManager.isTokenFormatValid(""))
        assertFalse(tokenManager.isTokenFormatValid("   "))
        assertFalse(tokenManager.isTokenFormatValid("short"))
        assertFalse(tokenManager.isTokenFormatValid("abc"))
    }

    @Test
    fun testSaveToken() = testScope.runTest {
        val token = "new_test_token"
        val expiresTime = 9876543210uL

        // 保存token
        tokenManager.saveToken(token, expiresTime)

        // 验证token已保存
        assertEquals(token, tokenManager.currentToken.first())
        assertEquals(expiresTime.toLong(), tokenManager.tokenExpiresTime.first())
    }

    private fun Context.preferencesDataStoreFile(name: String): File {
        return File(this.filesDir, "datastore/$name.preferences_pb")
    }
}