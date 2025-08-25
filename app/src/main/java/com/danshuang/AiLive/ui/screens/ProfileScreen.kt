package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import kotlinx.coroutines.launch
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.ui.viewinterop.AndroidView

// 用户数据类
data class UserInfo(
    var nickname: String = "用户昵称",
    val phoneNumber: String = "13800138000",
    val avatarUrl: String = "" // 实际应用中使用
)

// 模拟服务数据
data class SubscriptionInfo(
    val expireDate: String = "2023-12-31",
    val isExpired: Boolean = false
)

// 菜单项数据类
data class MenuItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    onNavigateToChangePassword: () -> Unit = {},
    onNavigateToHelp: () -> Unit = {},
    onNavigateToCardActivation: () -> Unit = {}
) {
    val context = LocalContext.current
    val loginRepository = remember { LoginRepository(context) }
    val scope = rememberCoroutineScope()
    var showHelpDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showCacheDialog by remember { mutableStateOf(false) }
    var cacheCleaned by remember { mutableStateOf(false) }
    
    // 用户数据和状态
    val userInfo = remember { UserInfo() }
    val subscriptionInfo = remember { SubscriptionInfo() }
    var showNicknameDialog by remember { mutableStateOf(false) }
    var newNickname by remember { mutableStateOf(userInfo.nickname) }
    
    // 菜单列表
    val menuItems = remember {
        listOf(
            MenuItem(
                icon = Icons.Filled.Lock,
                title = "修改密码",
                onClick = { 
                    onNavigateToChangePassword()
                }
            ),
            MenuItem(
                icon = Icons.Filled.Search,
                title = "使用帮助",
                onClick = { onNavigateToHelp() }
            ),
            MenuItem(
                icon = Icons.Filled.Email,
                title = "联系我们",
                onClick = { showContactDialog = true }
            ),
            MenuItem(
                icon = Icons.Filled.Refresh,
                title = "清理缓存",
                onClick = { showCacheDialog = true }
            )
        )
    }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                title = {
                    Text("我的")
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 用户信息区域
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 头像
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { /* 更换头像逻辑 */ },
                        contentAlignment = Alignment.Center
                    ) {
                        // 实际应用中使用Image加载网络图片
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = "用户头像",
                            modifier = Modifier.size(60.dp),
                            tint = Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // 昵称（可点击）
                    Text(
                        text = userInfo.nickname,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            newNickname = userInfo.nickname
                            showNicknameDialog = true
                        }
                    )
                    
                    // 手机号码
                    Text(
                        text = userInfo.phoneNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 会员服务卡片
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "会员服务",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "到期时间",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = subscriptionInfo.expireDate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (subscriptionInfo.isExpired) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                // Button(
                                //     onClick = { /* 立即续费逻辑 */ },
                                //     modifier = Modifier.padding(bottom = 8.dp),
                                //     colors = ButtonDefaults.buttonColors(
                                //         containerColor = MaterialTheme.colorScheme.primary
                                //     )
                                // ) {
                                //     Text("立即续费")
                                // }
                                
                                Button(
                                    onClick = { onNavigateToCardActivation() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Text("卡密激活")
                                }
                            }
                        }
                    }
                }
            }
            
            // 菜单列表
            items(menuItems) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { item.onClick() }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "箭头",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Divider()
            }
            
            // 退出按钮
            item {
                Button(
                    onClick = {
                        scope.launch {
                            loginRepository.clearLoginInfo()
                            onLogout()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "退出登录",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("退出APP")
                }
            }
        }
    }
    
    // 使用帮助对话框
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("使用帮助") },
            text = {
                // WebView嵌入帮助页面
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            loadUrl("https://www.hc2099.com/android/v2.html")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
    
    // 联系我们对话框
    if (showContactDialog) {
        AlertDialog(
            onDismissRequest = { showContactDialog = false },
            title = { Text("联系我们") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("客服电话: 400-123-4567")
                    Text("客服邮箱: support@hc2099.com")
                    Text("工作时间: 周一至周五 9:00-18:00")
                    Text("地址: 厦门市思明区创想大厦B座1606")
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactDialog = false }) {
                    Text("确定")
                }
            }
        )
    }
    
    // 昵称修改对话框
    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("修改昵称") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TextField(
                        value = newNickname,
                        onValueChange = { newNickname = it },
                        label = { Text("请输入新昵称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    userInfo.nickname = newNickname
                    showNicknameDialog = false
                    Toast.makeText(context, "昵称修改成功", Toast.LENGTH_SHORT).show()
                }) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 清理缓存对话框
    if (showCacheDialog) {
        AlertDialog(
            onDismissRequest = { showCacheDialog = false },
            title = { Text("清理缓存") },
            text = {
                if (cacheCleaned) {
                    Text("缓存清理成功！")
                } else {
                    Text("确定要清理缓存和用户数据吗？此操作不可恢复。")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (!cacheCleaned) {
                        // 清理缓存逻辑
                        scope.launch {
                            loginRepository.clearLoginInfo()
                            cacheCleaned = true
                        }
                    } else {
                        showCacheDialog = false
                        cacheCleaned = false
                    }
                }) {
                    Text(if (cacheCleaned) "确定" else "清理")
                }
            },
            dismissButton = if (!cacheCleaned) {
                { 
                    TextButton(onClick = { showCacheDialog = false }) {
                        Text("取消")
                    }
                }
            } else null
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    AiLiveTheme {
        ProfileScreen()
    }
}