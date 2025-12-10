# 直播语音播放服务迁移指南

## 概述

本文档说明了如何将项目中的旧版直播语音播放服务（LiveChattingService）迁移到优化版服务（LiveChattingServiceOptimized）。

## 迁移步骤

### 1. 导入语句更新

所有引用旧版服务的文件都需要更新导入语句：

```kotlin
// 旧版导入
import com.danshuang.AiLive.service.LiveChattingService

// 新版导入（使用别名保持代码兼容性）
import com.danshuang.AiLive.service.LiveChattingServiceOptimized as LiveChattingService
```

### 2. 已更新的文件列表

以下文件已完成迁移：

1. [MainActivity.kt](../../app/src/main/java/com/danshuang/AiLive/MainActivity.kt)
2. [AILiveScreen.kt](../../app/src/main/java/com/danshuang/AiLive/ui/screens/AILiveScreen.kt)
3. [HomeScreen.kt](../../app/src/main/java/com/danshuang/AiLive/ui/screens/HomeScreen.kt)
4. [AppLifecycleManager.kt](../../app/src/main/java/com/danshuang/AiLive/util/AppLifecycleManager.kt)
5. [UnauthorizedErrorHandler.kt](../../app/src/main/java/com/danshuang/AiLive/util/UnauthorizedErrorHandler.kt)

### 3. API 兼容性

新版服务保持了与旧版服务相同的公共API接口，因此无需修改调用代码：

```kotlin
// 启动服务（保持不变）
LiveChattingService.startService(context)

// 停止服务（保持不变）
LiveChattingService.stopService(context)

// 观察状态（保持不变）
val chattingState by LiveChattingService.chattingStateFlow.collectAsState()
val isServiceRunning by LiveChattingService.isServiceRunning.collectAsState()
```

## 验证迁移结果

### 1. 语法检查
所有更新的文件都已通过语法检查，未发现错误。

### 2. 功能验证
迁移后，以下功能应保持正常工作：
- AI直播语音播放的启动和停止
- 服务状态的UI显示
- 应用前后台切换时的服务行为
- 401错误处理时的服务停止
- 应用退出时的服务清理

## 回滚方案

如果需要回滚到旧版服务，只需将导入语句改回：

```kotlin
// 回滚到旧版导入
import com.danshuang.AiLive.service.LiveChattingService
```

## 优势说明

迁移到优化版服务后，将获得以下优势：

1. **更好的性能表现**：基于SoundPool实现，资源消耗更低
2. **支持音频参数随机化**：可为播放的音频随机设置音量、音调、速度
3. **并发播放支持**：支持同时播放多个音频流
4. **更低的延迟**：音频播放响应更快

## 注意事项

1. 两个服务版本可以共存，AndroidManifest.xml中已同时声明
2. 优化版服务向前兼容旧版服务的所有功能
3. 如需完全移除旧版服务，可删除[LiveChattingService.kt](../../app/src/main/java/com/danshuang/AiLive/service/LiveChattingService.kt)文件并从AndroidManifest.xml中移除相关声明