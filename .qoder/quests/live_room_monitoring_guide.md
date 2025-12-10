# 直播间监控服务使用说明

## 功能概述

本系统实现了一个完整的直播间监控服务，能够在后台实时监控直播间状态变化，通过WebSocket连接获取直播间动态信息，并在AI直播监控界面中实时展示。

## 核心特性

### 1. 后台监控服务
- **自动启动**: 应用启动时自动开启直播间监控服务
- **前台服务**: 使用前台服务确保持续运行，不被系统杀死
- **状态管理**: 实时监控服务状态(0=未监控, 1=监控中)

### 2. WebSocket连接管理
- **动态配置**: WebSocket地址通过API动态获取
- **自动重连**: 连接断开时自动重连，支持指数退避算法
- **心跳机制**: 定期发送心跳包维持连接
- **状态监控**: 实时显示连接状态(连接中/已连接/已断开/连接错误)

### 3. 消息处理与展示
- **多类型消息**: 支持直播开始/结束、观众互动、系统通知等多种消息类型
- **实时展示**: 直播动态信息在AI直播监控界面实时显示
- **消息管理**: 自动滚动显示最新消息，支持手动滚动控制

### 4. 数据持久化
- **本地存储**: 直播间信息、配置和日志本地持久化
- **API集成**: 与服务端API集成，获取最新配置和直播间详情
- **状态同步**: 服务状态和错误信息实时同步

## 系统架构

```
[AI直播监控界面] 
       ↓
[直播间监控服务] 
       ↓
[WebSocket管理器] ← → [WebSocket服务器]
       ↓
[消息处理器]
       ↓
[直播间数据仓库] ← → [API服务]
       ↓
[本地数据库]
```

## 使用方法

### 1. 启动监控
- 打开应用后，进入"AI直播"页面
- 应用会自动启动后台监控服务
- 监控服务会获取WebSocket配置并建立连接

### 2. 控制监控状态
- **直播间监控开关**: 控制后台监控服务的启动/停止
- **AI直播开关**: 控制AI模拟消息的生成
- 开关状态实时反映服务运行状态

### 3. 查看连接状态
- 连接状态显示在监控开关下方
- 包含四种状态：已连接、连接中、已断开、连接错误
- 错误时会显示详细错误信息

### 4. 查看直播动态
- 实时动态列表显示所有监控到的消息
- 支持多种消息类型：
  - 观众评论
  - 礼物消息
  - 关注消息
  - 进入直播间
  - 系统消息
  - AI主播回复

### 5. 消息交互
- 列表自动滚动显示最新消息
- 手动滚动时暂停自动滚动
- 滚动到底部时恢复自动滚动
- 提供"滚动到底部"快捷按钮

## 技术实现

### 1. 核心组件

#### LiveRoomPullingService (监控服务)
- 继承Android Service，在后台运行
- 管理WebSocket连接生命周期
- 处理服务状态变化通知
- 实现自动重连和配置更新

#### WebSocketManager (连接管理器)
- 基于OkHttp实现WebSocket连接
- 支持自动重连和指数退避
- 处理连接状态变化回调
- 实现心跳机制和消息发送

#### MessageProcessor (消息处理器)
- 解析JSON格式的WebSocket消息
- 处理不同类型的直播间事件
- 调用API获取详细信息
- 更新UI显示和本地存储

#### LiveRoomRepository (数据仓库)
- 管理本地数据存储(DataStore)
- 集成API服务调用
- 提供数据访问接口
- 实现缓存和同步策略

### 2. 数据模型

```kotlin
// 直播间信息
data class LiveRoomInfo(
    val roomId: String,
    val hostName: String,
    val title: String,
    val viewerCount: Int,
    val status: RoomStatus,
    // ... 其他字段
)

// WebSocket配置
data class WebSocketConfig(
    val websocketUrl: String,
    val isActive: Boolean
)

// 服务状态
data class ServiceState(
    val pullingState: Int,
    val lastWebSocketUrl: String?,
    val errorMessage: String?
)
```

### 3. API接口

#### 获取WebSocket配置
```
GET /api/live/websocket/config
Response: {
    "websocketUrl": "wss://...",
    "isActive": true
}
```

#### 获取直播间详情
```
GET /api/live/room/details?roomId=xxx
Response: {
    "roomId": "xxx",
    "hostInfo": {...},
    "roomInfo": {...}
}
```

## 配置说明

### 1. WebSocket消息格式

```json
{
    "type": "live_start|live_end|viewer_update|comment|gift|follow|enter|system",
    "data": {
        // 根据消息类型包含不同字段
        "roomId": "直播间ID",
        "hostName": "主播名称",
        "userName": "用户名称",
        "content": "消息内容",
        "viewerCount": 观众数量
    },
    "timestamp": 时间戳
}
```

### 2. 权限配置

AndroidManifest.xml中需要的权限：
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
```

### 3. 服务声明

```xml
<service
    android:name=".service.LiveRoomPullingService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="dataSync" />
```

## 故障排除

### 1. 连接问题
- **现象**: 显示"连接错误"状态
- **原因**: WebSocket服务器不可达或URL配置错误
- **解决**: 检查网络连接和服务器状态，确认API返回正确的WebSocket URL

### 2. 消息不显示
- **现象**: 监控已开启但没有消息显示
- **原因**: WebSocket消息格式不正确或处理异常
- **解决**: 检查消息格式是否符合预期，查看日志排查处理错误

### 3. 服务自动停止
- **现象**: 监控状态异常变为"未监控"
- **原因**: 系统资源不足或服务被杀死
- **解决**: 确保应用有足够权限，检查系统电源管理设置

### 4. 重连失败
- **现象**: 连接断开后无法自动重连
- **原因**: 达到最大重连次数或网络持续异常
- **解决**: 手动重启监控或检查网络稳定性

## 性能优化

### 1. 内存管理
- 限制消息历史记录数量(最多1000条)
- 定期清理过期的监控日志
- 使用对象池减少GC压力

### 2. 网络优化
- 实现指数退避重连策略
- 设置合适的连接超时时间
- 优化心跳频率

### 3. UI优化
- 使用虚拟化列表减少渲染开销
- 实现智能滚动控制
- 异步处理消息更新

## 监控指标

### 1. 服务状态
- 监控服务运行时长
- WebSocket连接稳定性
- 重连成功率

### 2. 消息处理
- 消息处理延迟
- 消息丢失率
- 错误处理次数

### 3. 资源使用
- 内存使用量
- CPU使用率
- 网络流量

## 扩展功能

### 1. 多平台支持
- 支持抖音、快手等多个直播平台
- 统一的消息格式和处理逻辑
- 平台特定的配置管理

### 2. 高级过滤
- 消息类型过滤
- 关键词过滤
- 用户级别过滤

### 3. 数据分析
- 直播间热度统计
- 观众互动分析
- 实时数据导出

## 注意事项

1. **网络权限**: 确保应用有网络访问权限
2. **后台运行**: 需要相关权限以保证服务持续运行
3. **电池优化**: 建议在系统设置中关闭电池优化
4. **存储空间**: 监控日志会占用一定存储空间，定期清理
5. **隐私安全**: 遵守相关平台的API使用规范