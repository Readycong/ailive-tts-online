# 直播语音播放服务使用示例

## 概述

本文档展示了如何在项目中使用新的基于SoundPool的直播语音播放服务优化版。

## 服务启动与停止

### 启动服务

```kotlin
// 在Activity或Fragment中启动优化版服务
LiveChattingServiceOptimized.startService(this)
```

### 停止服务

```kotlin
// 在Activity或Fragment中停止服务
LiveChattingServiceOptimized.stopService(this)
```

## 状态监听

### 观察播放状态

```kotlin
// 在ViewModel或Activity中观察播放状态
viewModelScope.launch {
    LiveChattingServiceOptimized.chattingStateFlow.collect { state ->
        when (state) {
            LiveChattingServiceOptimized.CHATTING_STATE_STOPPED -> {
                // 更新UI显示停止状态
                // 例如：隐藏播放指示器
            }
            LiveChattingServiceOptimized.CHATTING_STATE_PLAYING -> {
                // 更新UI显示播放状态
                // 例如：显示播放指示器
            }
        }
    }
}
```

### 观察服务运行状态

```kotlin
// 在ViewModel或Activity中观察服务运行状态
viewModelScope.launch {
    LiveChattingServiceOptimized.isServiceRunning.collect { isRunning ->
        if (isRunning) {
            // 服务正在运行，更新UI
            // 例如：显示"服务运行中"提示
        } else {
            // 服务未运行，更新UI
            // 例如：显示"服务已停止"提示
        }
    }
}
```

## 语音设置配置

### 更新语音设置

语音设置通过DataStore进行管理，当设置更新时，服务会自动接收更新：

```kotlin
// 在设置页面更新语音设置
// 通过发送广播通知服务重新加载设置
val intent = Intent("com.danshuang.AiLive.VOICE_SETTINGS_CHANGED")
context.sendBroadcast(intent)
```

### 语音设置数据结构

```kotlin
data class VoiceSettingsData(
    val volumeRange: RangeValue,  // 音量范围
    val pitchRange: RangeValue,   // 音调范围
    val speedRange: RangeValue,   // 速度范围
    val silenceRange: RangeValue  // 静音时间范围
)

data class RangeValue(
    val min: Float,  // 最小值
    val max: Float   // 最大值
)
```

## 集成到现有项目

### 1. 替换服务实现

如果要完全替换原有的服务实现，需要修改服务启动和停止的地方：

```kotlin
// 原有代码
// LiveChattingService.startService(this)

// 新代码
LiveChattingServiceOptimized.startService(this)
```

```kotlin
// 原有代码
// LiveChattingService.stopService(this)

// 新代码
LiveChattingServiceOptimized.stopService(this)
```

### 2. 保持兼容性

如果需要同时支持两个版本的服务，可以添加配置选项：

```kotlin
// 在应用设置中添加选项
val useOptimizedService = preferences.getBoolean("use_optimized_voice_service", false)

if (useOptimizedService) {
    LiveChattingServiceOptimized.startService(this)
} else {
    LiveChattingService.startService(this)
}
```

## 性能监控

### 日志输出

服务会输出详细的日志信息，便于调试和监控：

```kotlin
// 关键日志信息包括：
// - 服务启动/停止
// - 网络状态变化
// - 音频文件加载
// - 音频播放状态
// - 错误信息
```

### 性能对比

可以通过以下方式对比两个版本的性能：

1. **内存使用**：监控应用内存占用情况
2. **CPU使用率**：监控音频播放时的CPU消耗
3. **电池消耗**：长时间运行下的电池消耗对比
4. **播放延迟**：音频从触发到实际播放的时间

## 故障排除

### 常见问题

1. **音频无法播放**
   - 检查网络连接状态
   - 确认音频文件URL有效性
   - 检查音频文件格式是否支持

2. **参数设置无效**
   - 确认DataStore中的设置已正确保存
   - 检查广播是否正确发送
   - 验证服务是否重新加载了设置

3. **服务无法启动**
   - 检查AndroidManifest.xml中服务声明
   - 确认必要的权限已添加
   - 查看日志输出中的错误信息

### 调试技巧

```kotlin
// 启用详细日志
// 在Logcat中过滤标签"LiveChattingServiceOpt"

// 检查服务状态
val isRunning = LiveChattingServiceOptimized.isServiceRunning.value
val playState = LiveChattingServiceOptimized.chattingStateFlow.value
```

## 最佳实践

1. **资源管理**：服务会在适当时候自动释放资源，无需手动干预
2. **错误处理**：服务内置了完善的错误处理机制
3. **网络状态**：服务会自动检测网络状态并相应调整行为
4. **用户体验**：通过前台服务确保在后台也能正常播放
5. **配置更新**：支持动态更新语音设置，无需重启服务