package com.danshuang.AiLive.ui.screens

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.content.Context
import android.media.AudioManager
import android.media.AudioAttributes
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
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.model.VoiceFileInfo
import android.util.Log
import java.io.File
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.platform.LocalContext
import java.lang.Exception
// 添加必要的导入语句
import android.provider.Settings
import android.content.Intent
import android.net.Uri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCloningScreen(
    navController: NavigationState,
    serverApiUtils: ServerApiUtilsV2
) {
    // 获取LocalContext
    val context = LocalContext.current
    // 获取CoroutineScope
    val coroutineScope = rememberCoroutineScope()
    
    // 添加SnackbarHostState
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 状态变量
    var hasRecordedAudio by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isClonedVoicePlaying by remember { mutableStateOf(false) } // 新增状态变量
    // 新增状态变量
    var recordingStartTime by remember { mutableStateOf<Long>(0) }
    var recordedDuration by remember { mutableStateOf<Long>(0) }
    var isRecording by remember { mutableStateOf(false) }
    var voiceFileInfo by remember { mutableStateOf<VoiceFileInfo?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var tempRecordingFile by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var clonedVoiceMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) } // 新增MediaPlayer实例
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var isUploading by remember { mutableStateOf(false) }
    var uploadMessage by remember { mutableStateOf<String?>(null) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    
    // 页面加载时获取语音文件信息
    LaunchedEffect(Unit) {
        try {
            val response = serverApiUtils.getMyVoice()
            if (response.isSuccess) {
                // 解析响应数据并更新状态
                val data = response.data
                if (data != null) {
                    voiceFileInfo = VoiceFileInfo(
                        id = data["id"]?.toString() ?: "",
                        text = data["name"]?.toString() ?: "默认声音",
                        url = data["url"]?.toString() ?: "",
                        cache = false
                    )
                }
            } else {
                errorMessage = response.msg
            }
        } catch (e: Exception) {
            errorMessage = "获取语音文件失败: ${e.message}"
        } finally {
            isLoading = false
        }
    }


    // 录音时长计时器协程
    LaunchedEffect(isRecording) {
        if (isRecording) {
            recordingStartTime = System.currentTimeMillis()
            recordedDuration = 0
            while (isRecording) {
                kotlinx.coroutines.delay(1000) // 每秒更新一次
                recordedDuration = System.currentTimeMillis() - recordingStartTime
            }
        }
    }

    // 显示权限请求对话框
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("需要录音权限") },
            text = { Text("此功能需要录音权限来录制您的声音。请在设置中授予录音权限。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        // 打开应用设置页面
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", context.packageName, null)
                        intent.data = uri
                        context.startActivity(intent)
                    }
                ) {
                    Text("去设置")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showPermissionDialog = false }
                ) {
                    Text("取消")
                }
            }
        )
    }
    
    // 显示错误信息的Snackbar
    errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            errorMessage = null
        }
    }
    
    // 显示上传结果的Snackbar
    uploadMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            uploadMessage = null
        }
    }
    
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
        },
        // 添加snackbarHost
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
            ClonedVoiceCard(
                voiceFileInfo = voiceFileInfo,
                isLoading = isLoading,
                isPlaying = isClonedVoicePlaying,
                onPlayClick = { url ->
                    // 停止可能正在播放的音频
                    stopAudio(clonedVoiceMediaPlayer, { clonedVoiceMediaPlayer = it }, { isClonedVoicePlaying = it })
                    // 播放音频
                    playAudio(context, { clonedVoiceMediaPlayer = it }, url, { isClonedVoicePlaying = it })
                },
                onStopClick = {
                    // 停止播放
                    stopAudio(clonedVoiceMediaPlayer, { clonedVoiceMediaPlayer = it }, { isClonedVoicePlaying = it })
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 录音按钮
            RecordingButton(
                isRecording = isRecording,
                recordedDuration = recordedDuration,
                onRecordingStateChange = { recording ->
                    isRecording = recording
                },
                onStartRecording = {
                    // 检查录音权限
                    if (checkAudioPermission(context)) {
                        startRecording(context, { mediaRecorder = it }, { tempRecordingFile = it })
                    } else {
                        // 显示权限请求对话框
                        showPermissionDialog = true
                        android.util.Log.e("VoiceCloningScreen", "Missing audio recording permission")
                    }
                },
                onStopRecording = {
                    stopRecording(mediaRecorder, { mediaRecorder = null }) { 
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
                    onClick = {
                        if (isPlaying) {
                            // 当前正在播放，需要暂停
                            pauseAudio(mediaPlayer) { isPlaying = it }
                        } else {
                            // 当前未播放，需要开始播放
                            tempRecordingFile?.let { filePath ->
                                // 检查文件是否存在
                                val file = java.io.File(filePath)
                                if (file.exists()) {
                                    playAudio(context, { mediaPlayer = it }, filePath, { isPlaying = it }, mediaPlayer)
                                } else {
                                    // 文件不存在，显示错误信息
                                    errorMessage = "录音文件不存在，请先录制音频"
                                }
                            } ?: run {
                                errorMessage = "未录制音频文件"
                            }
                        }
                    },
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
                    onClick = {
                        tempRecordingFile?.let { filePath ->
                            isUploading = true
                            uploadMessage = null
                            uploadRecording(serverApiUtils, filePath) { success, message ->
                                isUploading = false
                                if (success) {
                                    uploadMessage = message
                                } else {
                                    errorMessage = message
                                }
                            }
                        } ?: run {
                            isUploading = false
                            errorMessage = "未录制音频文件"
                        }
                    },
                    enabled = hasRecordedAudio && !isRecording && !isPlaying && !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "上传",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isUploading) "上传中..." else "上传")
                }
            }
        }
    }
}

@Composable
fun ClonedVoiceCard(
    voiceFileInfo: VoiceFileInfo?,
    isLoading: Boolean,
    isPlaying: Boolean,
    onPlayClick: (String) -> Unit,
    onStopClick: () -> Unit
) {
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
            
            if (isLoading) {
                // 显示加载状态
                CircularProgressIndicator()
            } else {
                voiceFileInfo?.let { fileInfo ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = fileInfo.text, // 显示语音名称
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        IconButton(
                            onClick = { 
                                if (isPlaying) {
                                    onStopClick()
                                } else {
                                    onPlayClick(fileInfo.url)
                                }
                            }, // 点击播放/停止音频
                            modifier = Modifier
                                .size(40.dp)
                                .border(
                                    width = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "停止" else "播放",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingButton(
    isRecording: Boolean,
    recordedDuration: Long,
    onRecordingStateChange: (Boolean) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit
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
                            onStartRecording()
                            tryAwaitRelease()
                            onRecordingStateChange(false)
                            onStopRecording()
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = formatDuration(recordedDuration),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                )

                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "录音",
                    tint = backgroundColor,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        Text(
            text = if (isRecording) "录音中..." else "长按开始录音",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// 开始录音
fun startRecording(
    context: android.content.Context,
    setMediaRecorder: (android.media.MediaRecorder) -> Unit,
    setTempFile: (String) -> Unit
) {
    try {
        // 检查是否有录音权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.util.Log.e("VoiceCloningScreen", "No audio recording permission")
                return
            }
        }
        
        // 创建新的MediaRecorder实例
        val recorder = android.media.MediaRecorder()
        setMediaRecorder(recorder)
        
        // 配置音频录制参数
        recorder.setAudioSource(android.media.MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(android.media.MediaRecorder.OutputFormat.THREE_GPP)
        recorder.setAudioEncoder(android.media.MediaRecorder.AudioEncoder.AMR_NB)
        
        // 创建临时文件路径
        val fileName = "temp_recording_${System.currentTimeMillis()}.3gp"
        val file = java.io.File(context.cacheDir, fileName)
        val filePath = file.absolutePath
        recorder.setOutputFile(filePath)
        
        // 准备并开始录制
        recorder.prepare()
        recorder.start()
        
        // 保存文件路径
        setTempFile(filePath)
        android.util.Log.d("VoiceCloningScreen", "Started recording to: $filePath")
    } catch (e: Exception) {
        // 处理录制错误
        android.util.Log.e("VoiceCloningScreen", "Error starting recording: ${e.message}", e)
        e.printStackTrace()
    }
}

// 停止录音
fun stopRecording(
    mediaRecorder: android.media.MediaRecorder?,
    resetMediaRecorder: () -> Unit,
    onStopComplete: () -> Unit
) {
    try {
        mediaRecorder?.apply {
            stop()
            release()
            android.util.Log.d("VoiceCloningScreen", "Stopped recording")
        }
        resetMediaRecorder()
        onStopComplete()
    } catch (e: Exception) {
        // 处理停止录制错误
        android.util.Log.e("VoiceCloningScreen", "Error stopping recording: ${e.message}", e)
        e.printStackTrace()
        resetMediaRecorder()
        onStopComplete()
    }
}

// 播放音频
fun playAudio(
    context: android.content.Context,
    setMediaPlayer: (android.media.MediaPlayer?) -> Unit,
    filePath: String,
    onPlayStateChange: (Boolean) -> Unit,
    mediaPlayer: android.media.MediaPlayer? = null  // 可选参数，用于传递已存在的MediaPlayer实例
) {
    try {
        // 如果传入了MediaPlayer实例且处于暂停状态，则恢复播放
        if (mediaPlayer != null && !mediaPlayer.isPlaying) {
            mediaPlayer.start()
            onPlayStateChange(true)
            return
        }
        
        // 检查是否为网络URL
        val isNetworkUrl = filePath.startsWith("http://") || filePath.startsWith("https://")
        
        // 只有本地文件才需要检查是否存在
        if (!isNetworkUrl) {
            val file = java.io.File(filePath)
            if (!file.exists()) {
                android.util.Log.e("VoiceCloningScreen", "Audio file not found: $filePath")
                onPlayStateChange(false)
                return
            }
        }
        
        // 创建新的MediaPlayer实例
        val player = android.media.MediaPlayer()
        setMediaPlayer(player)
        
        // 设置音频属性（统一使用AudioAttributes，避免使用被弃用的setAudioStreamType）
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .build()
        player.setAudioAttributes(audioAttributes)
        
        // 设置数据源
        if (isNetworkUrl) {
            // 网络URL
            player.setDataSource(context, android.net.Uri.parse(filePath))
        } else {
            // 本地文件
            player.setDataSource(filePath)
        }
        
        // 设置准备完成监听器
        player.setOnPreparedListener { mp ->
            mp.start()
            onPlayStateChange(true)
        }
        
        // 设置播放完成监听器
        player.setOnCompletionListener { mp ->
            mp.release()
            setMediaPlayer(null)
            onPlayStateChange(false)
        }
        
        // 设置错误监听器
        player.setOnErrorListener { mp, what, extra ->
            android.util.Log.e("VoiceCloningScreen", "MediaPlayer error: what=$what, extra=$extra")
            mp.release()
            setMediaPlayer(null)
            onPlayStateChange(false)
            false
        }
        
        // 异步准备播放器
        player.prepareAsync()
    } catch (e: Exception) {
        // 处理播放错误
        android.util.Log.e("VoiceCloningScreen", "Error playing audio: ${e.message}", e)
        setMediaPlayer(null)
        onPlayStateChange(false)
    }
}

// 暂停音频
fun pauseAudio(
    mediaPlayer: android.media.MediaPlayer?,
    onPlayStateChange: (Boolean) -> Unit
) {
    try {
        mediaPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
                onPlayStateChange(false)
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("VoiceCloningScreen", "Error pausing audio: ${e.message}", e)
        onPlayStateChange(false)
    }
}

// 停止音频
fun stopAudio(
    mediaPlayer: android.media.MediaPlayer?,
    setMediaPlayer: (android.media.MediaPlayer?) -> Unit,
    onPlayStateChange: (Boolean) -> Unit
) {
    mediaPlayer?.let { player ->
        try {
            if (player.isPlaying) {
                player.stop()
            }
            player.release()
            setMediaPlayer(null)
            onPlayStateChange(false)
        } catch (e: Exception) {
            android.util.Log.e("VoiceCloningScreen", "Error stopping audio: ${e.message}", e)
            setMediaPlayer(null)
            onPlayStateChange(false)
        }
    }
}

// 上传录音文件
fun uploadRecording(
    serverApiUtils: com.danshuang.AiLive.api.ServerApiUtilsV2,
    filePath: String,
    onUploadResult: (Boolean, String) -> Unit
) {
    // 启动协程处理上传
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
        try {
            val response = serverApiUtils.uploadAudio(filePath)
            if (response.isSuccess) {
                // 上传成功
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onUploadResult(true, "上传成功")
                }
            } else {
                // 上传失败
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onUploadResult(false, "上传失败: ${response.msg}")
                }
            }
        } catch (e: Exception) {
            // 网络异常
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onUploadResult(false, "上传异常: ${e.message}")
            }
        }
    }
}

// 检查录音权限
fun checkAudioPermission(context: android.content.Context): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true // 在Android 6.0之前，权限在安装时就已经授予
    }
}

// 请求录音权限
fun requestAudioPermission(activity: android.app.Activity) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
        activity.requestPermissions(
            arrayOf(android.Manifest.permission.RECORD_AUDIO),
            1001 // 请求码
        )
    }
}

// 格式化时长
fun formatDuration(duration: Long): String {
    val seconds = (duration / 1000) % 60
    val minutes = (duration / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun VoiceCloningScreenPreview() {
    AiLiveTheme {
        VoiceCloningScreen(
            navController = NavigationState(),
            serverApiUtils = TODO("Provide ServerApiUtilsV2 instance")
        )
    }
}