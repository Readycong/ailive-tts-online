# AI直播页面消息插入接口使用示例

## 接口说明

`LiveMessageManager` 提供了一个全局的消息插入接口，可以在应用的任何地方调用来向直播间动态信息列表插入新消息。

## 使用方法

### 1. 插入消息

```kotlin
// 插入观众评论
LiveMessageManager.insertMessage(
    type = MessageType.COMMENT,
    content = "这个直播内容真的很有趣！",
    userName = "用户小明"
)

// 插入礼物信息
LiveMessageManager.insertMessage(
    type = MessageType.GIFT,
    content = "送出了一个跑车",
    userName = "土豪用户"
)

// 插入AI回复
LiveMessageManager.insertMessage(
    type = MessageType.AI_RESPONSE,
    content = "感谢大家的支持，接下来为大家带来更精彩的内容！",
    userName = "AI主播"
)

// 插入系统消息（无用户名）
LiveMessageManager.insertMessage(
    type = MessageType.SYSTEM,
    content = "直播间在线人数突破500人"
)
```

### 2. 消息类型

- `COMMENT`: 观众评论
- `GIFT`: 礼物信息  
- `FOLLOW`: 关注信息
- `ENTER`: 进入直播间
- `SYSTEM`: 系统消息
- `AI_RESPONSE`: AI回复

### 3. 清空消息

```kotlin
// 清空所有消息
LiveMessageManager.clearMessages()
```

### 4. 获取消息列表

```kotlin
// 获取当前所有消息
val messages = LiveMessageManager.messages
```

## 特性

- **实时刷新**: 新消息会立即显示在列表中
- **自动滚动**: 新消息插入后，列表会自动滚动到最新消息
- **消息限制**: 最多保持100条消息，超过时会自动删除最旧的消息
- **类型分类**: 不同类型的消息有不同的颜色和图标
- **全局访问**: 可以在应用的任何地方调用插入接口

## 界面说明

- **上半部分**: 两个开关控制直播间监控状态和AI直播状态
- **下半部分**: 实时显示的消息列表，新消息出现在底部
- **无底部导航栏**: 页面专注于直播监控功能，不显示底部导航

这个设计使得其他模块可以轻松地向直播间动态信息流中插入消息，提供统一的信息展示体验。