package com.danshuang.AiLive.ui.screens

import android.media.MediaPlayer
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.danshuang.AiLive.data.model.VoiceItem
import com.danshuang.AiLive.data.model.VoiceEmotion
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.viewmodel.VoiceManagementViewModel
import com.danshuang.AiLive.viewmodel.VoiceManagementViewModelFactory
import kotlinx.coroutines.launch

/**
 * 音色管理页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementScreen(
    navController: NavigationState
) {
    val context = LocalContext.current
    val viewModel: VoiceManagementViewModel = viewModel(
        factory = VoiceManagementViewModelFactory(context)
    )
    
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val voiceList by viewModel.voiceList.collectAsState()
    val tempSelectedVoice by viewModel.tempSelectedVoice.collectAsState()
    val tempSelectedEmotion by viewModel.tempSelectedEmotion.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 管理MediaPlayer实例和播放状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPlayingUrl by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    
    // 清理MediaPlayer
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
            currentPlayingUrl = null
            isPlaying = false
        }
    }

    // 初始化加载数据
    LaunchedEffect(Unit) {
        viewModel.loadVoiceData()
    }

    // 显示错误消息
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // 保存成功提示
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar("保存成功")
            viewModel.resetSaveSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("音色管理") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { viewModel.saveVoice() },
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "保存",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 当前音色区域（包含情感选择）
                CurrentVoiceSection(
                    selectedVoice = selectedVoice,
                    tempSelectedVoice = tempSelectedVoice,
                    tempSelectedEmotion = tempSelectedEmotion,
                    currentPlayingUrl = currentPlayingUrl,
                    isPlaying = isPlaying,
                    onPlayAudio = { url ->
                        if (currentPlayingUrl == url && isPlaying) {
                            // 停止播放
                            mediaPlayer?.stop()
                            mediaPlayer?.release()
                            mediaPlayer = null
                            currentPlayingUrl = null
                            isPlaying = false
                        } else {
                            // 开始播放新音频
                            mediaPlayer?.release()
                            currentPlayingUrl = url
                            isPlaying = true
                            mediaPlayer = MediaPlayer().apply {
                                try {
                                    setDataSource(url)
                                    prepareAsync()
                                    setOnPreparedListener { start() }
                                    setOnCompletionListener {
                                        release()
                                        currentPlayingUrl = null
                                        isPlaying = false
                                    }
                                    setOnErrorListener { _, _, _ ->
                                        release()
                                        currentPlayingUrl = null
                                        isPlaying = false
                                        true
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("VoiceManagement", "播放音频失败", e)
                                    release()
                                    currentPlayingUrl = null
                                    isPlaying = false
                                }
                            }
                        }
                    },
                    onEmotionSelected = { emotion ->
                        viewModel.selectEmotion(emotion)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 全部音色区域
                Text(
                    text = "全部音色",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(voiceList) { voice ->
                        VoiceItemCard(
                            voice = voice,
                            // 只有与服务端保存的音色一致才显示为使用中
                            isSelected = selectedVoice?.voice_info?.voice_id == voice.voice_id,
                            // 临时选中但未保存
                            isTempSelected = tempSelectedVoice?.voice_id == voice.voice_id,
                            currentPlayingUrl = currentPlayingUrl,
                            isPlaying = isPlaying,
                            onSelect = {
                                viewModel.selectVoice(voice)
                            },
                            onPlay = {
                                if (currentPlayingUrl == voice.audio_url && isPlaying) {
                                    // 停止播放
                                    mediaPlayer?.stop()
                                    mediaPlayer?.release()
                                    mediaPlayer = null
                                    currentPlayingUrl = null
                                    isPlaying = false
                                } else {
                                    // 开始播放新音频
                                    mediaPlayer?.release()
                                    currentPlayingUrl = voice.audio_url
                                    isPlaying = true
                                    mediaPlayer = MediaPlayer().apply {
                                        try {
                                            setDataSource(voice.audio_url)
                                            prepareAsync()
                                            setOnPreparedListener { start() }
                                            setOnCompletionListener {
                                                release()
                                                currentPlayingUrl = null
                                                isPlaying = false
                                            }
                                            setOnErrorListener { _, _, _ ->
                                                release()
                                                currentPlayingUrl = null
                                                isPlaying = false
                                                true
                                            }
                                        } catch (e: Exception) {
                                            android.util.Log.e("VoiceManagement", "播放音频失败", e)
                                            release()
                                            currentPlayingUrl = null
                                            isPlaying = false
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 当前音色区域（包含情感选择）
 */
@Composable
fun CurrentVoiceSection(
    selectedVoice: com.danshuang.AiLive.data.model.SelectedVoiceData?,
    tempSelectedVoice: VoiceItem?,
    tempSelectedEmotion: String,
    currentPlayingUrl: String?,
    isPlaying: Boolean,
    onPlayAudio: (String) -> Unit,
    onEmotionSelected: (String) -> Unit
) {
    val displayVoice = tempSelectedVoice ?: selectedVoice?.voice_info?.let { info ->
        VoiceItem(
            voice_id = info.voice_id,
            voice_name = info.voice_name,
            voice_desc = info.voice_desc,
            voice_type = info.voice_type,
            enable_emotion = info.enable_emotion,
            emotions = info.emotions,
            audio_url = info.audio_url,
            avatar_url = info.avatar_url
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(16.dp)
    ) {
        Text(
            text = "当前音色",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        displayVoice?.let { voice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 头像
                Image(
                    painter = rememberAsyncImagePainter(voice.avatar_url),
                    contentDescription = voice.voice_name,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(16.dp))

                // 音色信息
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 音色名称和情感在同一行
                    // val emotionText = if (tempSelectedEmotion.isEmpty()) "通用" else getEmotionText(tempSelectedEmotion)
                    Text(
                        //text = "${voice.voice_name}（$emotionText）",
                        text = voice.voice_name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (voice.voice_desc.isNotEmpty()) {
                        Text(
                            text = voice.voice_desc,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // 播放按钮
                val isCurrentPlaying = currentPlayingUrl == voice.audio_url && isPlaying
                IconButton(
                    onClick = { onPlayAudio(voice.audio_url) },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFF6347), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isCurrentPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentPlaying) "停止" else "试听",
                        tint = Color.White
                    )
                }
            }
            
            // 情感选择区域
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "情感选择",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                voice.emotions.forEach { emotionItem ->
                    val emotion = emotionItem.emotion
                    val emotionDisplayText = getEmotionText(emotion)
                    val isSelected = emotion == tempSelectedEmotion
                    
                    Surface(
                        modifier = Modifier
                            .clickable { onEmotionSelected(emotion) }
                            .height(32.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF2196F3) else Color.LightGray
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = emotionDisplayText,
                                color = if (isSelected) Color.White else Color.DarkGray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        
        }
    }
}

/**
 * 音色项卡片
 */
@Composable
fun VoiceItemCard(
    voice: VoiceItem,
    isSelected: Boolean,
    isTempSelected: Boolean,
    currentPlayingUrl: String?,
    isPlaying: Boolean,
    onSelect: () -> Unit,
    onPlay: () -> Unit
) {
    // 边框颜色：使用中为橙红色，临时选中为蓝色，未选中无边框
    val borderColor = when {
        isSelected -> Color(0xFFFF6347)  // 使用中：橙红色
        isTempSelected -> Color(0xFF2196F3)  // 临时选中：蓝色
        else -> Color.Transparent
    }
    val borderWidth = if (isSelected || isTempSelected) 3.dp else 0.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(voice.avatar_url),
                    contentDescription = voice.voice_name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                // 播放按钮覆盖层
                val isCurrentPlaying = currentPlayingUrl == voice.audio_url && isPlaying
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .clickable { onPlay() }
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isCurrentPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isCurrentPlaying) "停止" else "试听",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = voice.voice_name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // 使用按钮 - 固定大小和样式
            Button(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) Color(0xFFFF6347) else Color.White,
                    contentColor = if (isSelected) Color.White else Color(0xFFFF6347)
                ),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = if (isSelected) "使用中" else "使用",
                    fontSize = 12.sp
                )
            }
        }
    }
}

/**
 * 播放音频
 */
private fun playAudio(context: android.content.Context, url: String) {
    try {
        MediaPlayer().apply {
            setDataSource(url)
            prepareAsync()
            setOnPreparedListener { start() }
            setOnCompletionListener { release() }
        }
    } catch (e: Exception) {
        android.util.Log.e("VoiceManagement", "播放音频失败", e)
    }
}

/**
 * 获取情感文本
 */
private fun getEmotionText(emotion: String): String {
    return when (emotion) {
        "" -> "通用"
        "happy" -> "开心"
        "sad" -> "悲伤"
        "angry" -> "生气"
        "surprised" -> "惊讶"
        "fear" -> "恐惧"
        "hate" -> "厌恶"
        "excited" -> "激动"
        "coldness" -> "冷漠"
        "neutral" -> "中性"
        "depressed" -> "沮丧"
        "lovey-dovey" -> "撒娇"
        "shy" -> "害羞"
        "comfort" -> "安慰鼓励"
        "tension" -> "咆哮/焦急"
        "tender" -> "温柔"
        "storytelling" -> "讲故事/自然讲述"
        "radio" -> "情感电台"
        "magnetic" -> "磁性"
        "advertising" -> "广告营销"
        "vocal-fry" -> "气泡音"
        "ASMR" -> "低语"
        "news" -> "新闻播报"
        "entertainment" -> "娱乐八卦"
        "dialect" -> "方言"
        "warm" -> "温暖"
        "affectionate" -> "深情"
        "authoritative" -> "权威"
        "chat" -> "对话/闲聊"
        else -> emotion
    }
}
