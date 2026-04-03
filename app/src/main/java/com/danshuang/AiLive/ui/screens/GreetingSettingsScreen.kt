package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Save
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.api.service.KnowledgeApiService
import com.danshuang.AiLive.config.AppConfig
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.manager.TokenManager
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingSettingsScreen(
    navController: NavigationState
) {
    // 招呼设置状态
    var isActiveGreeting by remember { mutableStateOf(true) }
    var maxOnlineUsers by remember { mutableStateOf("20") }
    var responseDelay by remember { mutableStateOf("1") }

    // 输入验证错误状态
    var maxOnlineUsersError by remember { mutableStateOf(false) }
    var responseDelayError by remember { mutableStateOf(false) }
    
    // 加载和操作状态
    var isLoading by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    
    // Snackbar状态
    val snackbarHostState = remember { SnackbarHostState() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 初始化API依赖项
    val loginRepository = remember { LoginRepository(context) }
    val tokenManager = remember { TokenManager(loginRepository) }

    val serverApiUtils = remember {
        ServerApiUtilsV2.createFromContext(
            context = context,
            tokenManager = tokenManager,
            loginRepository = loginRepository
        )
    }
    // 用户信息状态
    val userProfile by loginRepository.userProfile.collectAsState(initial = com.danshuang.AiLive.data.model.UserProfile())
    val douyinId = userProfile.boundAccount?.userId ?: ""


    // 页面初始化时加载数据
    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch {
            try {
                val result = serverApiUtils.getGreetingConfig(douyinId)
                if (result.isSuccess && result.data != null) {
                    val config = result.data
                    isActiveGreeting = config.greetStatus == 1
                    maxOnlineUsers = config.maxOnline.toString()
                    responseDelay = config.delaySeconds.toString()
                } else {
                    // 加载失败时显示错误提示但继续显示默认值
                    snackbarHostState.showSnackbar("加载招呼设置失败: ${result.msg}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("加载招呼设置失败: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }
    
    // 保存设置的协程函数
    suspend fun saveSettings() {
        isSaving = true
        try {
            val result = serverApiUtils.updateGreetingConfig(
                douyinId = douyinId,
                greetStatus = if (isActiveGreeting) 1 else 0,
                maxOnline = maxOnlineUsers.toInt(),
                delaySeconds = responseDelay.toInt()
            )
            
            if (result.isSuccess) {
                snackbarHostState.showSnackbar("招呼设置保存成功")
                navController.navigateUp()
            } else {
                snackbarHostState.showSnackbar("保存失败: ${result.msg}")
            }
        } catch (e: Exception) {
            snackbarHostState.showSnackbar("保存失败: ${e.message}")
        } finally {
            isSaving = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("招呼设置") },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        enabled = !isLoading && !isSaving
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = {
                            // 验证输入
                            val maxUsers = maxOnlineUsers.toIntOrNull()
                            val delay = responseDelay.toIntOrNull()
                            
                            maxOnlineUsersError = maxUsers == null || maxUsers <= 0
                            responseDelayError = delay == null || delay < 0 || delay > 20
                            
                            if (!maxOnlineUsersError && !responseDelayError) {
                                // 保存设置
                                scope.launch {
                                    saveSettings()
                                }
                            }
                        },
                        enabled = !isLoading && !isSaving,
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
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 加载状态指示器
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "正在加载招呼设置...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
            // 主动招呼设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "主动招呼",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Switch(
                            checked = isActiveGreeting,
                            onCheckedChange = { isActiveGreeting = it },
                            enabled = !isLoading && !isSaving
                        )
                    }
                    Text(
                        text = "开启后会对沉默与在线用户进行随机点名问候",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // 最大在线人数设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "最大在线人数",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = maxOnlineUsers,
                        onValueChange = {
                            maxOnlineUsers = it
                            maxOnlineUsersError = false
                        },
                        label = { Text("人数") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = maxOnlineUsersError,
                        enabled = !isLoading && !isSaving,
                        supportingText = {
                            if (maxOnlineUsersError) {
                                Text("请输入0-100之间的数字")
                            } else {
                                Text("在线人数超过后不进行点名问候")
                            }
                        }
                    )
                }
            }

            // 响应延迟设置
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "评论响应延迟（秒）",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = responseDelay,
                        onValueChange = {
                            responseDelay = it
                            responseDelayError = false
                        },
                        label = { Text("秒") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = responseDelayError,
                        enabled = !isLoading && !isSaving,
                        supportingText = {
                            if (responseDelayError) {
                                Text("请输入0-20之间的数字")
                            } else {
                                Text("0为不延迟，为同一用户分多条发送评论间隔")
                            }
                        }
                    )
                }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingSettingsScreenPreview() {
    // 注意：Preview中无法使用真实的API调用
    AiLiveTheme {
         GreetingSettingsScreen(
             navController = NavigationState(),
         )
    }
}