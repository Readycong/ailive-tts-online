package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.navigation.NavigationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GreetingSettingsScreen(
    navController: NavigationState
) {
    // 招呼设置状态
    var isActiveGreeting by remember { mutableStateOf(true) }
    var maxOnlineUsers by remember { mutableStateOf("20") }
    var responseDelay by remember { mutableStateOf("5") }

    // 输入验证错误状态
    var maxOnlineUsersError by remember { mutableStateOf(false) }
    var responseDelayError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("招呼设置") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // 验证输入
                            val maxUsers = maxOnlineUsers.toIntOrNull()
                            val delay = responseDelay.toIntOrNull()
                            
                            maxOnlineUsersError = maxUsers == null || maxUsers < 0 || maxUsers > 100
                            responseDelayError = delay == null || delay < 0 || delay > 20
                            
                            if (!maxOnlineUsersError && !responseDelayError) {
                                // 保存设置（这里可以添加实际的保存逻辑）
                                navController.navigateUp()
                            }
                        }
                    ) {
                        Text("保存", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
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
                            onCheckedChange = { isActiveGreeting = it }
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
                        text = "响应延迟",
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
                        supportingText = {
                            if (responseDelayError) {
                                Text("请输入0-20之间的数字")
                            } else {
                                Text("0为不延迟")
                            }
                        }
                    )
                }
            }
        }
    }
}