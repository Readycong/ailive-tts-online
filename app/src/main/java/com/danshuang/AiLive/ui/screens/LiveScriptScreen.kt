package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.danshuang.AiLive.data.LiveScriptItem
import com.danshuang.AiLive.navigation.NavigationState
import androidx.compose.ui.zIndex
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.api.service.KnowledgeApiService
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.model.KnowledgeItem
import com.danshuang.AiLive.config.AppConfig
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScriptScreen(
    navController: NavigationState,
    scriptKey: String
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 初始化API依赖项
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val knowledgeApiService = remember {
        KnowledgeApiService(
            tokenManager = tokenManager,
            baseUrl = AppConfig.getApiBaseUrl(context),
            context = context
        )
    }
    val serverApiUtils = remember {
        ServerApiUtilsV2.createFromContext(
            context = context,
            tokenManager = tokenManager,
            loginRepository = loginRepository
        )
    }
    
    var currentSwipeId by remember { mutableStateOf<Number?>(null) }
    
    // 状态管理
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    // scriptKey到typeId的映射
    val typeId = when (scriptKey) {
        "WELCOME" -> "WELCOME"
        "ROOM" -> "ROOM"
        "PRODUCT" -> "PRODUCT"
        "KEEP" -> "KEEP"
        "ORDER" -> "ORDER"
        "THANKS" -> "THANKS"
        else -> "unknown_scripts"
    }
    
    val title = when (scriptKey) {
        "WELCOME" -> "问候话术"
        "ROOM" -> "品牌话术"
        "PRODUCT" -> "带货话术"
        "KEEP" -> "留咨话术"
        "ORDER" -> "促成话术"
        "THANKS" -> "感谢话术"
        else -> "eRRRRRor"
    }

    // 话术数据（现在从服务器获取）
    var scriptList by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }

    // 从服务器获取话术数据
    LaunchedEffect(scriptKey) {
        isLoading = true
        try {
            val currentToken = tokenManager.currentToken.first()
            if (currentToken != null) {
                val response = serverApiUtils.fetchKbItems(typeId)
                if (response.isSuccess && response.data != null) {
                    scriptList = response.data
                } else {
                    snackbarMessage = "获取话术失败: ${response.msg}"
                    showSnackbar = true
                }
            } else {
                snackbarMessage = "未登录，无法获取数据"
                showSnackbar = true
            }
        } catch (e: Exception) {
            snackbarMessage = "加载失败: ${e.message}"
            showSnackbar = true
        } finally {
            isLoading = false
        }
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var editContent by remember { mutableStateOf("") }

    // 获取当前用户的douyinId
    suspend fun getCurrentDouyinId(): String? {
        return try {
            val currentUser = loginRepository.loggedInUser.first()
            currentUser?.userProfile?.boundAccount?.userId
        } catch (e: Exception) {
            null
        }
    }

    // 保存功能
    val saveContent: () -> Unit = {
        scope.launch {
            isSaving = true
            try {
                val currentToken = tokenManager.currentToken.first()
                
                if (currentToken != null) {
                    val response = if (editingItem != null) {
                        // 更新现有话术
                        knowledgeApiService.updateKbItem(
                            token = currentToken,
                            typeId = typeId,
                            id = editingItem!!.id,
                            question = editingItem!!.question,
                            answer = editContent
                        )
                    } else {
                        // 添加新话术
                        knowledgeApiService.addKbItem(
                            token = currentToken,
                            typeId = typeId,
                            question = title,
                            answer = editContent
                        )
                    }
                    
                    if (response.isSuccess) {
                        snackbarMessage = if (editingItem != null) "保存成功" else "添加成功"
                        showEditDialog = false
                        
                        // 重新获取列表数据
                        val refreshResponse = serverApiUtils.fetchKbItems(typeId)
                        if (refreshResponse.isSuccess && refreshResponse.data != null) {
                            scriptList = refreshResponse.data
                        }
                    } else {
                        snackbarMessage = if (editingItem != null) "保存失败: ${response.msg}" else "添加失败: ${response.msg}"
                    }
                } else {
                    snackbarMessage = "未登录"
                }
            } catch (e: Exception) {
                snackbarMessage = "操作失败: ${e.message}"
            } finally {
                isSaving = false
                showSnackbar = true
            }
        }
    }

    // 删除功能
    val deleteItem: (KnowledgeItem) -> Unit = { item ->
        scope.launch {
            isSaving = true
            try {
                val currentToken = tokenManager.currentToken.first()
                
                if (currentToken != null) {
                    val response = knowledgeApiService.deleteKbItem(
                        token = currentToken,
                        id = item.id
                    )
                    
                    if (response.isSuccess) {
                        snackbarMessage = "删除成功"
                        
                        // 重新获取列表数据
                        val refreshResponse = serverApiUtils.fetchKbItems(typeId)
                        if (refreshResponse.isSuccess && refreshResponse.data != null) {
                            scriptList = refreshResponse.data                        }
                    } else {
                        snackbarMessage = "删除失败: ${response.msg}"
                    }
                } else {
                    snackbarMessage = "未登录"
                }
            } catch (e: Exception) {
                snackbarMessage = "删除失败: ${e.message}"
            } finally {
                isSaving = false
                showSnackbar = true
            }
        }
    }

    // 显示Snackbar
    LaunchedEffect(showSnackbar) {
        if (showSnackbar && snackbarMessage != null) {
            snackbarHostState.showSnackbar(
                message = snackbarMessage!!,
                duration = SnackbarDuration.Short
            )
            showSnackbar = false
            snackbarMessage = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
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
                    IconButton(
                        onClick = { 
                            editingItem = null
                            editContent = ""
                            showEditDialog = true 
                        },
                        enabled = !isLoading && !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加话术"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val listState = rememberLazyListState()
            
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(scriptList) { item ->
                    KnowledgeScriptCard(
                        item = item,
                        onDeleteClick = { deleteItem(item) },
                        onEditClick = {
                            scope.launch {
                                try {
                                    // 先获取最新的answer内容
                                    val response = serverApiUtils.fetchKbById(item.typeId, item.id)
                                    if (response.isSuccess && response.data?.isNotEmpty() == true) {
                                        val latestItem = response.data.first()
                                        editingItem = item
                                        editContent = latestItem.answer // 使用最新的answer
                                        showEditDialog = true
                                    } else {
                                        // 如果获取失败，使用原有的answer
                                        editingItem = item
                                        editContent = item.answer
                                        showEditDialog = true
                                        snackbarMessage = "获取最新内容失败，使用缓存内容: ${response.msg}"
                                        showSnackbar = true
                                    }
                                } catch (e: Exception) {
                                    // 出现异常时使用原有的answer
                                    editingItem = item
                                    editContent = item.answer
                                    showEditDialog = true
                                    snackbarMessage = "获取最新内容异常: ${e.message}"
                                    showSnackbar = true
                                }
                            }
                        },
                        currentSwipeId = currentSwipeId,
                        onSwipeStateChange = { newSwipeId ->
                            currentSwipeId = newSwipeId
                        }
                    )
                }
            }

            // 添加新项后自动滚动到新增位置
            val previousListSize = remember { mutableStateOf(scriptList.size) }
            LaunchedEffect(scriptList.size) {
                if (scriptList.size > previousListSize.value) {
                    val lastItemIndex = scriptList.size - 1
                    if (lastItemIndex >= 0) {
                        listState.animateScrollToItem(lastItemIndex)
                    }
                }
                previousListSize.value = scriptList.size
            }
            
            // 加载遮罩
            if (isLoading) {
                Dialog(
                    onDismissRequest = { },
                    properties = DialogProperties(
                        dismissOnBackPress = false,
                        dismissOnClickOutside = false
                    )
                ) {
                    Card(
                        modifier = Modifier.size(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "加载中...",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(if (editingItem != null) "编辑话术" else "添加话术") },
            text = {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextField(
                        value = editContent,
                        onValueChange = { editContent = it },
                        label = { Text("话术内容") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        maxLines = 8,
                        enabled = !isSaving
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = saveContent,
                    enabled = !isSaving && editContent.isNotBlank()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("保存")
                    }
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false },
                    enabled = !isSaving
                ) {
                    Text("取消")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

// 新的KnowledgeItem卡片组件
@Composable
fun KnowledgeScriptCard(
    item: KnowledgeItem,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    currentSwipeId: Number?,
    onSwipeStateChange: (Number?) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeOffset = -300f

    // 监听当前侧滑状态变化，如果其他卡片被侧滑，重置此卡片
    LaunchedEffect(currentSwipeId) {
        if (currentSwipeId != item.id) {
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 背景删除按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onDeleteClick()
                        offsetX = 0f
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .zIndex(2f)
                ) {
//                    Icon(
//                        imageVector = Icons.Default.Delete,
//                        contentDescription = "删除",
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }

        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .zIndex(3f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val shouldOpen = offsetX < maxSwipeOffset / 2
                            if (shouldOpen) {
                                offsetX = maxSwipeOffset
                                onSwipeStateChange(item.id)
                            } else {
                                offsetX = 0f
                                onSwipeStateChange(null)
                            }
                        }
                    ) { change, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(maxSwipeOffset, 0f)
                        change.consume()

                        if (currentSwipeId == null && offsetX != 0f) {
                            onSwipeStateChange(item.id)
                        }
                    }
                }
                .clickable(
                    enabled = offsetX == 0f,
                    onClick = onEditClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 时间戳格式化显示
                if (item.updateTime > 0) {
                    val updateTimeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.updateTime))
                    Text(
                        text = "更新于: $updateTimeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                } else {
                    val createTimeFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.createTime))
                    Text(
                        text = "创建于: $createTimeFormatted",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

// 保留原有的LiveScriptItem版本作为备用
@Composable
fun ScriptCard(
    item: LiveScriptItem,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    currentSwipeId: Int?,
    onSwipeStateChange: (Int?) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeOffset = -300f

    LaunchedEffect(currentSwipeId) {
        if (currentSwipeId != item.id) {
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // 背景删除按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onDeleteClick()
                        offsetX = 0f
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .zIndex(2f)
                ) {
//                    Icon(
//                        imageVector = Icons.Default.Delete,
//                        contentDescription = "删除",
//                        modifier = Modifier.size(16.dp)
//                    )
//                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }

        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .zIndex(3f)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val shouldOpen = offsetX < maxSwipeOffset / 2
                            if (shouldOpen) {
                                offsetX = maxSwipeOffset
                                onSwipeStateChange(item.id)
                            } else {
                                offsetX = 0f
                                onSwipeStateChange(null)
                            }
                        }
                    ) { change, dragAmount ->
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(maxSwipeOffset, 0f)
                        change.consume()

                        if (currentSwipeId == null && offsetX != 0f) {
                            onSwipeStateChange(item.id)
                        }
                    }
                }
                .clickable(
                    enabled = offsetX == 0f,
                    onClick = onEditClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "创建于: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(item.createdAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}