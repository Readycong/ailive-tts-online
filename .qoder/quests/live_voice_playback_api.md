# 直播语音播放服务API文档

## 概述
本文档描述了直播语音播放服务相关的API接口和数据模型。

## 数据模型

### VoiceFileInfo
语音文件信息数据模型，用于表示需要播放的音频文件信息。

```kotlin
data class VoiceFileInfo(
    val id: String,
    val text: String,
    val url: String,
    val cache: Boolean = false
)
```

字段说明：
- `id`: 语音文件唯一标识符
- `text`: 语音文件对应的文本内容
- `url`: 语音文件的URL地址
- `cache`: 是否需要缓存该文件

### AiComment (保持不变)
原有的AI评论数据模型保持不变，用于其他场景。

```kotlin
data class AiComment(
    val content: String,
    val type: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

## API接口

### fetchAiKnowledges
获取播放音频内容，用于除COMMENT阶段外的所有播放阶段。

**方法签名：**
```kotlin
suspend fun fetchAiKnowledges(
    roomId: String,
    stage: String
): VoiceFileInfo?
```

**参数说明：**
- `roomId`: 直播间ID
- `stage`: 播放阶段名称（WELCOME, ROOM, PRODUCT, KEEP, ORDER, THANKS）

**返回值：**
- `VoiceFileInfo?`: 语音文件信息对象，如果获取失败则返回null

**使用示例：**
```kotlin
val voiceFile = serverApiUtils.fetchAiKnowledges("room_123", "WELCOME")
voiceFile?.let {
    // 处理语音文件播放
}
```

### fetchAiComments
获取AI评论音频内容列表，专门用于COMMENT播放阶段。

**方法签名：**
```kotlin
suspend fun fetchAiComments(
    roomId: String,
    step: String
): List<VoiceFileInfo>?
```

**参数说明：**
- `roomId`: 直播间ID
- `step`: 步骤名称，对于评论阶段固定为"comment"

**返回值：**
- `List<VoiceFileInfo>?`: 语音文件信息对象列表，如果获取失败则返回null

**使用示例：**
```kotlin
val voiceFiles = serverApiUtils.fetchAiComments("room_123", "comment")
voiceFiles?.forEach { voiceFile ->
    // 处理每个语音文件的播放
}
```

## 服务组件

### LiveChattingService
Android后台服务，负责管理语音播放流程。

### FileDownloader
文件下载和缓存管理组件，负责语音文件的下载和本地缓存管理。

## 播放流程
播放阶段按以下顺序循环进行：
```
WELCOME -> ROOM -> PRODUCT -> KEEP -> ORDER -> THANKS -> COMMENT -> WELCOME (循环)
```

除了COMMENT阶段获取音频列表外，其他阶段均获取单个音频文件。