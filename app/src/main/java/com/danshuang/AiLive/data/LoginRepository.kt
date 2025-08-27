package com.danshuang.AiLive.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import com.danshuang.AiLive.data.model.LoggedInUser
import com.danshuang.AiLive.data.model.UserProfile
import com.danshuang.AiLive.data.model.BoundAccountInfo
import com.google.gson.Gson

private val Context.dataStore by preferencesDataStore(name = "login_prefs")

class LoginRepository(private val context: Context) {
    private val gson = Gson()
    
    // 基础登录信息
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val USER_PHONE_KEY = stringPreferencesKey("user_phone")
    private val EXPIRES_TIME_KEY = longPreferencesKey("expires_time")
    
    // 用户个人信息
    private val USER_NICKNAME_KEY = stringPreferencesKey("user_nickname")
    private val USER_HEADIMG_KEY = stringPreferencesKey("user_headimg")
    private val USER_BALANCE_KEY = stringPreferencesKey("user_balance")
    private val USER_POINT_KEY = stringPreferencesKey("user_point")
    
    // 绑定账户信息
    private val BOUND_ACCOUNT_PLATFORM_KEY = stringPreferencesKey("bound_account_platform")
    private val BOUND_ACCOUNT_AVATAR_KEY = stringPreferencesKey("bound_account_avatar")
    private val BOUND_ACCOUNT_NICKNAME_KEY = stringPreferencesKey("bound_account_nickname")
    private val BOUND_ACCOUNT_USER_ID_KEY = stringPreferencesKey("bound_account_user_id")
    private val BOUND_ACCOUNT_HOME_URL_KEY = stringPreferencesKey("bound_account_home_url")
    private val BOUND_ACCOUNT_IS_LIVE_KEY = stringPreferencesKey("bound_account_is_live")
    private val BOUND_ACCOUNT_BIND_TIME_KEY = longPreferencesKey("bound_account_bind_time")

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[TOKEN_KEY]
        }

    val userPhone: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_PHONE_KEY]
        }
    
    val expiresTime: Flow<Long?> = context.dataStore.data
        .map { preferences ->
            preferences[EXPIRES_TIME_KEY]
        }
    
    val userProfile: Flow<UserProfile> = context.dataStore.data
        .map { preferences ->
            val boundAccount = if (preferences[BOUND_ACCOUNT_PLATFORM_KEY] != null) {
                BoundAccountInfo(
                    platform = preferences[BOUND_ACCOUNT_PLATFORM_KEY] ?: "douyin",
                    avatar = preferences[BOUND_ACCOUNT_AVATAR_KEY] ?: "",
                    nickname = preferences[BOUND_ACCOUNT_NICKNAME_KEY] ?: "",
                    userId = preferences[BOUND_ACCOUNT_USER_ID_KEY] ?: "",
                    homeUrl = preferences[BOUND_ACCOUNT_HOME_URL_KEY] ?: "",
                    isLive = preferences[BOUND_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                    bindTime = preferences[BOUND_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
                )
            } else {
                null
            }
            
            UserProfile(
                nickname = preferences[USER_NICKNAME_KEY] ?: "",
                headimg = preferences[USER_HEADIMG_KEY] ?: "",
                balance = preferences[USER_BALANCE_KEY] ?: "",
                point = preferences[USER_POINT_KEY] ?: "",
                boundAccount = boundAccount
            )
        }
    
    val loggedInUser: Flow<LoggedInUser?> = combine(
        authToken,
        userPhone,
        expiresTime,
        userProfile
    ) { token, phone, expires, profile ->
        if (token != null && phone != null && expires != null) {
            LoggedInUser(
                token = token,
                mobile = phone,
                expiresTime = expires.toUInt(),
                userProfile = profile
            )
        } else {
            null
        }
    }

    suspend fun saveLoginInfo(
        token: String, 
        phone: String, 
        expiresTime: UInt = 0u
    ) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_PHONE_KEY] = phone
            preferences[EXPIRES_TIME_KEY] = expiresTime.toLong()
        }
    }
    
    suspend fun saveLoggedInUser(user: LoggedInUser) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = user.token
            preferences[USER_PHONE_KEY] = user.mobile
            preferences[EXPIRES_TIME_KEY] = user.expiresTime.toLong()
            preferences[USER_NICKNAME_KEY] = user.userProfile.nickname
            preferences[USER_HEADIMG_KEY] = user.userProfile.headimg
            preferences[USER_BALANCE_KEY] = user.userProfile.balance
            preferences[USER_POINT_KEY] = user.userProfile.point
            
            // 保存绑定账户信息
            user.userProfile.boundAccount?.let { boundAccount ->
                preferences[BOUND_ACCOUNT_PLATFORM_KEY] = boundAccount.platform
                preferences[BOUND_ACCOUNT_AVATAR_KEY] = boundAccount.avatar
                preferences[BOUND_ACCOUNT_NICKNAME_KEY] = boundAccount.nickname
                preferences[BOUND_ACCOUNT_USER_ID_KEY] = boundAccount.userId
                preferences[BOUND_ACCOUNT_HOME_URL_KEY] = boundAccount.homeUrl
                preferences[BOUND_ACCOUNT_IS_LIVE_KEY] = boundAccount.isLive.toString()
                preferences[BOUND_ACCOUNT_BIND_TIME_KEY] = boundAccount.bindTime
            }
        }
    }
    
    suspend fun updateUserProfile(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[USER_NICKNAME_KEY] = profile.nickname
            preferences[USER_HEADIMG_KEY] = profile.headimg
            preferences[USER_BALANCE_KEY] = profile.balance
            preferences[USER_POINT_KEY] = profile.point
            
            // 更新绑定账户信息
            profile.boundAccount?.let { boundAccount ->
                preferences[BOUND_ACCOUNT_PLATFORM_KEY] = boundAccount.platform
                preferences[BOUND_ACCOUNT_AVATAR_KEY] = boundAccount.avatar
                preferences[BOUND_ACCOUNT_NICKNAME_KEY] = boundAccount.nickname
                preferences[BOUND_ACCOUNT_USER_ID_KEY] = boundAccount.userId
                preferences[BOUND_ACCOUNT_HOME_URL_KEY] = boundAccount.homeUrl
                preferences[BOUND_ACCOUNT_IS_LIVE_KEY] = boundAccount.isLive.toString()
                preferences[BOUND_ACCOUNT_BIND_TIME_KEY] = boundAccount.bindTime
            } ?: run {
                // 如果绑定账户信息为null，则清除相关字段
                preferences.remove(BOUND_ACCOUNT_PLATFORM_KEY)
                preferences.remove(BOUND_ACCOUNT_AVATAR_KEY)
                preferences.remove(BOUND_ACCOUNT_NICKNAME_KEY)
                preferences.remove(BOUND_ACCOUNT_USER_ID_KEY)
                preferences.remove(BOUND_ACCOUNT_HOME_URL_KEY)
                preferences.remove(BOUND_ACCOUNT_IS_LIVE_KEY)
                preferences.remove(BOUND_ACCOUNT_BIND_TIME_KEY)
            }
        }
    }

    suspend fun clearLoginInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_PHONE_KEY)
            preferences.remove(EXPIRES_TIME_KEY)
            preferences.remove(USER_NICKNAME_KEY)
            preferences.remove(USER_HEADIMG_KEY)
            preferences.remove(USER_BALANCE_KEY)
            preferences.remove(USER_POINT_KEY)
            // 同时清除绑定账户信息
            preferences.remove(BOUND_ACCOUNT_PLATFORM_KEY)
            preferences.remove(BOUND_ACCOUNT_AVATAR_KEY)
            preferences.remove(BOUND_ACCOUNT_NICKNAME_KEY)
            preferences.remove(BOUND_ACCOUNT_USER_ID_KEY)
            preferences.remove(BOUND_ACCOUNT_HOME_URL_KEY)
            preferences.remove(BOUND_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(BOUND_ACCOUNT_BIND_TIME_KEY)
        }
    }
    
    suspend fun saveBoundAccountInfo(boundAccount: BoundAccountInfo) {
        context.dataStore.edit { preferences ->
            preferences[BOUND_ACCOUNT_PLATFORM_KEY] = boundAccount.platform
            preferences[BOUND_ACCOUNT_AVATAR_KEY] = boundAccount.avatar
            preferences[BOUND_ACCOUNT_NICKNAME_KEY] = boundAccount.nickname
            preferences[BOUND_ACCOUNT_USER_ID_KEY] = boundAccount.userId
            preferences[BOUND_ACCOUNT_HOME_URL_KEY] = boundAccount.homeUrl
            preferences[BOUND_ACCOUNT_IS_LIVE_KEY] = boundAccount.isLive.toString()
            preferences[BOUND_ACCOUNT_BIND_TIME_KEY] = boundAccount.bindTime
        }
    }
    
    suspend fun clearBoundAccountInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(BOUND_ACCOUNT_PLATFORM_KEY)
            preferences.remove(BOUND_ACCOUNT_AVATAR_KEY)
            preferences.remove(BOUND_ACCOUNT_NICKNAME_KEY)
            preferences.remove(BOUND_ACCOUNT_USER_ID_KEY)
            preferences.remove(BOUND_ACCOUNT_HOME_URL_KEY)
            preferences.remove(BOUND_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(BOUND_ACCOUNT_BIND_TIME_KEY)
        }
    }

    fun isLoggedInFlow(): Flow<Boolean> {
        return combine(authToken, expiresTime) { token, expires ->
            token != null && expires != null && System.currentTimeMillis() / 1000 < expires
        }
    }
    
    fun isTokenValidFlow(): Flow<Boolean> {
        return expiresTime.map { expires ->
            expires != null && System.currentTimeMillis() / 1000 < expires
        }
    }
}