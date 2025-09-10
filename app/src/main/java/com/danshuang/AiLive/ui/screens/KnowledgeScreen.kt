package com.danshuang.AiLive.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.background
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.navigation.NavigationState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.data.model.KnowledgeItem
import kotlinx.coroutines.launch



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    navController: NavigationState? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 初始化API服务
    val tokenManager = remember { TokenManager(LoginRepository(context)) }
    val loginRepository = remember { LoginRepository(context) }
    val serverApiUtils = remember {
        ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository)
    }
    
    // 页面状态
    var currentSwipeId by remember { mutableStateOf<Number?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var knowledgeList by remember { mutableStateOf<List<KnowledgeItem>>(emptyList()) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    
    // 编辑对话框状态
    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var editQuestion by remember { mutableStateOf("") }
    var editAnswer by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    
    // 分类设置：仅保留"我的知识库"
    val categories = listOf("我的知识库")
    val selectedTabIndex = 0 // 固定为0，因为只有一个分类
    
    // 获取当前用户信息
    var currentUser by remember { mutableStateOf<com.danshuang.AiLive.data.model.LoggedInUser?>(null) }

    // 页面初始化时加载用户信息和知识库数据
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // 获取当前用户信息
                currentUser = serverApiUtils.getCurrentUser()
                
                if (currentUser != null) {
                    // 加载知识库数据
                    isLoading = true
                    val response = serverApiUtils.fetchKbItems("QA")
                    if (response.isSuccess && response.data != null) {
                        knowledgeList = response.data
                    } else {
                        snackbarMessage = "加载我的知识库失败：${response.msg}"
                    }
                } else {
                    snackbarMessage = "用户未登录，请先登录"
                }
            } catch (e: Exception) {
                snackbarMessage = "加载我的知识库失败：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    // 新增知识条目
    fun addNewItem() {
        scope.launch {
            isSaving = true
            try {
                val response = serverApiUtils.updateKbItem(
                    typeId = "QA",
                    id = 0, // 新增时id为0
                    question = editQuestion.trim(),
                    answer = editAnswer.trim()
                )
                
                if (response.isSuccess) {
                    snackbarMessage = "添加成功"
                    showEditDialog = false
                    editQuestion = ""
                    editAnswer = ""
                    editingItem = null
                    
                    // 重新加载数据
                    val refreshResponse = serverApiUtils.fetchKbItems("QA")
                    if (refreshResponse.isSuccess && refreshResponse.data != null) {
                        knowledgeList = refreshResponse.data
                    }
                } else {
                    snackbarMessage = "添加失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "添加失败：${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
    
    // 编辑知识条目
    fun editItem(item: KnowledgeItem) {
        scope.launch {
            try {
                // 先获取最新数据
                val response = serverApiUtils.fetchKbById("QA", item.id)
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
    fun saveEdit() {
        scope.launch {
            isSaving = true
            try {
                val response = serverApiUtils.updateKbItem(
                    typeId = "QA",
                    id = editingItem!!.id,
                    question = editQuestion.trim(),
                    answer = editAnswer.trim()
                )
                
                if (response.isSuccess) {
                    snackbarMessage = "更新成功"
                    showEditDialog = false
                    editQuestion = ""
                    editAnswer = ""
                    editingItem = null
                    
                    // 重新加载数据
                    val refreshResponse = serverApiUtils.fetchKbItems("QA")
                    if (refreshResponse.isSuccess && refreshResponse.data != null) {
                        knowledgeList = refreshResponse.data
                    }
                } else {
                    snackbarMessage = "更新失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "更新失败：${e.message}"
            } finally {
                isSaving = false
            }
        }
    }
    
    // 删除知识条目
    fun deleteItem(item: KnowledgeItem) {
        scope.launch {
            try {
                val response = serverApiUtils.deleteKbItem(
                    id = item.id
                )
                
                if (response.isSuccess) {
                    snackbarMessage = "删除成功"
                    knowledgeList = knowledgeList.filterNot { it.id == item.id }
                    currentSwipeId = null
                } else {
                    snackbarMessage = "删除失败：${response.msg}"
                }
            } catch (e: Exception) {
                snackbarMessage = "删除失败：${e.message}"
            }
        }
    }
    
    // Snackbar消息处理
    snackbarMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            snackbarMessage = null
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ),
                title = {
                    Text("知识库")
                },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            editingItem = null
                            editQuestion = ""
                            editAnswer = ""
                            showEditDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加问答"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // 分类标签（仅显示"我的知识库"）
                // SecondaryTabRow(
                //     selectedTabIndex = selectedTabIndex,
                //     containerColor = MaterialTheme.colorScheme.surface
                // ) {
                //     categories.forEachIndexed { index, category ->
                //         Tab(
                //             selected = selectedTabIndex == index,
                //             onClick = { /* 固定选中，无需切换 */ },
                //             text = { Text(category) }
                //         )
                //     }
                // }

                // 知识条目列表
                val listState = rememberLazyListState()

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
                ) {
                    items(
                        items = knowledgeList,
                        key = { it.id }
                    ) { item ->
                        KnowledgeCard(
                            item = item,
                            onDeleteClick = {
                                deleteItem(item)
                            },
                            onEditClick = {
                                editItem(item)
                            },
                            currentSwipeId = currentSwipeId,
                            onSwipeStateChange = { newSwipeId ->
                                currentSwipeId = newSwipeId
                            }
                        )
                    }
                }

                // 添加新项后自动滚动到底部
                val previousListSize = remember { mutableStateOf(knowledgeList.size) }
                LaunchedEffect(knowledgeList.size) {
                    if (knowledgeList.size > previousListSize.value) {
                        val lastItemIndex = knowledgeList.size - 1
                        if (lastItemIndex >= 0) {
                            listState.animateScrollToItem(lastItemIndex)
                        }
                    }
                    previousListSize.value = knowledgeList.size
                }
            }
        }
    }

    // 编辑对话框
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    showEditDialog = false
                }
            },
            title = { Text(if (editingItem != null) "编辑知识条目" else "新增知识条目") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedTextField(
                        value = editQuestion,
                        onValueChange = { editQuestion = it },
                        label = { Text("问题") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )
                    OutlinedTextField(
                        value = editAnswer,
                        onValueChange = { editAnswer = it },
                        label = { Text("答案") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5,
                        enabled = !isSaving
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingItem != null) {
                            saveEdit()
                        } else {
                            addNewItem()
                        }
                    },
                    enabled = !isSaving && editQuestion.isNotBlank() && editAnswer.isNotBlank()
                ) {
                    if (isSaving) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp)
                            )
                            Text("保存中...")
                        }
                    } else {
                        Text("保存")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        editQuestion = ""
                        editAnswer = ""
                        editingItem = null
                    },
                    enabled = !isSaving
                ) {
                    Text("取消")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
fun KnowledgeCard(
    item: KnowledgeItem,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    currentSwipeId: Number?,
    onSwipeStateChange: (Number?) -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    val maxSwipeOffset = -300f // 最大侧滑距离

    // 监听当前侧滑状态变化，如果其他卡片被侧滑，重置此卡片
    LaunchedEffect(currentSwipeId) {
        if (currentSwipeId != item.id) {
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // 保证背景和卡片高度一致
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
                        offsetX = 0f // 重置位置
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .zIndex(2f) // 保证按钮在最上层
                ) {
                    Text("删除")
                }
            }
        }

        // 前景卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .zIndex(3f) // 卡片在按钮之上，但通过offset让出空间
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // 滑动超过一半时显示删除按钮
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

                        // 当开始滑动时，如果当前没有打开状态，则设置当前卡片为滑动状态
                        if (currentSwipeId == null && offsetX != 0f) {
                            onSwipeStateChange(item.id)
                        }
                    }
                }
                .clickable(
                    enabled = offsetX == 0f, // 只有在未侧滑状态下才能点击编辑
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
                    text = item.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun KnowledgeScreenPreview() {
    AiLiveTheme {
        KnowledgeScreen()
    }
}