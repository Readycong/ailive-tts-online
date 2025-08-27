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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScriptScreen(
    navController: NavigationState,
    scriptKey: String
) {
    var currentSwipeId by remember { mutableStateOf<Int?>(null) }
    
    val title = when (scriptKey) {
        "WELCOME" -> "问候话术"
        "ROOM" -> "品牌话术"
        "PRODUCT" -> "带货话术"
        "KEEP" -> "留咨话术"
        "ORDER" -> "促成话术"
        "THANKS" -> "感谢话术"
        else -> "eRRRRRor"
    }

    // 话术数据
    var scriptList by remember {
        mutableStateOf(
            when (scriptKey) {
                "WELCOME" -> listOf(
                    LiveScriptItem(1, "问候", "欢迎新进来的家人们，今天是我们直播间开业庆典，全场福利送不停！"),
                    LiveScriptItem(2, "问候", "哈喽各位宝宝们，我是你们的主播小美，今天带来超多美业好物，不要错过哦！"),
                    LiveScriptItem(3, "问候", "欢迎欢迎，今天直播间有专业美容师在线答疑，有任何肌肤问题都可以问！")
                )
                "ROOM" -> listOf(
                    LiveScriptItem(1, "品牌", "我们店是本地5年老店，服务过5000+客户，口碑保证，品质看得见！"),
                    LiveScriptItem(2, "品牌", "专业团队持证上岗，所有产品都是正规渠道，安全有保障，用得放心！")
                )
                "PRODUCT" -> listOf(
                    LiveScriptItem(1, "带货", "我们店是本地5年老店，服务过5000+客户，口碑保证，品质看得见！"),
                    LiveScriptItem(2, "带货", "专业团队持证上岗，所有产品都是正规渠道，安全有保障，用得放心！")
                )
                "KEEP" -> listOf(
                    LiveScriptItem(1, "留咨", "想了解适合自己肤质的护肤品吗？在评论区打个1，我来帮你分析！"),
                    LiveScriptItem(2, "留咨", "有肌肤问题困扰的宝宝们，可以私信留下联系方式，专业顾问一对一服务！")
                )
                "ORDER" -> listOf(
                    LiveScriptItem(1, "促成", "前10名下单的宝宝享受5折优惠，还送精美礼品，数量有限，先到先得！"),
                    LiveScriptItem(2, "促成", "今天直播间专属福利，原价299的体验套餐，现在只要99元！")
                )
                "THANKS" -> listOf(
                    LiveScriptItem(1, "感谢", "感谢今天所有支持我们的宝宝们，你们的信任是我们前进的动力！"),
                    LiveScriptItem(2, "感谢", "谢谢大家的陪伴，有任何问题随时联系客服，我们会第一时间为您解决！")
                )
                else -> emptyList()
            }
        )
    }

    var showEditDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<LiveScriptItem?>(null) }
    var editContent by remember { mutableStateOf("") }

    Scaffold(
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
                    IconButton(onClick = { 
                        editingItem = null
                        editContent = ""
                        showEditDialog = true 
                    }) {
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
                    ScriptCard(
                        item = item,
                        onDeleteClick = {
                            scriptList = scriptList.filterNot { it.id == item.id }
                            currentSwipeId = null
                        },
                        onEditClick = {
                            editingItem = item
                            editContent = item.content
                            showEditDialog = true
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
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingItem != null) {
                            // 编辑现有话术
                            scriptList = scriptList.map {
                                if (it.id == editingItem!!.id) {
                                    it.copy(content = editContent)
                                } else it
                            }
                        } else {
                            // 添加新话术
                            val newItem = LiveScriptItem(
                                id = scriptList.maxOfOrNull { it.id }?.plus(1) ?: 1,
                                category = title,
                                content = editContent
                            )
                            scriptList = scriptList + newItem
                        }
                        showEditDialog = false
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showEditDialog = false }
                ) {
                    Text("取消")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}

@Composable
fun ScriptCard(
    item: LiveScriptItem,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit,
    currentSwipeId: Int?,
    onSwipeStateChange: (Int?) -> Unit
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
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "删除",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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