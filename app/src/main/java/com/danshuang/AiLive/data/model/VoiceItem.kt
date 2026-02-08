package com.danshuang.AiLive.data.model

/**
 * 情感类型
 */
data class VoiceEmotion(
    val emotion: String  // 情感类型：happy, surprised, excited, tender, advertising等
)

/**
 * 音色项
 */
data class VoiceItem(
    val voice_id: Int,
    val voice_name: String,
    val voice_desc: String,
    val voice_type: String,
    val enable_emotion: Int,  // 0-不支持情感, 1-支持情感
    val emotions: List<VoiceEmotion>,
    val audio_url: String,
    val avatar_url: String
)

/**
 * 音色信息（嵌套在SelectedVoiceData中）
 */
data class VoiceInfo(
    val voice_id: Int,
    val voice_name: String,
    val voice_desc: String,
    val voice_type: String,
    val enable_emotion: Int,
    val emotions: List<VoiceEmotion>,
    val audio_url: String,
    val avatar_url: String
)

/**
 * 选中的音色数据
 */
data class SelectedVoiceData(
    val voice_type: String,
    val emotion: String,
    val voice_info: VoiceInfo
)

/**
 * 保存音色请求参数
 */
data class SaveVoiceRequest(
    val voice_type: String,
    val emotion: String
)
