# 招呼设置API使用指南

本文档介绍如何使用新增的招呼设置API接口。

## 数据模型

### GreetingConfig
```kotlin
data class GreetingConfig(
    val greetStatus: Int,        // 主动招呼状态：0-关闭，1-开启
    val maxOnline: Int,          // 最大在线人数
    val delaySeconds: Int        // 响应延迟（秒）
)
```

## API接口

### 1. 获取招呼设置配置

#### ConfigApiService直接调用
```kotlin
suspend fun getGreetingConfig(
    token: String,
    douyinId: String
): ServerResponse<GreetingConfig>
```

#### ServerApiUtilsV2统一接口
```kotlin
suspend fun getGreetingConfig(
    douyinId: String
): ServerResponse<GreetingConfig>
```

**使用示例：**
```kotlin
// 使用统一接口（推荐）
val result = serverApiUtilsV2.getGreetingConfig("your_douyin_id")
if (result.isSuccess && result.data != null) {
    val config = result.data
    println("招呼状态: ${config.greetStatus}")
    println("最大在线人数: ${config.maxOnline}")
    println("响应延迟: ${config.delaySeconds}秒")
} else {
    println("获取失败: ${result.msg}")
}
```

### 2. 提交招呼设置配置

#### ConfigApiService直接调用
```kotlin
suspend fun updateGreetingConfig(
    token: String,
    douyinId: String,
    greetStatus: Int,
    maxOnline: Int,
    delaySeconds: Int
): ServerResponse<Any>
```

#### ServerApiUtilsV2统一接口
```kotlin
suspend fun updateGreetingConfig(
    douyinId: String,
    greetStatus: Int,
    maxOnline: Int,
    delaySeconds: Int
): ServerResponse<Any>
```

**使用示例：**
```kotlin
// 使用统一接口（推荐）
val result = serverApiUtilsV2.updateGreetingConfig(
    douyinId = "your_douyin_id",
    greetStatus = 1,      // 开启主动招呼
    maxOnline = 100,      // 最大在线人数100
    delaySeconds = 30     // 延迟30秒
)

if (result.isSuccess) {
    println("招呼设置更新成功")
} else {
    println("更新失败: ${result.msg}")
}
```

## 完整使用示例

### 在ViewModel中使用
```kotlin
class GreetingSettingsViewModel(
    private val serverApiUtilsV2: ServerApiUtilsV2
) : ViewModel() {
    
    private val _greetingConfig = MutableLiveData<GreetingConfig>()
    val greetingConfig: LiveData<GreetingConfig> = _greetingConfig
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message
    
    /**
     * 加载招呼设置
     */
    fun loadGreetingConfig(douyinId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = serverApiUtilsV2.getGreetingConfig(douyinId)
                if (result.isSuccess && result.data != null) {
                    _greetingConfig.value = result.data
                } else {
                    _message.value = "加载失败: ${result.msg}"
                }
            } catch (e: Exception) {
                _message.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * 保存招呼设置
     */
    fun saveGreetingConfig(
        douyinId: String,
        greetStatus: Int,
        maxOnline: Int,
        delaySeconds: Int
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = serverApiUtilsV2.updateGreetingConfig(
                    douyinId, greetStatus, maxOnline, delaySeconds
                )
                if (result.isSuccess) {
                    _message.value = "设置保存成功"
                    // 重新加载最新配置
                    loadGreetingConfig(douyinId)
                } else {
                    _message.value = "保存失败: ${result.msg}"
                }
            } catch (e: Exception) {
                _message.value = "保存失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

### 在Composable中使用
```kotlin
@Composable
fun GreetingSettingsScreen(
    douyinId: String,
    viewModel: GreetingSettingsViewModel = hiltViewModel()
) {
    val greetingConfig by viewModel.greetingConfig.observeAsState()
    val isLoading by viewModel.isLoading.observeAsState(false)
    val message by viewModel.message.observeAsState()
    
    // 初始加载
    LaunchedEffect(douyinId) {
        viewModel.loadGreetingConfig(douyinId)
    }
    
    // 处理消息显示
    message?.let { msg ->
        LaunchedEffect(msg) {
            // 显示Snackbar或Toast
            // snackbarHostState.showSnackbar(msg)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            greetingConfig?.let { config ->
                // 招呼状态开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("主动招呼", modifier = Modifier.weight(1f))
                    Switch(
                        checked = config.greetStatus == 1,
                        onCheckedChange = { checked ->
                            viewModel.saveGreetingConfig(
                                douyinId,
                                if (checked) 1 else 0,
                                config.maxOnline,
                                config.delaySeconds
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 最大在线人数设置
                Text("最大在线人数: ${config.maxOnline}")
                Slider(
                    value = config.maxOnline.toFloat(),
                    onValueChange = { /* 实时更新 */ },
                    onValueChangeFinished = {
                        // 保存设置
                    },
                    valueRange = 10f..1000f
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 响应延迟设置
                Text("响应延迟: ${config.delaySeconds}秒")
                Slider(
                    value = config.delaySeconds.toFloat(),
                    onValueChange = { /* 实时更新 */ },
                    onValueChangeFinished = {
                        // 保存设置
                    },
                    valueRange = 0f..300f
                )
            }
        }
    }
}
```

## 接口端点

- **获取招呼设置**: `POST /dscrm/dylive-android/get-greeting-config`
- **更新招呼设置**: `POST /dscrm/dylive-android/update-greeting-config`

## 注意事项

1. 所有接口都需要用户认证token
2. 统一接口会自动处理token管理
3. 建议使用ServerApiUtilsV2的统一接口而不是直接调用ConfigApiService
4. 接口调用失败时会返回相应的错误信息
5. 数值参数都是Number类型，会自动转换为Int