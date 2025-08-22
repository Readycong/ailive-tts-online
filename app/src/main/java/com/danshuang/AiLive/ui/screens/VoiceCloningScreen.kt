package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.ui.theme.AiLiveTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCloningScreen(
    navController: NavigationState
) {
    // 状态变量
    var isRecording by remember { mutableStateOf(false) }
    var hasRecordedAudio by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("声音克隆") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 已克隆声音卡片
            ClonedVoiceCard()
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 录音按钮
            RecordingButton(
                isRecording = isRecording,
                onRecordingStateChange = { recording ->
                    isRecording = recording
                    if (!recording && !hasRecordedAudio) {
                        hasRecordedAudio = true
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // 播放和上传按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // 播放按钮
                Button(
                    onClick = { isPlaying = !isPlaying },
                    enabled = hasRecordedAudio && !isRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isPlaying) "暂停" else "播放")
                }
                
                // 上传按钮
                Button(
                    onClick = { /* 上传录音文件到服务器 */ },
                    enabled = hasRecordedAudio && !isRecording && !isPlaying,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "上传",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("上传")
                }
            }
        }
    }
}

@Composable
fun ClonedVoiceCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "当前克隆声音",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "默认声音",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                IconButton(
                    onClick = { /* 播放示例声音 */ }
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "播放示例",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    onRecordingStateChange: (Boolean) -> Unit
) {
    val backgroundColor = if (isRecording) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(backgroundColor.copy(alpha = 0.1f))
                .border(2.dp, backgroundColor, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onRecordingStateChange(true)
                            tryAwaitRelease()
                            onRecordingStateChange(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "录音",
                tint = backgroundColor,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Text(
            text = if (isRecording) "录音中..." else "长按开始录音",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Preview(showBackground = true)
@Composable
fun VoiceCloningScreenPreview() {
    AiLiveTheme {
        VoiceCloningScreen(
            navController = NavigationState()
        )
    }
}