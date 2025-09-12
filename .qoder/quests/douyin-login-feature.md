# 抖音和来客登录功能设计文档

## 1. Overview

本设计文档描述了在"我的"页面中添加"登录抖音"和"登录来客"两个菜单项的功能实现方案。这两个功能将允许用户独立登录抖音和来客平台，并根据登录状态动态显示不同的菜单文本。

### 1.1 核心功能
1. 在"我的"页面的菜单列表中添加"登录抖音"和"登录来客"两个菜单项
2. 菜单项名称根据登录状态动态显示：
   - 未登录时显示"登录抖音"/"登录来客"
   - 已登录时显示"抖音：$nickname已登录"/"来客：$nickname已登录"
3. 点击菜单项时，如果用户已绑定相应账户，则跳转到对应的账户页面
4. 如果用户未绑定账户，则跳转到账户绑定页面进行绑定操作

### 1.2 技术约束
- 以上两个登录绑定的功能是独立的新功能页，登录信息都在独立存储
- 不修改现有的AccountBindingScreen账户绑定业务逻辑

## 2. Architecture

### 2.1 项目类型
这是一个使用Jetpack Compose和Material 3 Design构建的Android应用，采用单模块架构模式。

### 2.2 技术栈
- Kotlin: 2.0.21
- Android Gradle Plugin: 8.10.1
- Compose BOM: 2024.10.01
- Material 3: 最新稳定版
- Jetpack Compose: 声明式UI框架

### 2.3 架构模式
- 声明式UI架构（Jetpack Compose）
- 单向数据流模式
- 组件化设计模式

## 2. Data Models & ORM Mapping

### 2.1 BoundAccountInfo数据模型扩展
扩展现有的BoundAccountInfo数据模型以支持平台类型标识：

```kotlin
data class BoundAccountInfo(
    val platform: String = "douyin",  // 平台标识（douyin, lk）
    val avatar: String = "",          // 账户头像
    val nickname: String = "",        // 账户昵称
    val userId: String = "",          // 用户ID
    val homeUrl: String = "",         // 用户主页URL
    val isLive: Boolean = false,      // 是否在直播
    val bindTime: Long = System.currentTimeMillis()  // 绑定时间
)
```

### 2.2 平台类型枚举
创建平台类型枚举以区分不同的平台：

```kotlin
object PlatformType {
    const val DOUYIN = "douyin"
    const val LK = "lk"
}
```

## 3. Storage Design

使用DataStore为抖音和来客分别创建独立的存储键，保持与现有绑定账户信息的隔离：

```kotlin
// 抖音账户信息存储键
private val DOUYIN_ACCOUNT_PLATFORM_KEY = stringPreferencesKey("douyin_account_platform")
private val DOUYIN_ACCOUNT_AVATAR_KEY = stringPreferencesKey("douyin_account_avatar")
private val DOUYIN_ACCOUNT_NICKNAME_KEY = stringPreferencesKey("douyin_account_nickname")
private val DOUYIN_ACCOUNT_USER_ID_KEY = stringPreferencesKey("douyin_account_user_id")
private val DOUYIN_ACCOUNT_HOME_URL_KEY = stringPreferencesKey("douyin_account_home_url")
private val DOUYIN_ACCOUNT_IS_LIVE_KEY = stringPreferencesKey("douyin_account_is_live")
private val DOUYIN_ACCOUNT_BIND_TIME_KEY = longPreferencesKey("douyin_account_bind_time")

// 来客账户信息存储键
private val LK_ACCOUNT_PLATFORM_KEY = stringPreferencesKey("lk_account_platform")
private val LK_ACCOUNT_AVATAR_KEY = stringPreferencesKey("lk_account_avatar")
private val LK_ACCOUNT_NICKNAME_KEY = stringPreferencesKey("lk_account_nickname")
private val LK_ACCOUNT_USER_ID_KEY = stringPreferencesKey("lk_account_user_id")
private val LK_ACCOUNT_HOME_URL_KEY = stringPreferencesKey("lk_account_home_url")
private val LK_ACCOUNT_IS_LIVE_KEY = stringPreferencesKey("lk_account_is_live")
private val LK_ACCOUNT_BIND_TIME_KEY = longPreferencesKey("lk_account_bind_time")
```

## 4. Navigation Design

在Navigation.kt中添加新的导航路由：

```kotlin
sealed class Screen(val route: String) {
    // ... existing screens ...
    data object DouyinAccountBinding : Screen("douyin_account_binding") {
        fun createRoute() = "douyin_account_binding"
    }
    data object LkAccountBinding : Screen("lk_account_binding") {
        fun createRoute() = "lk_account_binding"
    }
}

class NavigationState {
    // ... existing methods ...
    
    fun navigateToDouyinAccountBinding() {
        currentScreen = Screen.DouyinAccountBinding
    }
    
    fun navigateToLkAccountBinding() {
        currentScreen = Screen.LkAccountBinding
    }
}
```

## 5. UI Design

### 5.1 菜单项设计
在ProfileScreen.kt中添加新的菜单项，修改MenuItem数据类以支持动态标题：

```kotlin
// 扩展MenuItem数据类以支持动态标题
data class MenuItem(
    val icon: ImageVector,
    val title: String,
    val dynamicTitle: (() -> String)? = null, // 动态标题生成函数
    val onClick: () -> Unit
)
```

在ProfileScreen可组合函数中添加抖音和来客菜单项：

```kotlin
// 获取抖音和来客账户信息
val douyinAccount by loginRepository.getDouyinAccountFlow().collectAsState(initial = null)
val lkAccount by loginRepository.getLkAccountFlow().collectAsState(initial = null)

// 在ProfileScreen中添加抖音和来客菜单项
val menuItems = remember {
    listOf(
        // ... existing menu items ...
        MenuItem(
            icon = Icons.Filled.VideoLibrary,
            title = "登录抖音",
            dynamicTitle = { 
                if (douyinAccount != null) {
                    "抖音：${douyinAccount?.nickname}已登录"
                } else {
                    "登录抖音"
                }
            },
            onClick = { 
                if (douyinAccount != null) {
                    // 跳转到抖音账户页面（如果需要）
                } else {
                    // 跳转到抖音账户绑定页面
                    navigationState.navigateToDouyinAccountBinding()
                }
            }
        ),
        MenuItem(
            icon = Icons.Filled.Store,
            title = "登录来客",
            dynamicTitle = { 
                if (lkAccount != null) {
                    "来客：${lkAccount?.nickname}已登录"
                } else {
                    "登录来客"
                }
            },
            onClick = { 
                if (lkAccount != null) {
                    // 跳转到来客账户页面（如果需要）
                } else {
                    // 跳转到来客账户绑定页面
                    navigationState.navigateToLkAccountBinding()
                }
            }
        )
    )
}
```

### 5.2 菜单显示逻辑
修改菜单项显示逻辑以支持动态标题：

```kotlin
// 在LazyColumn的items部分修改
items(menuItems) { item ->
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = item.dynamicTitle?.invoke() ?: item.title,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = "箭头",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Divider()
}
```

## 6. Business Logic Layer

### 6.1 LoginRepository扩展
扩展LoginRepository以支持独立的抖音和来客账户管理：

```kotlin
class LoginRepository(val context: Context) {
    // ... existing code ...
    
    // 抖音账户信息获取（挂起函数）
    suspend fun getDouyinAccount(): BoundAccountInfo? {
        val preferences = context.dataStore.data.first()
        return if (preferences[DOUYIN_ACCOUNT_PLATFORM_KEY] != null) {
            BoundAccountInfo(
                platform = PlatformType.DOUYIN,
                avatar = preferences[DOUYIN_ACCOUNT_AVATAR_KEY] ?: "",
                nickname = preferences[DOUYIN_ACCOUNT_NICKNAME_KEY] ?: "",
                userId = preferences[DOUYIN_ACCOUNT_USER_ID_KEY] ?: "",
                homeUrl = preferences[DOUYIN_ACCOUNT_HOME_URL_KEY] ?: "",
                isLive = preferences[DOUYIN_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                bindTime = preferences[DOUYIN_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
            )
        } else {
            null
        }
    }
    
    // 抖音账户信息流
    fun getDouyinAccountFlow(): Flow<BoundAccountInfo?> = context.dataStore.data
        .map { preferences ->
            if (preferences[DOUYIN_ACCOUNT_PLATFORM_KEY] != null) {
                BoundAccountInfo(
                    platform = PlatformType.DOUYIN,
                    avatar = preferences[DOUYIN_ACCOUNT_AVATAR_KEY] ?: "",
                    nickname = preferences[DOUYIN_ACCOUNT_NICKNAME_KEY] ?: "",
                    userId = preferences[DOUYIN_ACCOUNT_USER_ID_KEY] ?: "",
                    homeUrl = preferences[DOUYIN_ACCOUNT_HOME_URL_KEY] ?: "",
                    isLive = preferences[DOUYIN_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                    bindTime = preferences[DOUYIN_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    
    // 来客账户信息获取（挂起函数）
    suspend fun getLkAccount(): BoundAccountInfo? {
        val preferences = context.dataStore.data.first()
        return if (preferences[LK_ACCOUNT_PLATFORM_KEY] != null) {
            BoundAccountInfo(
                platform = PlatformType.LK,
                avatar = preferences[LK_ACCOUNT_AVATAR_KEY] ?: "",
                nickname = preferences[LK_ACCOUNT_NICKNAME_KEY] ?: "",
                userId = preferences[LK_ACCOUNT_USER_ID_KEY] ?: "",
                homeUrl = preferences[LK_ACCOUNT_HOME_URL_KEY] ?: "",
                isLive = preferences[LK_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                bindTime = preferences[LK_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
            )
        } else {
            null
        }
    }
    
    // 来客账户信息流
    fun getLkAccountFlow(): Flow<BoundAccountInfo?> = context.dataStore.data
        .map { preferences ->
            if (preferences[LK_ACCOUNT_PLATFORM_KEY] != null) {
                BoundAccountInfo(
                    platform = PlatformType.LK,
                    avatar = preferences[LK_ACCOUNT_AVATAR_KEY] ?: "",
                    nickname = preferences[LK_ACCOUNT_NICKNAME_KEY] ?: "",
                    userId = preferences[LK_ACCOUNT_USER_ID_KEY] ?: "",
                    homeUrl = preferences[LK_ACCOUNT_HOME_URL_KEY] ?: "",
                    isLive = preferences[LK_ACCOUNT_IS_LIVE_KEY]?.toBoolean() ?: false,
                    bindTime = preferences[LK_ACCOUNT_BIND_TIME_KEY] ?: System.currentTimeMillis()
                )
            } else {
                null
            }
        }
    
    // 保存抖音账户信息
    suspend fun saveDouyinAccount(account: BoundAccountInfo) {
        context.dataStore.edit { preferences ->
            preferences[DOUYIN_ACCOUNT_PLATFORM_KEY] = account.platform
            preferences[DOUYIN_ACCOUNT_AVATAR_KEY] = account.avatar
            preferences[DOUYIN_ACCOUNT_NICKNAME_KEY] = account.nickname
            preferences[DOUYIN_ACCOUNT_USER_ID_KEY] = account.userId
            preferences[DOUYIN_ACCOUNT_HOME_URL_KEY] = account.homeUrl
            preferences[DOUYIN_ACCOUNT_IS_LIVE_KEY] = account.isLive.toString()
            preferences[DOUYIN_ACCOUNT_BIND_TIME_KEY] = account.bindTime
        }
    }
    
    // 保存来客账户信息
    suspend fun saveLkAccount(account: BoundAccountInfo) {
        context.dataStore.edit { preferences ->
            preferences[LK_ACCOUNT_PLATFORM_KEY] = account.platform
            preferences[LK_ACCOUNT_AVATAR_KEY] = account.avatar
            preferences[LK_ACCOUNT_NICKNAME_KEY] = account.nickname
            preferences[LK_ACCOUNT_USER_ID_KEY] = account.userId
            preferences[LK_ACCOUNT_HOME_URL_KEY] = account.homeUrl
            preferences[LK_ACCOUNT_IS_LIVE_KEY] = account.isLive.toString()
            preferences[LK_ACCOUNT_BIND_TIME_KEY] = account.bindTime
        }
    }
    
    // 清除抖音账户信息
    suspend fun clearDouyinAccount() {
        context.dataStore.edit { preferences ->
            preferences.remove(DOUYIN_ACCOUNT_PLATFORM_KEY)
            preferences.remove(DOUYIN_ACCOUNT_AVATAR_KEY)
            preferences.remove(DOUYIN_ACCOUNT_NICKNAME_KEY)
            preferences.remove(DOUYIN_ACCOUNT_USER_ID_KEY)
            preferences.remove(DOUYIN_ACCOUNT_HOME_URL_KEY)
            preferences.remove(DOUYIN_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(DOUYIN_ACCOUNT_BIND_TIME_KEY)
        }
    }
    
    // 清除来客账户信息
    suspend fun clearLkAccount() {
        context.dataStore.edit { preferences ->
            preferences.remove(LK_ACCOUNT_PLATFORM_KEY)
            preferences.remove(LK_ACCOUNT_AVATAR_KEY)
            preferences.remove(LK_ACCOUNT_NICKNAME_KEY)
            preferences.remove(LK_ACCOUNT_USER_ID_KEY)
            preferences.remove(LK_ACCOUNT_HOME_URL_KEY)
            preferences.remove(LK_ACCOUNT_IS_LIVE_KEY)
            preferences.remove(LK_ACCOUNT_BIND_TIME_KEY)
        }
    }
}
```

## 7. New Screens Implementation

### 7.1 抖音账户绑定页面
创建DouyinAccountBindingScreen.kt，基于现有的AccountBindingScreen实现，但专门用于抖音账户绑定：

1. 复用AccountBindingScreen的核心逻辑
2. 修改页面标题为"抖音账户绑定"
3. 调整URL输入提示文本以适应抖音链接格式
4. 使用独立的抖音账户存储方法（saveDouyinAccount）
5. 绑定成功后返回ProfileScreen页面

### 7.2 来客账户绑定页面
创建LkAccountBindingScreen.kt，基于现有的AccountBindingScreen实现，但专门用于来客账户绑定：

1. 复用AccountBindingScreen的核心逻辑
2. 修改页面标题为"来客账户绑定"
3. 调整URL输入提示文本以适应来客链接格式
4. 使用独立的来客账户存储方法（saveLkAccount）
5. 绑定成功后返回ProfileScreen页面

### 7.3 账户详情页面（可选）
如果需要显示已绑定账户的详细信息，可以创建相应的详情页面：

1. DouyinAccountDetailScreen.kt - 显示抖音账户详细信息
2. LkAccountDetailScreen.kt - 显示来客账户详细信息
3. 提供解绑功能按钮
4. 显示账户绑定时间等信息

## 8. Testing

### 8.1 单元测试
1. 测试LoginRepository中新增的抖音和来客账户管理方法
   - getDouyinAccount() 和 getLkAccount() 方法
   - saveDouyinAccount() 和 saveLkAccount() 方法
   - clearDouyinAccount() 和 clearLkAccount() 方法
2. 测试BoundAccountInfo数据类的序列化和反序列化
3. 测试菜单项动态标题的生成逻辑
4. 测试平台类型枚举的正确性

### 8.2 集成测试
1. 测试从ProfileScreen导航到账户绑定页面的流程
2. 测试账户绑定成功后返回ProfileScreen并显示正确状态
3. 测试已绑定账户时点击菜单项的导航行为
4. 测试账户解绑功能（如果实现）
5. 测试数据持久化功能，确保应用重启后账户信息仍然存在

### 8.3 UI测试
1. 验证菜单项在不同状态下的正确显示
   - 未登录状态显示"登录抖音"/"登录来客"
   - 已登录状态显示"抖音：$nickname已登录"/"来客：$nickname已登录"
2. 验证点击菜单项的正确导航行为
   - 未登录时跳转到对应绑定页面
   - 已登录时跳转到账户详情页面（如果实现）
3. 验证账户绑定页面的正确显示和功能
4. 验证表单输入和验证逻辑
5. 验证错误处理和提示信息




