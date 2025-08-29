package com.danshuang.AiLive.data.model

import androidx.annotation.Keep
import com.danshuang.AiLive.data.model.PlaybackStage

/**
 * 语音文件信息模型
 */
@Keep
data class VoiceFileInfo(
    val id: String,
    val url: String,
    val duration: Long,  // 毫秒
    val cached: Boolean = false
)

/**
 * 播放阶段模型
 */
@Keep
data class PlaybackStageInfo(
    val stage: PlaybackStage,
    val voiceFiles: List<VoiceFileInfo>,
    val loop: Boolean = false
)