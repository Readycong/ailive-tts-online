package com.danshuang.AiLive.data.model

import androidx.annotation.Keep

/**
 * 语音文件信息数据模型
 * 用于直播语音播放服务
 */
@Keep
data class VoiceFileInfo(
    val id: String,
    val text: String,
    val url: String,
    val cache: Boolean = false
)