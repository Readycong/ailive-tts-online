# 知识条目管理功能设计

## 概述

本设计文档描述了将KnowledgeScreen页面与服务端API完全集成的知识条目管理功能。该功能实现了知识库条目的查看、新增、编辑和删除操作，所有数据操作都通过服务端API进行，确保数据的持久化和一致性。

## 架构设计

### 整体架构

```mermaid
graph TB
    A[KnowledgeScreen] --> B[ViewModel/State]
    B --> C[ServerApiUtilsV2]
    C --> D[KnowledgeApiService]
    D --> E[HTTP API]
    E --> F[服务端]
    
    A --> G[Compose UI]
    G --> H[QACard组件]
    G --> I[编辑对话框]
    G --> J[Snackbar反馈]
    
    B --> K[本地状态管理]
    K --> L[知识条目列表]
    K --> M[加载状态]
    K --> N[错误状态]
```

### 数据流架构

```mermaid
flowchart TD
    Start([页面初始化]) --> LoadData["调用fetchKbItems('QA')加载数据"]
    LoadData --> UpdateUI["更新UI状态"]
    
    UserAction["用户操作"] --> CheckAction{操作类型}
    CheckAction -->|新增| AddItem["调用updateKbItem新增"]
    CheckAction -->|编辑| EditFlow["编辑流程"]
    CheckAction -->|删除| DeleteItem["调用deleteKbItem删除"]
    
    EditFlow --> FetchLatest["调用fetchKbItems('QA', kbId)获取最新数据"]
    FetchLatest --> ShowEditDialog["显示编辑对话框"]
    ShowEditDialog --> SaveEdit["调用updateKbItem保存"]
    
    AddItem --> HandleResponse["处理API响应"]
    SaveEdit --> HandleResponse
    DeleteItem --> HandleResponse
    
    HandleResponse --> ShowSnackbar["显示Snackbar反馈"]
    HandleResponse --> RefreshUI["刷新UI数据"]
```

## 数据模型设计

### KnowledgeItem数据模型

```kotlin
data class KnowledgeItem(
    val id: String,
    val typeId: String,
    val question: String,
    val answer: String,
    val createTime: Long,
    val updateTime: Long
)
```

### UI状态模型

```kotlin
data class KnowledgeScreenState(
    val items: List<KnowledgeItem> = emptyList(),
    val isLoading: Boolean = false,
    val showEditDialog: Boolean = false,
    val editingItem: KnowledgeItem? = null,
    val errorMessage: String? = null,
    val snackbarMessage: String? = null,
    val editQuestion: String = "",
    val editAnswer: String = ""
)
```

## API集成设计

### 接口调用规范

| 操作 | API方法 | 参数 | 说明 |
|------|---------|------|------|
| 加载数据 | `fetchKbItems("QA")` | typeId="QA" | 页面初始化加载我的知识库 |
| 获取单条 | `fetchKbItems("QA", kbId)` | typeId="QA", kbId | 编辑前获取最新数据 |
| 新增/更新 | `updateKbItem()` | 完整条目信息 | 新增和编辑都使用此接口 |
| 删除 | `deleteKbItem()` | douyinId, itemId | 删除指定条目 |

### 错误处理策略

```mermaid
graph TD
    APICall[API调用] --> Success{调用成功?}
    Success -->|是| UpdateUI[更新UI状态]
    Success -->|否| ErrorType{错误类型}
    
    ErrorType -->|网络错误| ShowNetworkError[显示网络错误]
    ErrorType -->|权限错误| ShowAuthError[显示认证错误]
    ErrorType -->|数据错误| ShowDataError[显示数据错误]
    
    ShowNetworkError --> Snackbar[Snackbar提示]
    ShowAuthError --> Snackbar
    ShowDataError --> Snackbar
    
    UpdateUI --> SuccessSnackbar[成功提示]
```

## UI交互设计

### 页面状态管理

```mermaid
stateDiagram-v2
    [*] --> Loading
    Loading --> Loaded : 我的知识库加载成功
    Loading --> Error : 数据加载失败
    
    Loaded --> Editing : 点击编辑/新增
    Loaded --> Deleting : 侧滑删除
    
    Editing --> FetchingLatest : 编辑时获取最新数据
    FetchingLatest --> EditDialog : 获取成功
    FetchingLatest --> EditDialogWithCache : 获取失败，使用缓存
    
    EditDialog --> Saving : 保存编辑
    EditDialogWithCache --> Saving
    
    Saving --> Loaded : 保存成功
    Saving --> EditDialog : 保存失败
    
    Deleting --> Loaded : 删除成功
    Deleting --> Loaded : 删除失败
    
    Error --> Loading : 重试加载
```

### 用户反馈机制

| 操作 | 成功反馈 | 失败反馈 | 加载状态 |
|------|----------|----------|----------|
| 加载知识库 | 显示条目列表 | "加载我的知识库失败，请重试" | Loading遮罩 |
| 新增条目 | "添加成功" | "添加失败：错误信息" | 按钮禁用 |
| 编辑条目 | "更新成功" | "更新失败：错误信息" | 保存按钮禁用 |
| 删除条目 | "删除成功" | "删除失败：错误信息" | 删除按钮禁用 |

## 核心功能实现

### 1. 页面初始化加载

```kotlin
// 页面进入时加载知识库条目
LaunchedEffect(Unit) {
    scope.launch {
        isLoading = true
        try {
            val response = serverApiUtils.fetchKbItems("QA")
            if (response.isSuccess && response.data != null) {
                qaList = response.data
            } else {
                snackbarMessage = "加载我的知识库失败：${response.msg}"
            }
        } catch (e: Exception) {
            snackbarMessage = "加载我的知识库失败：${e.message}"
        } finally {
            isLoading = false
        }
    }
}
```

### 2. 新增条目功能

```kotlin
// 新增条目时调用API
suspend fun addNewItem(question: String, answer: String) {
    scope.launch {
        try {
            val response = serverApiUtils.updateKbItem(
                douyinId = currentUser.douyinId,
                typeId = "QA",
                id = "", // 新增时id为空
                question = question,
                answer = answer
            )
            if (response.isSuccess) {
                snackbarMessage = "添加成功"
                refreshData() // 重新加载数据
            } else {
                snackbarMessage = "添加失败：${response.msg}"
            }
        } catch (e: Exception) {
            snackbarMessage = "添加失败：${e.message}"
        }
    }
}
```

### 3. 编辑条目功能

```kotlin
// 编辑前获取最新数据
suspend fun editItem(item: KnowledgeItem) {
    scope.launch {
        try {
            // 先获取最新数据
            val response = serverApiUtils.fetchKbItems("QA", item.id)
            if (response.isSuccess && response.data?.isNotEmpty() == true) {
                val latestItem = response.data.first()
                editingItem = latestItem
                editQuestion = latestItem.question
                editAnswer = latestItem.answer
            } else {
                // 获取失败时使用缓存数据
                editingItem = item
                editQuestion = item.question
                editAnswer = item.answer
                snackbarMessage = "获取最新数据失败，使用缓存数据"
            }
            showEditDialog = true
        } catch (e: Exception) {
            editingItem = item
            editQuestion = item.question
            editAnswer = item.answer
            showEditDialog = true
            snackbarMessage = "获取最新数据失败：${e.message}"
        }
    }
}

// 保存编辑
suspend fun saveEdit() {
    scope.launch {
        try {
            val response = serverApiUtils.updateKbItem(
                douyinId = currentUser.douyinId,
                typeId = "QA",
                id = editingItem!!.id,
                question = editQuestion,
                answer = editAnswer
            )
            if (response.isSuccess) {
                snackbarMessage = "更新成功"
                showEditDialog = false
                refreshData()
            } else {
                snackbarMessage = "更新失败：${response.msg}"
            }
        } catch (e: Exception) {
            snackbarMessage = "更新失败：${e.message}"
        }
    }
}
```

### 4. 删除条目功能

```kotlin
// 删除条目
suspend fun deleteItem(item: KnowledgeItem) {
    scope.launch {
        try {
            val response = serverApiUtils.deleteKbItem(
                douyinId = currentUser.douyinId,
                id = item.id
            )
            if (response.isSuccess) {
                snackbarMessage = "删除成功"
                qaList = qaList.filterNot { it.id == item.id }
            } else {
                snackbarMessage = "删除失败：${response.msg}"
            }
        } catch (e: Exception) {
            snackbarMessage = "删除失败：${e.message}"
        }
    }
}
```

## 状态管理规范

### Loading状态管理

```kotlin
// 统一的Loading状态管理
var isLoading by remember { mutableStateOf(false) }

// 在需要Loading的操作中
scope.launch {
    isLoading = true
    try {
        // API操作
    } finally {
        isLoading = false
    }
}

// UI中显示Loading
if (isLoading) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
```

### Snackbar反馈管理

```kotlin
// Snackbar状态管理
var snackbarMessage by remember { mutableStateOf<String?>(null) }

// 显示Snackbar
snackbarMessage?.let { message ->
    LaunchedEffect(message) {
        snackbarHostState.showSnackbar(
            message = message,
            duration = SnackbarDuration.Short
        )
        snackbarMessage = null
    }
}
```

## 组件设计

### KnowledgeCard组件设计

```mermaid
graph TD
    A[KnowledgeCard] --> B[显示问题和答案]
    A --> C[侧滑删除手势]
    A --> D[点击编辑功能]
    
    C --> E[检测侧滑手势]
    E --> F[显示删除按钮]
    F --> G[调用deleteKbItem API]
    
    D --> H[触发编辑流程]
    H --> I[获取最新数据]
    I --> J[显示编辑对话框]
```

### 页面布局设计

```mermaid
graph TD
    A[KnowledgeScreen] --> B[TopAppBar]
    A --> C[知识条目列表]
    A --> D[FloatingActionButton]
    
    B --> E[标题: 我的知识库]
    B --> F[返回按钮]
    B --> G[添加按钮]
    
    C --> H[LazyColumn]
    H --> I[KnowledgeCard组件]
    
    D --> J[新增知识条目]
```

### 编辑对话框设计

```kotlin
@Composable
fun EditKnowledgeDialog(
    item: KnowledgeItem?,
    question: String,
    answer: String,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    isSaving: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item != null) "编辑知识条目" else "新增知识条目") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = onQuestionChange,
                    label = { Text("问题") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = answer,
                    onValueChange = onAnswerChange,
                    label = { Text("答案") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = !isSaving && question.isNotBlank() && answer.isNotBlank()
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text("保存")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("取消")
            }
        }
    )
}
```

## 性能优化

### 1. 数据缓存策略

```mermaid
graph TD
    A[数据请求] --> B{本地缓存存在?}
    B -->|是| C[检查缓存有效期]
    B -->|否| D[发起网络请求]
    
    C --> E{缓存有效?}
    E -->|是| F[使用缓存数据]
    E -->|否| D
    
    D --> G[更新本地缓存]
    G --> H[返回最新数据]
```

### 2. UI性能优化

- 使用`LazyColumn`进行列表虚拟化
- 通过`key`参数优化列表重组
- 合理使用`remember`避免不必要的重新计算
- 使用`derivedStateOf`优化计算状态

### 3. 网络请求优化

- 合并批量操作减少请求次数
- 实现请求去重避免重复调用
- 使用协程管理并发请求
- 实现重试机制提高成功率

## 测试策略

### 单元测试

```kotlin
@Test
fun `测试知识条目加载功能`() = runTest {
    // Given
    val mockApiUtils = mockk<ServerApiUtilsV2>()
    val expectedItems = listOf(
        KnowledgeItem("1", "QA", "问题1", "答案1", 0L, 0L)
    )
    coEvery { mockApiUtils.fetchKbItems("QA") } returns 
        ServerResponse(1, "成功", expectedItems)
    
    // When
    val result = mockApiUtils.fetchKbItems("QA")
    
    // Then
    assertEquals(true, result.isSuccess)
    assertEquals(expectedItems, result.data)
}
```

### UI测试

```kotlin
@Test
fun `测试编辑对话框显示和隐藏`() {
    composeTestRule.setContent {
        KnowledgeScreen()
    }
    
    // 点击编辑按钮
    composeTestRule.onNodeWithText("编辑").performClick()
    
    // 验证对话框显示
    composeTestRule.onNodeWithText("编辑知识条目").assertIsDisplayed()
    
    // 点击取消按钮
    composeTestRule.onNodeWithText("取消").performClick()
    
    // 验证对话框隐藏
    composeTestRule.onNodeWithText("编辑知识条目").assertDoesNotExist()
}
```