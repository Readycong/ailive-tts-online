package com.danshuang.AiLive.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class RangeValue(
    val min: Float,
    val max: Float
) {
    companion object {
        val STEP = 0.1f
        fun generateOptions(minRange: Float, maxRange: Float): List<Float> {
            return generateSequence(minRange) { value -> value + STEP }
                .takeWhile { value -> value <= maxRange }
                .toList()
        }
    }
}

object VoiceSettings {
    var volumeRange by mutableStateOf(RangeValue(0.5f, 2.0f))
    var pitchRange by mutableStateOf(RangeValue(0.5f, 2.0f))
    var speedRange by mutableStateOf(RangeValue(0.5f, 2.0f))
    var silenceRange by mutableStateOf(RangeValue(0f, 2.0f))

    fun resetToDefaults() {
        volumeRange = RangeValue(1.0f, 1.0f)
        pitchRange = RangeValue(1.0f, 1.0f)
        speedRange = RangeValue(1.0f, 1.0f)
        silenceRange = RangeValue(0f, 2.0f)
    }

    fun applySettings(
        volume: RangeValue,
        pitch: RangeValue,
        speed: RangeValue,
        silence: RangeValue
    ) {
        volumeRange = volume
        pitchRange = pitch
        speedRange = speed
        silenceRange = silence
    }
}