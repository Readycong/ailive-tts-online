package com.danshuang.AiLive.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.SelectedVoiceData
import com.danshuang.AiLive.data.model.VoiceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 音色管理ViewModel
 */
class VoiceManagementViewModel(
    private val context: Context,
    private val apiUtils: ServerApiUtilsV2
) : ViewModel() {

    // 当前选中的音色
    private val _selectedVoice = MutableStateFlow<SelectedVoiceData?>(null)
    val selectedVoice: StateFlow<SelectedVoiceData?> = _selectedVoice.asStateFlow()

    // 全部音色列表
    private val _voiceList = MutableStateFlow<List<VoiceItem>>(emptyList())
    val voiceList: StateFlow<List<VoiceItem>> = _voiceList.asStateFlow()

    // 临时选中的音色（在保存前）
    private val _tempSelectedVoice = MutableStateFlow<VoiceItem?>(null)
    val tempSelectedVoice: StateFlow<VoiceItem?> = _tempSelectedVoice.asStateFlow()

    // 临时选中的情感
    private val _tempSelectedEmotion = MutableStateFlow<String>("")
    val tempSelectedEmotion: StateFlow<String> = _tempSelectedEmotion.asStateFlow()

    // 加载状态
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // 错误消息
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // 保存成功标志
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    /**
     * 加载当前音色和全部音色列表
     */
    fun loadVoiceData() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // 并发请求
                val selectedResponse = apiUtils.getSelectedVoice()
                val listResponse = apiUtils.getVoiceList()

                if (selectedResponse.isSuccess && selectedResponse.data != null) {
                    _selectedVoice.value = selectedResponse.data
                    // 初始化临时选择
                    val voiceInfo = selectedResponse.data.voice_info
                    _tempSelectedVoice.value = VoiceItem(
                        voice_id = voiceInfo.voice_id,
                        voice_name = voiceInfo.voice_name,
                        voice_desc = voiceInfo.voice_desc,
                        voice_type = voiceInfo.voice_type,
                        enable_emotion = voiceInfo.enable_emotion,
                        emotions = voiceInfo.emotions,
                        audio_url = voiceInfo.audio_url,
                        avatar_url = voiceInfo.avatar_url
                    )
                    // 如果返回的emotion为空，设置为"通用"
                    _tempSelectedEmotion.value = if (selectedResponse.data.emotion.isEmpty()) {
                        "通用"
                    } else {
                        selectedResponse.data.emotion
                    }
                } else {
                    _errorMessage.value = "获取当前音色失败: ${selectedResponse.msg}"
                }

                if (listResponse.isSuccess && listResponse.data != null) {
                    _voiceList.value = listResponse.data
                } else {
                    _errorMessage.value = "获取音色列表失败: ${listResponse.msg}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 选择音色（临时，未保存）
     */
    fun selectVoice(voice: VoiceItem) {
        _tempSelectedVoice.value = voice
        
        // 如果选择的是当前服务端保存的音色，恢复服务端保存的情感值
        val savedVoiceId = _selectedVoice.value?.voice_info?.voice_id
        if (savedVoiceId == voice.voice_id) {
            // 恢复服务端保存的情感值
            _tempSelectedEmotion.value = if (_selectedVoice.value?.emotion.isNullOrEmpty()) {
                "通用"
            } else {
                _selectedVoice.value?.emotion ?: "通用"
            }
        } else {
            // 选择其他音色，使用第一个情感作为默认值
            if (voice.enable_emotion == 1 && voice.emotions.isNotEmpty()) {
                _tempSelectedEmotion.value = voice.emotions[0].emotion
            } else {
                _tempSelectedEmotion.value = "通用"
            }
        }
    }

    /**
     * 选择情感（临时，未保存）
     */
    fun selectEmotion(emotion: String) {
        _tempSelectedEmotion.value = emotion
    }

    /**
     * 保存音色设置
     */
    fun saveVoice() {
        val voice = _tempSelectedVoice.value
        if (voice == null) {
            _errorMessage.value = "请先选择音色"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _saveSuccess.value = false

            try {
                val response = apiUtils.saveVoice(
                    voiceType = voice.voice_type,
                    emotion = _tempSelectedEmotion.value
                )

                if (response.isSuccess) {
                    // 更新当前选中的音色
                    _selectedVoice.value = SelectedVoiceData(
                        voice_type = voice.voice_type,
                       emotion = _tempSelectedEmotion.value,
                        voice_info = com.danshuang.AiLive.data.model.VoiceInfo(
                            voice_id = voice.voice_id,
                            voice_name = voice.voice_name,
                            voice_desc = voice.voice_desc,
                            voice_type = voice.voice_type,
                            enable_emotion = voice.enable_emotion,
                            emotions = voice.emotions,
                            audio_url = voice.audio_url,
                            avatar_url = voice.avatar_url
                        )
                    )
                    _saveSuccess.value = true
                } else {
                    _errorMessage.value = "保存失败: ${response.msg}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "保存失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 清除错误消息
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * 重置保存成功标志
     */
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}

/**
 * ViewModel 工厂
 */
class VoiceManagementViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceManagementViewModel::class.java)) {
            val loginRepository = LoginRepository(context)
            val tokenManager = TokenManager(loginRepository)
            val apiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
            @Suppress("UNCHECKED_CAST")
            return VoiceManagementViewModel(context, apiUtils) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
