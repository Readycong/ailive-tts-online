package com.danshuang.AiLive.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.core.app.ApplicationProvider
import com.danshuang.AiLive.data.model.BoundAccountInfo
import com.danshuang.AiLive.data.model.PlatformType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LoginRepositoryTest {

    private lateinit var loginRepository: LoginRepository
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        loginRepository = LoginRepository(context)
    }

    @After
    fun tearDown() {
        // 清理测试数据
        runTest {
            loginRepository.clearDouyinAccount()
            loginRepository.clearLkAccount()
        }
    }

    @Test
    fun testSaveAndRetrieveDouyinAccount() = runTest {
        // 创建测试账户信息
        val accountInfo = BoundAccountInfo(
            platform = PlatformType.DOUYIN,
            avatar = "https://example.com/avatar.jpg",
            nickname = "测试抖音用户",
            userId = "douyin_123456",
            homeUrl = "https://www.douyin.com/user/test",
            isLive = false,
            bindTime = System.currentTimeMillis()
        )

        // 保存账户信息
        loginRepository.saveDouyinAccount(accountInfo)

        // 获取账户信息
        val retrievedAccount = loginRepository.getDouyinAccount()

        // 验证信息是否正确保存和检索
        assertNotNull(retrievedAccount)
        assertEquals(PlatformType.DOUYIN, retrievedAccount?.platform)
        assertEquals("https://example.com/avatar.jpg", retrievedAccount?.avatar)
        assertEquals("测试抖音用户", retrievedAccount?.nickname)
        assertEquals("douyin_123456", retrievedAccount?.userId)
        assertEquals("https://www.douyin.com/user/test", retrievedAccount?.homeUrl)
        assertEquals(false, retrievedAccount?.isLive)
    }

    @Test
    fun testSaveAndRetrieveLkAccount() = runTest {
        // 创建测试账户信息
        val accountInfo = BoundAccountInfo(
            platform = PlatformType.LK,
            avatar = "https://example.com/lk_avatar.jpg",
            nickname = "测试来客用户",
            userId = "lk_123456",
            homeUrl = "https://www.laik.live/user/test",
            isLive = true,
            bindTime = System.currentTimeMillis()
        )

        // 保存账户信息
        loginRepository.saveLkAccount(accountInfo)

        // 获取账户信息
        val retrievedAccount = loginRepository.getLkAccount()

        // 验证信息是否正确保存和检索
        assertNotNull(retrievedAccount)
        assertEquals(PlatformType.LK, retrievedAccount?.platform)
        assertEquals("https://example.com/lk_avatar.jpg", retrievedAccount?.avatar)
        assertEquals("测试来客用户", retrievedAccount?.nickname)
        assertEquals("lk_123456", retrievedAccount?.userId)
        assertEquals("https://www.laik.live/user/test", retrievedAccount?.homeUrl)
        assertEquals(true, retrievedAccount?.isLive)
    }

    @Test
    fun testClearDouyinAccount() = runTest {
        // 创建并保存测试账户信息
        val accountInfo = BoundAccountInfo(
            platform = PlatformType.DOUYIN,
            avatar = "https://example.com/avatar.jpg",
            nickname = "测试抖音用户",
            userId = "douyin_123456",
            homeUrl = "https://www.douyin.com/user/test",
            isLive = false,
            bindTime = System.currentTimeMillis()
        )

        loginRepository.saveDouyinAccount(accountInfo)

        // 确保账户信息已保存
        assertNotNull(loginRepository.getDouyinAccount())

        // 清除账户信息
        loginRepository.clearDouyinAccount()

        // 验证账户信息已被清除
        assertNull(loginRepository.getDouyinAccount())
    }

    @Test
    fun testClearLkAccount() = runTest {
        // 创建并保存测试账户信息
        val accountInfo = BoundAccountInfo(
            platform = PlatformType.LK,
            avatar = "https://example.com/lk_avatar.jpg",
            nickname = "测试来客用户",
            userId = "lk_123456",
            homeUrl = "https://www.laik.live/user/test",
            isLive = true,
            bindTime = System.currentTimeMillis()
        )

        loginRepository.saveLkAccount(accountInfo)

        // 确保账户信息已保存
        assertNotNull(loginRepository.getLkAccount())

        // 清除账户信息
        loginRepository.clearLkAccount()

        // 验证账户信息已被清除
        assertNull(loginRepository.getLkAccount())
    }
}