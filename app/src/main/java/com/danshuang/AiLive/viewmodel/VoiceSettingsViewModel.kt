package com.danshuang.AiLive.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.model.VoiceSettingsData
import kotlinx.coroutines.launch

class VoiceSettingsViewModel(private val context: Context, private val loginRepository: LoginRepository) : ViewModel() {
    
    fun saveVoiceSettingsAndNotify(settings: VoiceSettingsData) {
        viewModelScope.launch {
            // 保存到DataStore
            loginRepository.saveVoiceSettings(settings)
            
            // 发送广播通知服务参数已更新
            val intent = Intent("com.danshuang.AiLive.VOICE_SETTINGS_CHANGED")
                .setPackage(context.packageName)
            context.sendBroadcast(intent)
        }
    }
}

class VoiceSettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceSettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return VoiceSettingsViewModel(context, LoginRepository(context)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}