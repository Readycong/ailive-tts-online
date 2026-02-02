package com.danshuang.AiLive.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import com.danshuang.AiLive.data.model.LoggedInUser
import com.danshuang.AiLive.data.model.UserProfile
import com.danshuang.AiLive.data.model.BoundAccountInfo
import com.danshuang.AiLive.data.model.VoiceSettingsData
import com.danshuang.AiLive.data.model.PlatformType
import com.google.gson.Gson

private val Context.dataStore by preferencesDataStore(name = "login_prefs")

class LoginRepository(val context: Context) {
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
    private val USER_AILIVE_EXPIRE_TIME_KEY = longPreferencesKey("user_ailive_expire_time")

    // 绑定账户信息
    private val BOUND_ACCOUNT_PLATFORM_KEY = stringPreferencesKey("bound_account_platform")
    private val BOUND_ACCOUNT_AVATAR_KEY = stringPreferencesKey("bound_account_avatar")
    private val BOUND_ACCOUNT_NICKNAME_KEY = stringPreferencesKey("bound_account_nickname")
    private val BOUND_ACCOUNT_USER_ID_KEY = stringPreferencesKey("bound_account_user_id")
    private val BOUND_ACCOUNT_HOME_URL_KEY = stringPreferencesKey("bound_account_home_url")
    private val BOUND_ACCOUNT_IS_LIVE_KEY = stringPreferencesKey("bound_account_is_live")
    private val BOUND_ACCOUNT_BIND_TIME_KEY = longPreferencesKey("bound_account_bind_time")
    private val BOUND_ACCOUNT_STATUS_KEY = longPreferencesKey("bound_account_status")
    private val BOUND_ACCOUNT_BEGIN_AT_KEY = stringPreferencesKey("bound_account_begin_at")
    private val BOUND_ACCOUNT_EXPIRES_AT_KEY = stringPreferencesKey("bound_account_expires_at")

    // 抖音账户信息存储键
    private val DOUYIN_ACCOUNT_PLATFORM_KEY = stringPreferencesKey("douyin_account_platform")
    private val DOUYIN_ACCOUNT_AVATAR_KEY = stringPreferencesKey("douyin_account_avatar")
    private val DOUYIN_ACCOUNT_NICKNAME_KEY = stringPreferencesKey("douyin_account_nickname")
    private val DOUYIN_ACCOUNT_USER_ID_KEY = stringPreferencesKey("douyin_account_user_id")
    private val DOUYIN_ACCOUNT_HOME_URL_KEY = stringPreferencesKey("douyin_account_home_url")
    private val DOUYIN_ACCOUNT_IS_LIVE_KEY = stringPreferencesKey("douyin_account_is_live")
    private val DOUYIN_ACCOUNT_BIND_TIME_KEY = longPreferencesKey("douyin_account_bind_time")

    // 来客账户信息存储键
    private val LK_ACCOUNT_PLATFORM_KEY = stringPreferencesKey("lk_account_platform")
    private val LK_ACCOUNT_AVATAR_KEY = stringPreferencesKey("lk_account_avatar")
    private val LK_ACCOUNT_NICKNAME_KEY = stringPreferencesKey("lk_account_nickname")
    private val LK_ACCOUNT_USER_ID_KEY = stringPreferencesKey("lk_account_user_id")
    private val LK_ACCOUNT_HOME_URL_KEY = stringPreferencesKey("lk_account_home_url")
    private val LK_ACCOUNT_IS_LIVE_KEY = stringPreferencesKey("lk_account_is_live")
    private val LK_ACCOUNT_BIND_TIME_KEY = longPreferencesKey("lk_account_bind_time")

    // 直播间信息
    private val LIVE_ROOM_ID_KEY = stringPreferencesKey("live_room_id")
    
    // 语音设置信息
    private val VOLUME_RANGE_MIN_KEY = floatPreferencesKey("volume_range_min")
    private val VOLUME_RANGE_MAX_KEY = floatPreferencesKey("volume_range_max")
    private val PITCH_RANGE_MIN_KEY = floatPreferencesKey("pitch_range_min")
    private val PITCH_RANGE_MAX_KEY = floatPreferencesKey("pitch_range_max")
    private val SPEED_RANGE_MIN_KEY = floatPreferencesKey("speed_range_min")
    private val SPEED_RANGE_MAX_KEY = floatPreferencesKey("speed_range_max")
    private val SILENCE_RANGE_MIN_KEY = floatPreferencesKey("silence_range_min")
    private val SILENCE_RANGE_MAX_KEY = floatPreferencesKey("silence_range_max")

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

    // 直播间ID
    val liveRoomId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[LIVE_ROOM_ID_KEY]
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
                    bindTime = preferences[BOUND_ACCOUNT_BIND_TIME_KEY]
                        ?: System.currentTimeMillis(),
                    status = preferences[BOUND_ACCOUNT_STATUS_KEY] ?: 1L,
                    beginAt = preferences[BOUND_ACCOUNT_BEGIN_AT_KEY] ?: "",
                    expiresAt = preferences[BOUND_ACCOUNT_EXPIRES_AT_KEY] ?: ""
                )
            } else {
                null
            }

            UserProfile(
                nickname = preferences[USER_NICKNAME_KEY] ?: "",
                headimg = preferences[USER_HEADIMG_KEY] ?: "",
                balance = preferences[USER_BALANCE_KEY] ?: "",
                point = preferences[USER_POINT_KEY] ?: "",
                ailiveExpireTime = preferences[USER_AILIVE_EXPIRE_TIME_KEY] ?: 0L,
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
            // ailiveExpireTime 现在由 getBinding 接口同步，login 接口不再设置此值
            // preferences[USER_AILIVE_EXPIRE_TIME_KEY] = user.userProfile.ailiveExpireTime

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
            // ailiveExpireTime 现在由 getBinding 接口同步，此处不再更新
            // preferences[USER_AILIVE_EXPIRE_TIME_KEY] = profile.ailiveExpireTime

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
            preferences.remove(USER_AILIVE_EXPIRE_TIME_KEY)
            // 同时清除绑定账户信息
            preferences.remove(BOUND_ACCOUNT_PLATFORM_KEY)
            preferences.remove(BOUND_ACCOUNT_AVATAR_KEY)
            preferences.remove(BOUND_ACCOUNT_NICKNAME_KEY)
            preferences.remove(BOUND_ACCOUNT_USER_ID_KEY)
            preferences.remove(BOUND_ACCOUNT_HOME_URL_KEY)
            preferences.remove(BOUND_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(BOUND_ACCOUNT_BIND_TIME_KEY)
            // 清除直播间ID
            preferences.remove(LIVE_ROOM_ID_KEY)
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
            preferences[BOUND_ACCOUNT_STATUS_KEY] = boundAccount.status
            preferences[BOUND_ACCOUNT_BEGIN_AT_KEY] = boundAccount.beginAt
            preferences[BOUND_ACCOUNT_EXPIRES_AT_KEY] = boundAccount.expiresAt
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
            preferences.remove(BOUND_ACCOUNT_STATUS_KEY)
            preferences.remove(BOUND_ACCOUNT_BEGIN_AT_KEY)
            preferences.remove(BOUND_ACCOUNT_EXPIRES_AT_KEY)
        }
    }


    /**
     * 保存直播间ID
     */
    suspend fun saveLiveRoomId(roomId: String) {
        context.dataStore.edit { preferences ->
            preferences[LIVE_ROOM_ID_KEY] = roomId
        }
    }

    /**
     * 清除直播间ID
     */
    suspend fun clearLiveRoomId() {
        context.dataStore.edit { preferences ->
            preferences.remove(LIVE_ROOM_ID_KEY)
        }
    }


    /**
     * 从get-binding接口数据更新绑定账户信息
     */
    suspend fun updateBoundAccountFromBindingData(bindingData: Map<String, Any>) {
        context.dataStore.edit { preferences ->
            val douyinId = bindingData["douyin_id"] as? String ?: ""
            val nickname = bindingData["nickname"] as? String ?: ""
            val avatar = bindingData["avatar"] as? String ?: ""
            val userHomeUrl = bindingData["user_home_url"] as? String ?: ""
            val status = when (val statusValue = bindingData["status"]) {
                is Number -> statusValue.toLong()
                is String -> statusValue.toLongOrNull() ?: 1L
                else -> 1L
            }
            val beginAt = bindingData["begin_at"] as? String ?: ""
            val expiresAt = bindingData["expires_at"] as? String ?: ""
            
            // 将 expires_at 日期字符串转换为 Unix 时间戳（秒）
            val ailiveExpireTime = if (expiresAt.isNotEmpty()) {
                try {
                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    val date = dateFormat.parse(expiresAt)
                    // 将到期日期设置为当天的 23:59:59
                    val calendar = java.util.Calendar.getInstance()
                    calendar.time = date
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 23)
                    calendar.set(java.util.Calendar.MINUTE, 59)
                    calendar.set(java.util.Calendar.SECOND, 59)
                    calendar.timeInMillis / 1000 // 转换为秒
                } catch (e: Exception) {
                    0L
                }
            } else {
                0L
            }

            if (douyinId.isNotEmpty()) {
                // 更新绑定账户信息
                preferences[BOUND_ACCOUNT_PLATFORM_KEY] = "douyin"
                preferences[BOUND_ACCOUNT_AVATAR_KEY] = avatar
                preferences[BOUND_ACCOUNT_NICKNAME_KEY] = nickname
                preferences[BOUND_ACCOUNT_USER_ID_KEY] = douyinId
                preferences[BOUND_ACCOUNT_HOME_URL_KEY] = userHomeUrl
                preferences[BOUND_ACCOUNT_IS_LIVE_KEY] = "false" // 默认设置为离线状态
                preferences[BOUND_ACCOUNT_BIND_TIME_KEY] = System.currentTimeMillis()
                preferences[BOUND_ACCOUNT_STATUS_KEY] = status
                preferences[BOUND_ACCOUNT_BEGIN_AT_KEY] = beginAt
                preferences[BOUND_ACCOUNT_EXPIRES_AT_KEY] = expiresAt
                
                // 同步更新 ailiveExpireTime
                preferences[USER_AILIVE_EXPIRE_TIME_KEY] = ailiveExpireTime

                // 同时更新用户头像
                if (avatar.isNotEmpty()) {
                    preferences[USER_HEADIMG_KEY] = avatar
                }
            } else {
                // 如果没有绑定信息，清除相关字段
                preferences.remove(BOUND_ACCOUNT_PLATFORM_KEY)
                preferences.remove(BOUND_ACCOUNT_AVATAR_KEY)
                preferences.remove(BOUND_ACCOUNT_NICKNAME_KEY)
                preferences.remove(BOUND_ACCOUNT_USER_ID_KEY)
                preferences.remove(BOUND_ACCOUNT_HOME_URL_KEY)
                preferences.remove(BOUND_ACCOUNT_IS_LIVE_KEY)
                preferences.remove(BOUND_ACCOUNT_BIND_TIME_KEY)
                preferences.remove(BOUND_ACCOUNT_STATUS_KEY)
                preferences.remove(BOUND_ACCOUNT_BEGIN_AT_KEY)
                preferences.remove(BOUND_ACCOUNT_EXPIRES_AT_KEY)
                // 清除 ailiveExpireTime
                preferences[USER_AILIVE_EXPIRE_TIME_KEY] = 0L
            }
        }
    }

    /**
     * 保存语音设置
     */
    suspend fun saveVoiceSettings(settings: VoiceSettingsData) {
        context.dataStore.edit { preferences ->
            preferences[VOLUME_RANGE_MIN_KEY] = settings.volumeRange.min
            preferences[VOLUME_RANGE_MAX_KEY] = settings.volumeRange.max
            preferences[PITCH_RANGE_MIN_KEY] = settings.pitchRange.min
            preferences[PITCH_RANGE_MAX_KEY] = settings.pitchRange.max
            preferences[SPEED_RANGE_MIN_KEY] = settings.speedRange.min
            preferences[SPEED_RANGE_MAX_KEY] = settings.speedRange.max
            preferences[SILENCE_RANGE_MIN_KEY] = settings.silenceRange.min
            preferences[SILENCE_RANGE_MAX_KEY] = settings.silenceRange.max
        }
    }

    /**
     * 获取语音设置
     */
    fun getVoiceSettingsFlow(): Flow<VoiceSettingsData> = context.dataStore.data
        .map { preferences ->
            VoiceSettingsData(
                volumeRange = RangeValue(
                    min = preferences[VOLUME_RANGE_MIN_KEY] ?: 1.0f,
                    max = preferences[VOLUME_RANGE_MAX_KEY] ?: 1.0f
                ),
                pitchRange = RangeValue(
                    min = preferences[PITCH_RANGE_MIN_KEY] ?: 1.0f,
                    max = preferences[PITCH_RANGE_MAX_KEY] ?: 1.0f
                ),
                speedRange = RangeValue(
                    min = preferences[SPEED_RANGE_MIN_KEY] ?: 1.0f,
                    max = preferences[SPEED_RANGE_MAX_KEY] ?: 1.0f
                ),
                silenceRange = RangeValue(
                    min = preferences[SILENCE_RANGE_MIN_KEY] ?: 0f,
                    max = preferences[SILENCE_RANGE_MAX_KEY] ?: 2.0f
                )
            )
        }

    /**
     * 获取语音设置（一次性获取）
     */
    suspend fun getVoiceSettings(): VoiceSettingsData {
        val preferences = context.dataStore.data.first()
        return VoiceSettingsData(
            volumeRange = RangeValue(
                min = preferences[VOLUME_RANGE_MIN_KEY] ?: 1.0f,
                max = preferences[VOLUME_RANGE_MAX_KEY] ?: 1.0f
            ),
            pitchRange = RangeValue(
                min = preferences[PITCH_RANGE_MIN_KEY] ?: 1.0f,
                max = preferences[PITCH_RANGE_MAX_KEY] ?: 1.0f
            ),
            speedRange = RangeValue(
                min = preferences[SPEED_RANGE_MIN_KEY] ?: 1.0f,
                max = preferences[SPEED_RANGE_MAX_KEY] ?: 1.0f
            ),
            silenceRange = RangeValue(
                min = preferences[SILENCE_RANGE_MIN_KEY] ?: 0f,
                max = preferences[SILENCE_RANGE_MAX_KEY] ?: 2.0f
            )
        )
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

    // 抖音账户信息获取（挂起函数）
    suspend fun getDouyinAccount(): BoundAccountInfo? {
        val preferences = context.dataStore.data.first()
        return if (preferences[DOUYIN_ACCOUNT_PLATFORM_KEY] != null) {
            BoundAccountInfo(
                platform = PlatformType.DOUYIN,
                avatar = preferences[DOUYIN_ACCOUNT_AVATAR_KEY] ?: "",
                nickname = preferences[DOUYIN_ACCOUNT_NICKNAME_KEY] ?: "",
                userId = preferences[DOUYIN_ACCOUNT_USER_ID_KEY] ?: "",
                homeUrl = preferences[DOUYIN_ACCOUNT_HOME_URL_KEY] ?: "",
                isLive = preferences[DOUYIN_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                bindTime = preferences[DOUYIN_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
            )
        } else {
            null
        }
    }
    
    // 抖音账户信息流
    fun getDouyinAccountFlow(): Flow<BoundAccountInfo?> = context.dataStore.data
        .map { preferences ->
            if (preferences[DOUYIN_ACCOUNT_PLATFORM_KEY] != null) {
                BoundAccountInfo(
                    platform = PlatformType.DOUYIN,
                    avatar = preferences[DOUYIN_ACCOUNT_AVATAR_KEY] ?: "",
                    nickname = preferences[DOUYIN_ACCOUNT_NICKNAME_KEY] ?: "",
                    userId = preferences[DOUYIN_ACCOUNT_USER_ID_KEY] ?: "",
                    homeUrl = preferences[DOUYIN_ACCOUNT_HOME_URL_KEY] ?: "",
                    isLive = preferences[DOUYIN_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                    bindTime = preferences[DOUYIN_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    
    // 来客账户信息获取（挂起函数）
    suspend fun getLkAccount(): BoundAccountInfo? {
        val preferences = context.dataStore.data.first()
        return if (preferences[LK_ACCOUNT_PLATFORM_KEY] != null) {
            BoundAccountInfo(
                platform = PlatformType.LK,
                avatar = preferences[LK_ACCOUNT_AVATAR_KEY] ?: "",
                nickname = preferences[LK_ACCOUNT_NICKNAME_KEY] ?: "",
                userId = preferences[LK_ACCOUNT_USER_ID_KEY] ?: "",
                homeUrl = preferences[LK_ACCOUNT_HOME_URL_KEY] ?: "",
                isLive = preferences[LK_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                bindTime = preferences[LK_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
            )
        } else {
            null
        }
    }
    
    // 来客账户信息流
    fun getLkAccountFlow(): Flow<BoundAccountInfo?> = context.dataStore.data
        .map { preferences ->
            if (preferences[LK_ACCOUNT_PLATFORM_KEY] != null) {
                BoundAccountInfo(
                    platform = PlatformType.LK,
                    avatar = preferences[LK_ACCOUNT_AVATAR_KEY] ?: "",
                    nickname = preferences[LK_ACCOUNT_NICKNAME_KEY] ?: "",
                    userId = preferences[LK_ACCOUNT_USER_ID_KEY] ?: "",
                    homeUrl = preferences[LK_ACCOUNT_HOME_URL_KEY] ?: "",
                    isLive = preferences[LK_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                    bindTime = preferences[LK_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    
    // 保存抖音账户信息
    suspend fun saveDouyinAccount(account: BoundAccountInfo) {
        context.dataStore.edit { preferences ->
            preferences[DOUYIN_ACCOUNT_PLATFORM_KEY] = account.platform
            preferences[DOUYIN_ACCOUNT_AVATAR_KEY] = account.avatar
            preferences[DOUYIN_ACCOUNT_NICKNAME_KEY] = account.nickname
            preferences[DOUYIN_ACCOUNT_USER_ID_KEY] = account.userId
            preferences[DOUYIN_ACCOUNT_HOME_URL_KEY] = account.homeUrl
            preferences[DOUYIN_ACCOUNT_IS_LIVE_KEY] = account.isLive.toString()
            preferences[DOUYIN_ACCOUNT_BIND_TIME_KEY] = account.bindTime
        }
    }
    
    // 保存来客账户信息
    suspend fun saveLkAccount(account: BoundAccountInfo) {
        context.dataStore.edit { preferences ->
            preferences[LK_ACCOUNT_PLATFORM_KEY] = account.platform
            preferences[LK_ACCOUNT_AVATAR_KEY] = account.avatar
            preferences[LK_ACCOUNT_NICKNAME_KEY] = account.nickname
            preferences[LK_ACCOUNT_USER_ID_KEY] = account.userId
            preferences[LK_ACCOUNT_HOME_URL_KEY] = account.homeUrl
            preferences[LK_ACCOUNT_IS_LIVE_KEY] = account.isLive.toString()
            preferences[LK_ACCOUNT_BIND_TIME_KEY] = account.bindTime
        }
    }
    
    // 清除抖音账户信息
    suspend fun clearDouyinAccount() {
        context.dataStore.edit { preferences ->
            preferences.remove(DOUYIN_ACCOUNT_PLATFORM_KEY)
            preferences.remove(DOUYIN_ACCOUNT_AVATAR_KEY)
            preferences.remove(DOUYIN_ACCOUNT_NICKNAME_KEY)
            preferences.remove(DOUYIN_ACCOUNT_USER_ID_KEY)
            preferences.remove(DOUYIN_ACCOUNT_HOME_URL_KEY)
            preferences.remove(DOUYIN_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(DOUYIN_ACCOUNT_BIND_TIME_KEY)
        }
    }
    
    // 清除来客账户信息
    suspend fun clearLkAccount() {
        context.dataStore.edit { preferences ->
            preferences.remove(LK_ACCOUNT_PLATFORM_KEY)
            preferences.remove(LK_ACCOUNT_AVATAR_KEY)
            preferences.remove(LK_ACCOUNT_NICKNAME_KEY)
            preferences.remove(LK_ACCOUNT_USER_ID_KEY)
            preferences.remove(LK_ACCOUNT_HOME_URL_KEY)
            preferences.remove(LK_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(LK_ACCOUNT_BIND_TIME_KEY)
        }
    }
}