package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.api.ServerApiUtilsV2
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentEditScreen(
    navController: NavigationState,
    contentKey: String,
    initialContent: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // 初始化依赖项
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }
    val apiUtils = remember { ServerApiUtilsV2.createFromContext(context, tokenManager, loginRepository) }
    
    // 状态管理
    var content by remember { mutableStateOf(initialContent) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    
    // Snackbar Host State
    val snackbarHostState = remember { SnackbarHostState() }
    
    val title = when (contentKey) {
        "shop_name" -> "门店品牌"
        "business_scope" -> "产品业务"
        "shop_activity" -> "推广活动"
        "customer_scope" -> "客户群体"
        "common_knowledge" -> "门店地址"
        else -> "内容编辑"
    }
    
    // 获取当前用户信息和douyinId
    LaunchedEffect(contentKey) {
        isLoading = true
        try {
            val currentUser = apiUtils.getCurrentUser()
            val douyinId = currentUser?.userProfile?.boundAccount?.userId
            
            if (douyinId != null && douyinId.isNotEmpty()) {
                val response = apiUtils.getConfig(douyinId, contentKey)
                if (response.isSuccess && response.data != null) {
                    content = response.data.value
                } else {
                    // 如果获取失败，使用初始内容
                    content = initialContent
                    snackbarMessage = "获取配置失败: ${response.msg}"
                    showSnackbar = true
                }
            } else {
                content = initialContent
                snackbarMessage = "未找到绑定的抖音账号"
                showSnackbar = true
            }
        } catch (e: Exception) {
            content = initialContent
            snackbarMessage = "加载失败: ${e.message}"
            showSnackbar = true
        } finally {
            isLoading = false
        }
    }
    
    // 保存功能
    val saveContent: () -> Unit = {
        scope.launch {
            isSaving = true
            try {
                val currentUser = apiUtils.getCurrentUser()
                val douyinId = currentUser?.userProfile?.boundAccount?.userId
                
                if (douyinId != null && douyinId.isNotEmpty()) {
                    val response = apiUtils.updateConfig(douyinId, contentKey, content)
                    if (response.isSuccess) {
                        snackbarMessage = "保存成功"
                        // 保存成功后不返回，继续编辑
                    } else {
                        snackbarMessage = "保存失败: ${response.msg}"
                    }
                } else {
                    snackbarMessage = "未找到绑定的抖音账号"
                }
            } catch (e: Exception) {
                snackbarMessage = "保存失败: ${e.message}"
            } finally {
                isSaving = false
                showSnackbar = true
            }
        }
        Unit
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
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = saveContent,
                        enabled = !isSaving && !isLoading,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("保存", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (content.isEmpty()) {
                                        Text(
                                            text = "请输入${title}内容...",
                                            style = TextStyle(
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            enabled = !isLoading
                        )
                    }
                }
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
                        modifier = Modifier
                            .size(120.dp),
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
}