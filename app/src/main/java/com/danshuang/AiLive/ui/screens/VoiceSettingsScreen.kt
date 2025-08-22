package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.data.VoiceSettings
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.ui.components.RangeSelectorCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSettingsScreen(
    navController: NavigationState
) {
    var volumeRange by remember { mutableStateOf(VoiceSettings.volumeRange) }
    var pitchRange by remember { mutableStateOf(VoiceSettings.pitchRange) }
    var speedRange by remember { mutableStateOf(VoiceSettings.speedRange) }
    var silenceRange by remember { mutableStateOf(VoiceSettings.silenceRange) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音调整设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            // 保存设置到全局变量
                            VoiceSettings.applySettings(
                                volume = volumeRange,
                                pitch = pitchRange,
                                speed = speedRange,
                                silence = silenceRange
                            )
                            navController.navigateUp()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RangeSelectorCard(
                title = "音量放大范围",
                rangeValue = volumeRange,
                minRange = 0.5f,
                maxRange = 2.0f,
                description = "每句话播放时，将在这个区间随机一个音量",
                onValueChange = { volumeRange = it }
            )

            RangeSelectorCard(
                title = "音调范围",
                rangeValue = pitchRange,
                minRange = 0.5f,
                maxRange = 2.0f,
                description = "每句话播放时，将在这个区间随机一个音调",
                onValueChange = { pitchRange = it }
            )

            RangeSelectorCard(
                title = "语速范围",
                rangeValue = speedRange,
                minRange = 0.5f,
                maxRange = 2.0f,
                description = "每句话播放时，将在这个区间随机一个语速",
                onValueChange = { speedRange = it }
            )

            RangeSelectorCard(
                title = "间隔静音时间范围",
                rangeValue = silenceRange,
                minRange = 0f,
                maxRange = 2f,
                description = "每句话播放时，将在这个区间随机一个间隔静音时间",
                onValueChange = { silenceRange = it }
            )

            Spacer(modifier = Modifier.weight(1f))

            // 保存按钮已移到顶部AppBar，这里可以保留或移除底部按钮
        }
    }
}

@Composable
private fun VoiceParameterCard(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    description: String,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = valueText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = range,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = description,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceSettingsScreenPreview() {
    VoiceSettingsScreen(
        navController = NavigationState()
    )
}