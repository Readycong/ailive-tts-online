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
            UserProfile(
                nickname = preferences[USER_NICKNAME_KEY] ?: "",
                headimg = preferences[USER_HEADIMG_KEY] ?: "",
                balance = preferences[USER_BALANCE_KEY] ?: "",
                point = preferences[USER_POINT_KEY] ?: ""
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
        }
    }
    
    suspend fun updateUserProfile(profile: UserProfile) {
        context.dataStore.edit { preferences ->
            preferences[USER_NICKNAME_KEY] = profile.nickname
            preferences[USER_HEADIMG_KEY] = profile.headimg
            preferences[USER_BALANCE_KEY] = profile.balance
            preferences[USER_POINT_KEY] = profile.point
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