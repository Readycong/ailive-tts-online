package com.danshuang.AiLive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import com.danshuang.AiLive.R
import com.danshuang.AiLive.navigation.NavigationState
import android.webkit.WebView
import android.webkit.WebViewClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBindingScreen(
    navController: NavigationState
) {
    var accountUrl by remember { mutableStateOf(TextFieldValue("") )}
    var isLoading by remember { mutableStateOf(false) }
    var bindingResult by remember { mutableStateOf<String?>(null) }
    var bindingTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(bindingTrigger) {
        if (bindingTrigger) {
            // 实际应用中这里应该是网络请求
            // 延迟2秒模拟请求
            kotlinx.coroutines.delay(2000)
            isLoading = false
            bindingResult = "绑定成功"
            bindingTrigger = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("抖音账户绑定") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) {
        innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 表单标题
            Text(
                text = "绑定您的抖音账户",
                fontSize = 20.sp,
                fontWeight = MaterialTheme.typography.titleLarge.fontWeight
            )

            // 输入框标签
            Text(
                text = "抖音账户主页地址",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // 多行输入框
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                BasicTextField(
                    value = accountUrl,
                    onValueChange = { accountUrl = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    decorationBox = { innerTextField ->
                        if (accountUrl.text.isEmpty()) {
                            Text(
                                text = "请输入抖音账户主页地址",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // 绑定按钮
            Button(
                onClick = {
                    // 触发绑定逻辑
                    isLoading = true
                    bindingTrigger = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = accountUrl.text.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "加载并绑定",
                        fontSize = 16.sp
                    )
                }
            }

            // 绑定结果提示
            bindingResult?.let {
                Text(
                    text = it,
                    color = if (it.contains("成功")) Color.Green else Color.Red,
                    fontSize = 16.sp
                )
            }

            // WebView帮助页面
            Text(
                text = "帮助信息",
                fontSize = 18.sp,
                fontWeight = MaterialTheme.typography.titleLarge.fontWeight
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            loadUrl("https://www.example.com/help") // 替换为实际的帮助页面URL
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AccountBindingScreenPreview() {
    MaterialTheme {
        AccountBindingScreen(navController = remember { NavigationState() })
    }
}