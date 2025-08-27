package com.danshuang.AiLive.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class LoginRepositoryTest {

    private lateinit var context: Context
    private lateinit var loginRepository: LoginRepository
    private lateinit var testScope: TestScope
    private lateinit var dataStoreFile: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        testScope = TestScope(UnconfinedTestDispatcher())
        
        // 创建测试用的DataStore文件
        dataStoreFile = context.preferencesDataStoreFile("test_login_prefs")
        
        // 创建测试用的LoginRepository
        loginRepository = LoginRepository(context)
    }

    @After
    fun teardown() {
        // 清理测试文件
        if (dataStoreFile.exists()) {
            dataStoreFile.delete()
        }
    }

    @Test
    fun testSaveAndRetrieveLoginInfo() = testScope.runTest {
        // 测试数据
        val token = "test_token_123"
        val phone = "13800138000"
        val expiresTime = 1234567890uL

        // 保存登录信息
        loginRepository.saveLoginInfo(token, phone, expiresTime)

        // 验证保存的数据
        assertEquals(token, loginRepository.authToken.first())
        assertEquals(phone, loginRepository.userPhone.first())
        assertEquals(expiresTime.toLong(), loginRepository.expiresTime.first())
    }

    @Test
    fun testSaveAndRetrieveLoggedInUser() = testScope.runTest {
        // 创建测试用户
        val userProfile = UserProfile(
            nickname = "测试用户",
            headimg = "http://example.com/avatar.jpg",
            balance = "100.00",
            point = "200",
            ailiveExpireTime = 1735632000L // 2024-12-31 的时间戳
        )
        
        val loggedInUser = LoggedInUser(
            token = "test_token_456",
            mobile = "13900139000",
            expiresTime = 9876543210uL,
            userProfile = userProfile
        )

        // 保存用户信息
        loginRepository.saveLoggedInUser(loggedInUser)

        // 验证保存的用户信息
        val retrievedUser = loginRepository.loggedInUser.first()
        assertNotNull(retrievedUser)
        assertEquals(loggedInUser.token, retrievedUser?.token)
        assertEquals(loggedInUser.mobile, retrievedUser?.mobile)
        assertEquals(loggedInUser.expiresTime, retrievedUser?.expiresTime)
        assertEquals(loggedInUser.userProfile.nickname, retrievedUser?.userProfile?.nickname)
        assertEquals(loggedInUser.userProfile.headimg, retrievedUser?.userProfile?.headimg)
        assertEquals(loggedInUser.userProfile.balance, retrievedUser?.userProfile?.balance)
        assertEquals(loggedInUser.userProfile.point, retrievedUser?.userProfile?.point)
    }

    @Test
    fun testUpdateUserProfile() = testScope.runTest {
        // 先保存一个基础用户
        val initialUser = LoggedInUser(
            token = "test_token",
            mobile = "13800138000",
            expiresTime = 1234567890uL,
            userProfile = UserProfile()
        )
        loginRepository.saveLoggedInUser(initialUser)

        // 更新用户资料
        val updatedProfile = UserProfile(
            nickname = "更新后的用户名",
            headimg = "http://example.com/new_avatar.jpg",
            balance = "200.00",
            point = "300",
            ailiveExpireTime = 1735632000L
        )
        loginRepository.updateUserProfile(updatedProfile)

        // 验证更新结果
        val retrievedProfile = loginRepository.userProfile.first()
        assertEquals(updatedProfile.nickname, retrievedProfile.nickname)
        assertEquals(updatedProfile.headimg, retrievedProfile.headimg)
        assertEquals(updatedProfile.balance, retrievedProfile.balance)
        assertEquals(updatedProfile.point, retrievedProfile.point)
    }

    @Test
    fun testClearLoginInfo() = testScope.runTest {
        // 先保存一些数据
        val userProfile = UserProfile("测试用户", "avatar.jpg", "100", "200", 1735632000L)
        val loggedInUser = LoggedInUser("token", "13800138000", 1234567890uL, userProfile)
        loginRepository.saveLoggedInUser(loggedInUser)

        // 验证数据已保存
        assertNotNull(loginRepository.loggedInUser.first())
        assertTrue(loginRepository.isLoggedInFlow().first())

        // 清除登录信息
        loginRepository.clearLoginInfo()

        // 验证数据已清除
        assertNull(loginRepository.authToken.first())
        assertNull(loginRepository.userPhone.first())
        assertNull(loginRepository.expiresTime.first())
        assertNull(loginRepository.loggedInUser.first())
        assertFalse(loginRepository.isLoggedInFlow().first())
    }

    @Test
    fun testIsLoggedInFlow() = testScope.runTest {
        // 初始状态应该是未登录
        assertFalse(loginRepository.isLoggedInFlow().first())

        // 保存有效的登录信息（未来的过期时间）
        val futureExpiresTime = (System.currentTimeMillis() / 1000 + 3600).toULong() // 1小时后过期
        loginRepository.saveLoginInfo("token", "13800138000", futureExpiresTime)

        // 应该显示已登录
        assertTrue(loginRepository.isLoggedInFlow().first())

        // 保存已过期的登录信息
        val pastExpiresTime = (System.currentTimeMillis() / 1000 - 3600).toULong() // 1小时前已过期
        loginRepository.saveLoginInfo("token", "13800138000", pastExpiresTime)

        // 应该显示未登录（因为token已过期）
        assertFalse(loginRepository.isLoggedInFlow().first())
    }

    @Test
    fun testIsTokenValidFlow() = testScope.runTest {
        // 保存有效的token（未来过期时间）
        val futureExpiresTime = (System.currentTimeMillis() / 1000 + 3600).toULong()
        loginRepository.saveLoginInfo("token", "13800138000", futureExpiresTime)

        // token应该是有效的
        assertTrue(loginRepository.isTokenValidFlow().first())

        // 保存已过期的token
        val pastExpiresTime = (System.currentTimeMillis() / 1000 - 3600).toULong()
        loginRepository.saveLoginInfo("token", "13800138000", pastExpiresTime)

        // token应该是无效的
        assertFalse(loginRepository.isTokenValidFlow().first())
    }

    @Test
    fun testUserProfileDisplayBalance() = testScope.runTest {
        // 测试显示余额格式
        val profile1 = UserProfile(balance = "100.50", point = "200")
        assertEquals("￥100.50 / 200", profile1.displayBalance)

        // 测试空值情况
        val profile2 = UserProfile(balance = "", point = "200")
        assertEquals("", profile2.displayBalance)

        val profile3 = UserProfile(balance = "100.50", point = "")
        assertEquals("", profile3.displayBalance)
    }
}