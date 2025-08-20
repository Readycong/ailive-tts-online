package com.danshuang.AiLive.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "login_prefs")

class LoginRepository(private val context: Context) {
    private val TOKEN_KEY = stringPreferencesKey("auth_token")
    private val USER_PHONE_KEY = stringPreferencesKey("user_phone")

    val authToken: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[TOKEN_KEY]
        }

    val userPhone: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[USER_PHONE_KEY]
        }

    suspend fun saveLoginInfo(token: String, phone: String) {
        context.dataStore.edit { preferences ->
            preferences[TOKEN_KEY] = token
            preferences[USER_PHONE_KEY] = phone
        }
    }

    suspend fun clearLoginInfo() {
        context.dataStore.edit { preferences ->
            preferences.remove(TOKEN_KEY)
            preferences.remove(USER_PHONE_KEY)
        }
    }

    fun isLoggedInFlow(): Flow<Boolean> {
        return authToken.map { it != null }
    }
}