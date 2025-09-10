package com.danshuang.AiLive.data.model

import com.danshuang.AiLive.data.RangeValue

data class VoiceSettingsData(
    val volumeRange: RangeValue,
    val pitchRange: RangeValue,
    val speedRange: RangeValue,
    val silenceRange: RangeValue
)