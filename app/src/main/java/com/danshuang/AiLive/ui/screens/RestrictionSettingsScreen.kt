package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import kotlinx.coroutines.flow.first

/**
 * 限制配置状态数据类
 */
data class RestrictionState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val hasUnsavedChanges: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionSettingsScreen(
    navController: NavigationState,
    restrictionType: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 初始化依赖项
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val apiUtils = remember { ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository) }
    
    // 转换restrictionType到API格式
    val apiRestrictionType = when (restrictionType) {
        "nickname" -> "stop_nickname"
        else -> "stop_comment"
    }
    
    // 状态管理
    var restrictionState by remember { mutableStateOf(RestrictionState()) }
    var restrictionItems by remember { mutableStateOf<List<String>>(emptyList()) }
    var originalItems by remember { mutableStateOf<List<String>>(emptyList()) }
    
    // UI状态
    var newItemText by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    var duplicateItem by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 检查是否有未保存的更改
    LaunchedEffect(restrictionItems, originalItems) {
        restrictionState = restrictionState.copy(
            hasUnsavedChanges = restrictionItems.sorted() != originalItems.sorted()
        )
    }
    
    // 自动隐藏重复提示
    LaunchedEffect(showDuplicateWarning) {
        if (showDuplicateWarning) {
            showDuplicateWarning = false
        }
    }
    
    // 加载数据
    LaunchedEffect(apiRestrictionType) {
        loadRestrictionConfig(apiUtils, apiRestrictionType) { state, items ->
            restrictionState = state
            restrictionItems = items
            originalItems = items
        }
    }
    
    // 保存配置
    val saveConfig: () -> Unit = {
        scope.launch {
            saveRestrictionConfig(apiUtils, apiRestrictionType, restrictionItems) { state ->
                restrictionState = state
                if (!state.isSaving && state.error == null) {
                    // 保存成功，更新原始数据
                    originalItems = restrictionItems
                    scope.launch {
                        snackbarHostState.showSnackbar("保存成功")
                    }
                } else if (state.error != null) {
                    scope.launch {
                        snackbarHostState.showSnackbar(state.error)
                    }
                }
            }
        }
    }
    
    // 重新加载配置
    val reloadConfig: () -> Unit = {
        scope.launch {
            loadRestrictionConfig(apiUtils, apiRestrictionType) { state, items ->
                restrictionState = state
                restrictionItems = items
                originalItems = items
            }
        }
    }

    val title = when (restrictionType) {
        "nickname" -> "呢称限制设置"
        else -> "评论限制设置"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    // 保存按钮
                    if (restrictionState.hasUnsavedChanges) {
                        OutlinedButton(
                            onClick = saveConfig,
                            enabled = !restrictionState.isSaving,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            if (restrictionState.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("保存", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    // 刷新按钮
                    OutlinedButton(
                        onClick = reloadConfig,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("刷新", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // 加载状态指示
            if (restrictionState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text("正在加载配置...")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // 错误状态指示
            restrictionState.error?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            // 说明文字
            Text(
                text = when (restrictionType) {
                    "nickname" -> "设置需要限制的呢称关键词，匹配到的用户将无法进入直播间"
                    else -> "设置需要限制的评论关键词，包含这些词汇的评论将被过滤"
                },
                fontSize = 14.sp,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Chips列表
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(restrictionItems) { item ->
                    RestrictionChip(
                        text = item,
                        isSelected = selectedItem == item,
                        onSelect = {
                            selectedItem = if (selectedItem == item) null else item
                        },
                        onDelete = {
                            restrictionItems = restrictionItems.filter { it != item }.sorted()
                            if (selectedItem == item) {
                                selectedItem = null
                            }
                        }
                    )
                }
            }

            // 重复项提示
            if (showDuplicateWarning) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = "「$duplicateItem」已存在，无需重复添加",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // 底部输入区域 - 类似IM聊天发送
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 输入框
                    OutlinedTextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        label = { Text("输入限制关键词") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (newItemText.isNotBlank()) {
                                    val trimmedText = newItemText.trim()
                                    if (restrictionItems.contains(trimmedText)) {
                                        duplicateItem = trimmedText
                                        showDuplicateWarning = true
                                    } else {
                                        restrictionItems = (restrictionItems + trimmedText).sorted()
                                        newItemText = ""
                                        keyboardController?.hide()
                                        
                                        // 滚动到新添加的项
                                        scope.launch {
                                            val newItemIndex = restrictionItems.indexOf(trimmedText)
                                            if (newItemIndex >= 0) {
                                                listState.animateScrollToItem(newItemIndex)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    )

                    // 发送按钮
                    IconButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                val trimmedText = newItemText.trim()
                                if (restrictionItems.contains(trimmedText)) {
                                    duplicateItem = trimmedText
                                    showDuplicateWarning = true
                                } else {
                                    restrictionItems = (restrictionItems + trimmedText).sorted()
                                    newItemText = ""
                                    keyboardController?.hide()
                                    
                                    // 滚动到新添加的项
                                    scope.launch {
                                        val newItemIndex = restrictionItems.indexOf(trimmedText)
                                        if (newItemIndex >= 0) {
                                            listState.animateScrollToItem(newItemIndex)
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = if (newItemText.isNotBlank()) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (newItemText.isNotBlank()) 
                                Color.White 
                            else 
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 加载限制配置的辅助函数
 */
private suspend fun loadRestrictionConfig(
    apiUtils: ServerApiUtilsV2,
    restrictionType: String,
    onResult: (RestrictionState, List<String>) -> Unit
) {
    onResult(RestrictionState(isLoading = true), emptyList())
    
    try {
        val currentUser = apiUtils.getCurrentUser()
        val douyinId = currentUser?.userProfile?.boundAccount?.userId
        
        if (douyinId != null && douyinId.isNotEmpty()) {
            val response = apiUtils.getRestrictionConfig(douyinId, restrictionType)
            if (response.isSuccess && response.data != null) {
                onResult(
                    RestrictionState(isLoading = false),
                    response.data
                )
            } else {
                onResult(
                    RestrictionState(
                        isLoading = false,
                        error = response.msg ?: "获取配置失败"
                    ),
                    getDefaultRestrictionItems(restrictionType)
                )
            }
        } else {
            onResult(
                RestrictionState(
                    isLoading = false,
                    error = "未找到绑定的抖音账号"
                ),
                getDefaultRestrictionItems(restrictionType)
            )
        }
    } catch (e: Exception) {
        onResult(
            RestrictionState(
                isLoading = false,
                error = "网络错误: ${e.message}"
            ),
            getDefaultRestrictionItems(restrictionType)
        )
    }
}

/**
 * 保存限制配置的辅助函数
 */
private suspend fun saveRestrictionConfig(
    apiUtils: ServerApiUtilsV2,
    restrictionType: String,
    items: List<String>,
    onResult: (RestrictionState) -> Unit
) {
    onResult(RestrictionState(isSaving = true))
    
    try {
        val currentUser = apiUtils.getCurrentUser()
        val douyinId = currentUser?.userProfile?.boundAccount?.userId
        
        if (douyinId != null && douyinId.isNotEmpty()) {
            val response = apiUtils.updateRestrictionConfig(douyinId, restrictionType, items)
            if (response.isSuccess) {
                onResult(RestrictionState(isSaving = false))
            } else {
                onResult(RestrictionState(
                    isSaving = false,
                    error = "保存失败: ${response.msg}"
                ))
            }
        } else {
            onResult(RestrictionState(
                isSaving = false,
                error = "未找到绑定的抖音账号"
            ))
        }
    } catch (e: Exception) {
        onResult(RestrictionState(
            isSaving = false,
            error = "网络错误: ${e.message}"
        ))
    }
}

/**
 * 获取默认限制配置
 */
private fun getDefaultRestrictionItems(restrictionType: String): List<String> {
    return when (restrictionType) {
        "stop_nickname" -> listOf(
            "广告号", "机器人", "测试用户", "管理员", "客服号"
        )
        "stop_comment" -> listOf(
            "广告链接", "联系方式", "敏感词汇", "辱骂言论", "垃圾信息"
        )
        else -> emptyList()
    }.sorted()
}

@Composable
private fun RestrictionChip(
    text: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onSelect
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.align(Alignment.CenterStart)
            )

            if (isSelected) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "删除",
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}