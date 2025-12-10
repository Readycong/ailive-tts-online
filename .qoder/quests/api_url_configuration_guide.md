# API URL 配置最佳实践

## 概述

> **注意**: 本文档主要介绍API URL配置，与直播语音播放服务相关的配置请参考 [直播语音播放服务优化版API文档](live_voice_playback_optimized_api.md) 和 [直播语音播放服务使用示例](live_voice_playback_usage_example.md)。

根据项目规范要求，API基础URL现在统一配置在 `strings.xml` 中，所有相关代码都已更新以支持从配置文件读取。

## 配置结构

### strings.xml 配置
```xml
<resources>
    <!-- 应用基础信息 -->
    <string name="app_name">Ai主播V2</string>
    <string name="app_logo">https://www.huichuang100.com/android/v2/img/ai-logo-tr.png</string>
    
    <!-- API 服务器配置 -->
    <string name="api_base_url1">http://192.168.0.101/index.php/api</string>     <!-- 开发环境 -->
    <string name="api_base_url2">https://www.huichuang100.com/devapi</string>    <!-- 测试环境 -->
    <string name="api_base_url">https://www.huichuang100.com/index.php/api</string> <!-- 生产环境 -->
    
    <!-- 其他配置 -->
    <string name="image_base_url">https://www.huichuang100.com/</string>
    <string name="douyin_search_pattern">https://www.douyin.com/search/%s?type=user</string>
    <string name="js_search_url">https://www.huichuang100.com/android/search.js</string>
    <string name="js_user_url">https://www.huichuang100.com/android/user.js</string>
</resources>
```

## 使用方式

### 1. 推荐方式：使用工厂方法（自动读取）

#### LoginViewModel
```kotlin
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val loginRepository = LoginRepository(application)
    private val tokenManager = TokenManager(loginRepository)
    
    // 自动从 strings.xml 读取 API URL
    private val apiUtils = ServerApiUtilsV2.createFromContext(application, tokenManager, loginRepository)
}
```

#### ServerApiUtilsV2
```kotlin
// 推荐：使用工厂方法
val apiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)

// 或者手动指定
val baseUrl = context.getString(R.string.api_base_url)
val apiUtils = ServerApiUtilsV2.create(baseUrl, tokenManager, loginRepository)
```

#### ServerApiUtils（向后兼容）
```kotlin
// 使用工厂方法
val apiUtils = ServerApiUtils.createFromContext(context)

// 或者带 token
val apiUtils = ServerApiUtils.createFromContext(context, existingToken)
```

### 2. 高级配置：使用 AppConfig 类

```kotlin
// 获取当前环境的 API URL
val config = AppConfig.getInstance(context)
val apiUrl = config.getApiUrlForEnvironment(AppConfig.Environment.PRODUCTION)

// 便捷扩展函数
val apiUrl = context.getApiBaseUrl()
val imageUrl = context.getImageBaseUrl()

// 环境切换
val currentEnv = config.getCurrentEnvironment()
val envApiUrl = config.getApiUrlForEnvironment(currentEnv)
```

## 环境管理

### 支持的环境
- **开发环境** (`api_base_url1`): `http://192.168.0.101/index.php/api`
- **测试环境** (`api_base_url2`): `https://www.huichuang100.com/devapi`  
- **生产环境** (`api_base_url`): `https://www.huichuang100.com/index.php/api`

### 环境切换方法

#### 方法1：修改 strings.xml
```xml
<!-- 开发时可以临时修改 api_base_url 指向不同环境 -->
<string name="api_base_url">@string/api_base_url1</string>  <!-- 指向开发环境 -->
```

#### 方法2：使用 AppConfig 编程切换
```kotlin
val config = AppConfig.getInstance(context)
val devUrl = config.getApiUrlForEnvironment(AppConfig.Environment.DEVELOPMENT)
val testUrl = config.getApiUrlForEnvironment(AppConfig.Environment.TESTING)  
val prodUrl = config.getApiUrlForEnvironment(AppConfig.Environment.PRODUCTION)
```

#### 方法3：基于 BuildConfig 自动切换
```kotlin
// AppConfig 会根据 BuildConfig.DEBUG 自动判断环境
val config = AppConfig.getInstance(context)
val currentEnv = config.getCurrentEnvironment()  // DEBUG 时返回 DEVELOPMENT，否则返回 PRODUCTION
```

## 迁移指南

### 现有代码无需修改
所有现有的 `LoginScreen`、`AppNavigation` 等代码都会自动使用新的配置方式，无需手动修改。

### 新代码推荐用法
```kotlin
// ✅ 推荐：使用工厂方法
val viewModel: LoginViewModel = viewModel()  // 自动读取配置

// ✅ 推荐：使用 ServerApiUtilsV2
val apiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)

// ✅ 推荐：使用 AppConfig 管理配置
val config = context.getAppConfig()
```

## 调试和测试

### 验证配置
```kotlin
// 打印当前使用的 API URL
val apiUrl = context.getString(R.string.api_base_url)
Log.d("ApiConfig", "Current API URL: $apiUrl")

// 验证环境配置
val config = AppConfig.getInstance(context)
Log.d("ApiConfig", "Environment: ${config.getCurrentEnvironment()}")
Log.d("ApiConfig", "Dev URL: ${config.getApiUrlForEnvironment(AppConfig.Environment.DEVELOPMENT)}")
```

### 单元测试
```kotlin
@Test
fun testApiUrlConfiguration() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val apiUrl = AppConfig.getApiBaseUrl(context)
    
    assertThat(apiUrl).isNotEmpty()
    assertThat(apiUrl).startsWith("http")
}
```

## 优势

1. **统一管理**: 所有 URL 配置集中在 `strings.xml` 中
2. **环境切换**: 支持多环境配置和自动/手动切换
3. **向后兼容**: 现有代码无需修改即可使用新配置
4. **类型安全**: 通过 `AppConfig` 提供类型安全的配置访问
5. **易于维护**: 配置变更只需修改 `strings.xml`
6. **调试友好**: 提供多种调试和验证方法

## 注意事项

1. **生产环境**: 确保 `api_base_url` 指向正确的生产服务器
2. **安全性**: 敏感配置考虑使用 BuildConfig 或远程配置
3. **缓存**: AppConfig 使用单例模式，配置在应用生命周期内缓存
4. **测试**: 在不同环境下充分测试 API 连接性

遵循这些最佳实践，可以确保 API 配置的灵活性、可维护性和安全性。