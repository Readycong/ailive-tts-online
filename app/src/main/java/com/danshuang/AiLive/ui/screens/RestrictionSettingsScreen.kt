package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestrictionSettingsScreen(
    navController: NavigationState,
    restrictionType: String
) {
    // 限制项列表状态
    var restrictionItems by remember { 
        mutableStateOf(
            when (restrictionType) {
                "nickname" -> listOf(
                    "广告号", "机器人", "测试用户", "管理员", "客服号"
                )
                else -> listOf(
                    "广告链接", "联系方式", "敏感词汇", "辱骂言论", "垃圾信息"
                )
            }.sorted()
        ) 
    }
    
    // 新输入项状态
    var newItemText by remember { mutableStateOf("") }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var showDuplicateWarning by remember { mutableStateOf(false) }
    var duplicateItem by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // 自动隐藏重复提示
    LaunchedEffect(showDuplicateWarning) {
        if (showDuplicateWarning) {
            delay(3000)
            showDuplicateWarning = false
        }
    }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                    IconButton(
                        onClick = {
                            // 刷新功能 - 重置为默认列表
                            restrictionItems = when (restrictionType) {
                                "nickname" -> listOf(
                                    "广告号", "机器人", "测试用户", "管理员", "客服号"
                                )
                                else -> listOf(
                                    "广告链接", "联系方式", "敏感词汇", "辱骂言论", "垃圾信息"
                                )
                            }.sorted()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "刷新"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
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
                                        coroutineScope.launch {
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
                                    coroutineScope.launch {
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