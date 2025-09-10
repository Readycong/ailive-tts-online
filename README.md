# Material 3 Compose Demo

这是一个使用Material 3 Design和Jetpack Compose构建的Android示例应用。

## 目录
- [功能特性](#功能特性)
- [项目结构](#项目结构)
- [使用的技术栈](#使用的技术栈)
- [主要组件展示](#主要组件展示)
- [开始使用](#开始使用)
- [自定义主题](#自定义主题)
- [添加新组件](#添加新组件)
- [最佳实践](#最佳实践)
- [参考资源](#参考资源)
- [服务组件](#服务组件)

## 功能特性

- ✅ **Material 3 Design**: 使用最新的Material Design 3规范
- ✅ **Jetpack Compose**: 完全使用Compose UI构建
- ✅ **动态主题**: 支持动态颜色和深浅色主题
- ✅ **Material 3组件**: 包含丰富的Material 3组件示例
- ✅ **响应式布局**: 适配不同屏幕尺寸
- ✅ **底部导航栏**: 5个菜单项（主页、知识库、AI直播、主播模型、我的）
- ✅ **知识库页面**: 分类问答管理（应用中、行业问答）
- ✅ **问答编辑功能**: 支持添加、编辑、完整显示问答

## 项目结构

```
app/
├── src/main/java/com/example/AiLive/
│   ├── MainActivity.kt              # 主Activity
│   ├── navigation/                  # 导航配置
│   │   └── AppNavigation.kt         # 底部导航栏
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt            # Material 3颜色定义
│       │   ├── Theme.kt            # Material 3主题配置
│       │   └── Type.kt             # Material 3字体样式
│       ├── components/
│       │   └── Material3Components.kt # Material 3组件示例
│       └── screens/                # 各个页面
│           ├── HomeScreen.kt       # 主页
│           ├── KnowledgeScreen.kt  # 知识库
│           ├── AILiveScreen.kt     # AI直播
│           ├── ModelScreen.kt      # 主播模型
│           └── ProfileScreen.kt    # 我的
```

## 使用的技术栈

- **Kotlin**: 2.0.21
- **Android Gradle Plugin**: 8.10.1
- **Compose BOM**: 2024.10.01
- **Material 3**: 最新稳定版
- **Activity Compose**: 1.9.3

## 主要组件展示

### 1. 主题系统
- 完整的Material 3颜色系统
- 支持动态颜色（Android 12+）
- 深浅色主题自动切换

### 2. 组件示例
- **TopAppBar**: Material 3应用栏
- **NavigationBar**: 底部导航栏
- **FloatingActionButton**: 悬浮操作按钮
- **Card**: 卡片组件
- **Button**: 主按钮和次要按钮
- **Switch**: 开关组件

### 3. 布局
- **Scaffold**: Material 3基础布局
- **Column/Row**: 灵活的布局系统
- **响应式间距**: 适配不同屏幕

## 开始使用

1. **克隆项目**
   ```bash
   git clone [项目地址]
   cd AiLive
   ```

2. **构建项目**
   ```bash
   ./gradlew build
   ```

3. **运行应用**
   ```bash
   ./gradlew installDebug
   ```

## 自定义主题

### 修改颜色
编辑 `app/src/main/java/com/example/AiLive/ui/theme/Color.kt` 文件，修改颜色值：

```kotlin
// 示例：修改主色调
val primaryLight = Color(0xFF00687A)  // 改为你的颜色
val primaryDark = Color(0xFF55D6F4)    // 深色模式颜色
```

### 修改字体
编辑 `app/src/main/java/com/example/AiLive/ui/theme/Type.kt` 文件，自定义字体样式：

```kotlin
// 示例：修改标题样式
val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,  // 改为你的字体
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    )
)
```

## 添加新组件

1. 在 `ui/components/` 目录下创建新的组件文件
2. 使用现有的主题和颜色系统
3. 在 `MainActivity.kt` 中集成新组件

## 最佳实践

- ✅ 使用Material 3主题颜色
- ✅ 遵循Material Design指南
- ✅ 提供深浅色主题支持
- ✅ 使用适当的间距和排版
- ✅ 添加组件预览

## 参考资源

- [Material 3 Design Guidelines](https://m3.material.io/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Material 3 Compose Components](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)

## 服务组件

### LiveChattingService
原有的直播语音播放服务，基于MediaPlayer实现。

### LiveChattingServiceOptimized
新的直播语音播放服务，基于SoundPool实现，具有以下优势：

- 更好的性能表现
- 支持音量、音调、速度的随机化设置
- 更流畅的音频播放体验
- 更低的资源消耗

使用方式：
```kotlin
// 启动优化版服务
LiveChattingServiceOptimized.startService(context)

// 停止服务
LiveChattingServiceOptimized.stopService(context)
```