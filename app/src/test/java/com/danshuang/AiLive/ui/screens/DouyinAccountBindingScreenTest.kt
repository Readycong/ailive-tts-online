package com.danshuang.AiLive.ui.screens

import com.danshuang.AiLive.data.model.UserAccountInfo
import org.junit.Test
import org.junit.Assert.*

class DouyinAccountBindingScreenTest {
    
    @Test
    fun testUserAccountInfoCreation() {
        // Test creating a UserAccountInfo object
        val userAccount = UserAccountInfo(
            avatar = "https://example.com/avatar.png",
            nickname = "TestUser",
            userId = "123456789",
            secUid = "sec_uid_123456",
            homeUrl = "https://www.douyin.com/user/123456789"
        )
        
        assertEquals("https://example.com/avatar.png", userAccount.avatar)
        assertEquals("TestUser", userAccount.nickname)
        assertEquals("123456789", userAccount.userId)
        assertEquals("sec_uid_123456", userAccount.secUid)
        assertEquals("https://www.douyin.com/user/123456789", userAccount.homeUrl)
    }
    
    @Test
    fun testUserAccountInfoDefaultValues() {
        // Test creating a UserAccountInfo object with default values
        val userAccount = UserAccountInfo()
        
        assertEquals("", userAccount.avatar)
        assertEquals("", userAccount.nickname)
        assertEquals("", userAccount.userId)
        assertEquals("", userAccount.secUid)
        assertEquals("", userAccount.homeUrl)
    }
}