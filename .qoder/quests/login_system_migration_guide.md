# 登录系统重构迁移指南

## 概述

我们已经完成了登录系统的重构，主要目标是减少耦合、增强数据持久化管理、并提供更好的架构。

## 主要变更

### 1. LoginState 变更

**之前:**
```kotlin
data class Success(val token: String, val phone: String) : LoginState()
```

**现在:**
```kotlin
data class Success(val user: LoggedInUser) : LoginState()
```

### 2. LoginViewModel 增强

**新增功能:**
- 集成真实的 API 调用
- 自动的数据持久化
- Token 管理
- 更好的错误处理

**新增方法:**
```kotlin
suspend fun isLoggedIn(): Boolean
suspend fun getCurrentUser(): LoggedInUser?
fun logout()
```

### 3. LoginRepository 扩展

**新增数据字段:**
- 用户昵称 (`nickname`)
- 用户头像 (`headimg`) 
- 账户余额 (`balance`)
- 积分 (`point`)
- Token过期时间 (`expiresTime`)

**新增方法:**
```kotlin
suspend fun saveLoggedInUser(user: LoggedInUser)
suspend fun updateUserProfile(profile: UserProfile)
fun isTokenValidFlow(): Flow<Boolean>
val loggedInUser: Flow<LoggedInUser?>
val userProfile: Flow<UserProfile>
```

## 迁移步骤

### 1. 更新现有的 LoginScreen 使用

**之前:**
```kotlin
if (loginState is LoginState.Success) {
    LaunchedEffect(Unit) {
        onLoginSuccess()
    }
}
```

**现在:**
```kotlin
if (loginState is LoginState.Success) {
    LaunchedEffect(loginState.user) {
        // 可以访问完整的用户信息
        val user = loginState.user
        onLoginSuccess()
    }
}
```

### 2. 如果你有其他地方处理登录成功

**之前:**
```kotlin
when (loginState) {
    is LoginState.Success -> {
        val token = loginState.token
        val phone = loginState.phone
        // 处理登录成功
    }
}
```

**现在:**
```kotlin
when (loginState) {
    is LoginState.Success -> {
        val user = loginState.user
        val token = user.token
        val phone = user.mobile
        val nickname = user.userProfile.nickname
        val avatar = user.userProfile.headimg
        // 处理登录成功，现在有更多信息可用
    }
}
```

### 3. 配置 API 基础地址

API 基础地址已配置在 `strings.xml` 中：

```xml
<string name="api_base_url">https://www.huichuang100.com/index.php/api</string>
```

**使用方式：**

1. **推荐方式（自动读取）**：
```kotlin
// LoginViewModel 会自动从 strings.xml 读取
val viewModel: LoginViewModel = viewModel()

// 或者使用工厂方法
val apiUtils = ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
val legacyApiUtils = ServerApiUtils.createFromContext(context)
```

2. **手动配置方式**：
```kotlin
val baseUrl = context.getString(R.string.api_base_url)
val apiUtils = ServerApiUtilsV2.create(baseUrl, tokenManager, loginRepository)
```

**环境配置建议：**
- 开发环境：使用 `api_base_url1` (本地测试)
- 测试环境：使用 `api_base_url2` (开发服务器) 
- 生产环境：使用 `api_base_url` (正式服务器)

## 向后兼容性

- 现有的 `LoginScreen` 调用方式保持不变
- `AppNavigation` 中的登录状态检查自动使用新的逻辑
- 所有现有的 `LoginRepository` 方法都保持兼容

## 新功能使用

### 1. 访问完整用户信息

```kotlin
@Composable
fun UserProfile() {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    val user by loginRepository.loggedInUser.collectAsState(initial = null)
    
    user?.let { 
        Text("欢迎, ${it.userProfile.nickname}")
        Text("余额: ${it.userProfile.displayBalance}")
    }
}
```

### 2. 检查 Token 状态

```kotlin
@Composable
fun TokenStatus() {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    val tokenValid by loginRepository.isTokenValidFlow().collectAsState(initial = false)
    
    if (!tokenValid) {
        Text("Token已过期，请重新登录", color = Color.Red)
    }
}
```

### 3. 使用新的 API 服务

```kotlin
class MyViewModel(
    private val apiUtils: ServerApiUtilsV2
) {
    suspend fun doSomething() {
        val accounts = apiUtils.listAccounts()
        if (accounts.isSuccess) {
            // 处理账号列表
        }
    }
}
```

## 注意事项

1. **API 地址配置**: 确保在生产环境中正确配置 API 服务器地址
2. **错误处理**: 新系统提供了更详细的错误信息，建议更新错误显示逻辑
3. **测试**: 建议对登录流程进行全面测试，特别是网络异常情况
4. **性能**: 新系统使用了更多的 Flow，注意在适当的地方取消订阅

## 问题排查

### 常见问题

1. **编译错误**: 检查是否正确导入了新的数据模型类
2. **运行时错误**: 确保 API 地址配置正确
3. **登录状态不更新**: 检查是否正确使用了 `collectAsState()`

### 调试技巧

1. 使用 `LoginSystemExample.kt` 中的示例代码进行测试
2. 检查 Logcat 中的 API 调用日志
3. 验证 DataStore 中的数据是否正确保存

## 后续计划

1. 添加 Token 自动刷新功能
2. 实现更多的 API 接口
3. 添加网络状态监控
4. 增强安全性措施