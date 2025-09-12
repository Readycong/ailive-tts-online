# VoiceCloningScreen 播放控制分离设计文档

## 1. 概述

本设计文档旨在解决VoiceCloningScreen中播放控制的问题。当前实现中，ClonedVoiceCard的IconButton播放和录音播放按钮使用了同一个isPlaying状态变量，这会导致播放控制冲突。此外，IconButton的播放/停止图标没有根据播放状态切换，且在播放中再次点击IconButton不能立即停止播放。

## 2. 问题分析

### 2.1 当前实现问题

1. **状态共享问题**：ClonedVoiceCard中的IconButton播放和录音播放按钮使用了同一个`isPlaying`状态变量
2. **图标切换问题**：IconButton的播放/停止图标没有根据播放状态切换
3. **播放控制问题**：在播放中再次点击IconButton不能立即停止播放

### 2.2 代码位置

文件路径：`d:\Qoder_projects\ailive\app\src\main\java\com\danshuang\AiLive\ui\screens\VoiceCloningScreen.kt`

## 3. 设计方案

### 3.1 状态分离

为了解决状态共享问题，我们需要为ClonedVoiceCard的播放控制创建独立的状态管理：

1. 创建一个新的状态变量`isClonedVoicePlaying`用于管理ClonedVoiceCard的播放状态
2. 创建一个新的MediaPlayer实例`clonedVoiceMediaPlayer`用于ClonedVoiceCard的音频播放

### 3.2 图标切换实现

修改ClonedVoiceCard中的IconButton，根据播放状态显示不同的图标：
- 播放状态时显示停止图标（Close）
- 停止状态时显示播放图标（PlayArrow）

### 3.3 播放控制优化

实现立即停止播放功能：
1. 当点击播放按钮时，如果已经有音频在播放，则先停止当前播放
2. 当点击停止按钮时，立即停止播放并释放资源

## 4. 实现细节

### 4.1 状态变量添加

在VoiceCloningScreen的Composable函数中添加以下状态变量：

```kotlin
var hasRecordedAudio by remember { mutableStateOf(false) }
var isPlaying by remember { mutableStateOf(false) }
var isClonedVoicePlaying by remember { mutableStateOf(false) }
// 新增状态变量
var recordingStartTime by remember { mutableStateOf<Long>(0) }
var recordedDuration by remember { mutableStateOf<Long>(0) }
var isRecording by remember { mutableStateOf(false) }
var voiceFileInfo by remember { mutableStateOf<VoiceFileInfo?>(null) }
var isLoading by remember { mutableStateOf(true) }
var errorMessage by remember { mutableStateOf<String?>(null) }
var tempRecordingFile by remember { mutableStateOf<String?>(null) }
var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
var clonedVoiceMediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
var isUploading by remember { mutableStateOf(false) }
var uploadMessage by remember { mutableStateOf<String?>(null) }
var showPermissionDialog by remember { mutableStateOf(false) }
```

### 4.2 ClonedVoiceCard修改

修改ClonedVoiceCard组件，添加独立的播放控制：

```kotlin
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
```

### 4.3 播放函数优化

修改playAudio函数，支持立即停止播放：

```kotlin
fun playAudio(
    context: android.content.Context,
    setMediaPlayer: (android.media.MediaPlayer?) -> Unit,
    filePath: String,
    onPlayStateChange: (Boolean) -> Unit,
    mediaPlayer: android.media.MediaPlayer? = null
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
```

### 4.4 停止函数实现

添加stopAudio函数用于立即停止播放：

```kotlin
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
```

## 5. 数据模型

### 5.1 状态变量表

| 变量名 | 类型 | 用途 |
|--------|------|------|
| isPlaying | Boolean | 管理录音播放按钮的播放状态 |
| isClonedVoicePlaying | Boolean | 管理ClonedVoiceCard的播放状态 |
| mediaPlayer | MediaPlayer? | 录音播放的MediaPlayer实例 |
| clonedVoiceMediaPlayer | MediaPlayer? | ClonedVoiceCard播放的MediaPlayer实例 |

### 5.2 状态管理说明

通过分离状态变量，我们可以实现以下功能：
1. **独立控制**：录音播放和克隆声音播放可以独立控制，互不影响
2. **资源隔离**：每个播放功能使用独立的MediaPlayer实例，避免资源冲突
3. **状态同步**：UI状态与实际播放状态保持同步，提供准确的用户反馈

## 6. UI组件修改

### 6.1 ClonedVoiceCard组件

修改ClonedVoiceCard组件以支持独立的播放控制：

1. 添加isPlaying参数用于显示播放状态
2. 添加onStopClick参数用于处理停止播放事件
3. 根据播放状态切换图标显示

### 6.2 IconButton修改

IconButton的修改已经包含在ClonedVoiceCard组件的完整实现中。关键修改点包括：

1. 根据播放状态切换图标：
   - 播放状态时显示停止图标（Icons.Default.Close）
   - 停止状态时显示播放图标（Icons.Default.PlayArrow）

2. 根据播放状态切换点击事件：
   - 播放状态时点击调用onStopClick()
   - 停止状态时点击调用onPlayClick()

3. 添加适当的contentDescription以提高可访问性

## 7. 测试策略

### 7.1 单元测试

1. 测试状态变量的独立性
2. 测试播放/停止功能的正确性
3. 测试图标切换的正确性

### 7.2 集成测试

1. 测试同时播放两个音频时的正确行为
2. 测试播放过程中切换页面的资源释放
3. 测试异常情况下的资源释放

## 8. 后向兼容性

本修改不会影响现有功能，仅优化了播放控制的独立性和用户体验。

## 9. 风险评估

### 9.1 潜在风险

1. 资源泄漏：需要确保MediaPlayer实例正确释放
2. 状态同步：需要确保UI状态与实际播放状态一致

### 9.2 缓解措施

1. 在组件销毁时释放MediaPlayer资源
2. 添加错误处理机制确保状态一致性

## 10. 实施计划

### 10.1 第一阶段：状态分离
- 添加独立的状态变量和MediaPlayer实例
- 修改ClonedVoiceCard组件
- 更新VoiceCloningScreen中ClonedVoiceCard的调用方式

### 10.2 第二阶段：图标切换
- 实现根据播放状态切换图标
- 优化UI显示效果

### 10.3 第三阶段：播放控制优化
- 实现立即停止播放功能
- 添加资源管理机制
- 更新VoiceCloningScreen中ClonedVoiceCard的调用方式

### 10.4 具体实现示例

在VoiceCloningScreen中更新ClonedVoiceCard的调用方式：

```kotlin
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
```

## 11. 总结

通过本次设计，我们成功解决了VoiceCloningScreen中播放控制的问题：

1. **状态分离**：为ClonedVoiceCard创建了独立的播放状态管理，避免了与录音播放按钮的状态冲突
2. **图标切换**：实现了根据播放状态自动切换播放/停止图标的功能
3. **即时控制**：实现了点击按钮立即停止播放的功能
4. **资源管理**：通过独立的MediaPlayer实例确保了资源的正确管理和释放

这些改进将显著提升用户体验，使播放控制更加直观和可靠。