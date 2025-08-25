package com.danshuang.AiLive.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.tooling.preview.Preview
import com.danshuang.AiLive.R
import com.danshuang.AiLive.navigation.NavigationState
import android.webkit.WebView
import android.webkit.WebViewClient

// 绑定步骤枚举
enum class BindingStep {
    URL_INPUT,      // 步骤一：URL输入
    ACCOUNT_PREVIEW // 步骤二：账户预览
}

// 模拟用户账户信息
data class UserAccountInfo(
    val avatar: String = "https://via.placeholder.com/80",
    val nickname: String = "示例用户",
    val isLive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBindingScreen(
    navController: NavigationState
) {
    var accountUrl by remember { mutableStateOf(TextFieldValue("") )}
    var isLoading by remember { mutableStateOf(false) }
    var bindingResult by remember { mutableStateOf<String?>(null) }
    var bindingTrigger by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(BindingStep.URL_INPUT) }
    var accountInfo by remember { mutableStateOf<UserAccountInfo?>(null) }
    var showWebView by remember { mutableStateOf(false) }
    var showDropdownMenu by remember { mutableStateOf(false) }

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
    
    // 模拟获取账户信息
    LaunchedEffect(currentStep) {
        if (currentStep == BindingStep.ACCOUNT_PREVIEW && accountInfo == null) {
            isLoading = true
            kotlinx.coroutines.delay(1500) // 模拟加载时间
            accountInfo = UserAccountInfo(
                nickname = "抖音用户${(Math.random() * 10000).toInt()}",
                isLive = Math.random() > 0.5
            )
            isLoading = false
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
                },
                actions = {
                    // 下拉菜单按钮
                    Box {
                        IconButton(onClick = { showDropdownMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showDropdownMenu,
                            onDismissRequest = { showDropdownMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("网页调试") },
                                onClick = {
                                    showWebView = !showWebView
                                    showDropdownMenu = false
                                },
                                leadingIcon = {
                                    Icon(Icons.Filled.BugReport, contentDescription = "网页调试")
                                }
                            )
                        }
                    }
                }
            )
        }
    ) {
        innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // WebView 调试区域 - 在所有内容之前显示
            item {
                WebViewControlSection(
                    url = accountUrl.text.ifEmpty { "https://www.douyin.com" },
                    isVisible = showWebView,
                    onVisibilityChange = { showWebView = it }
                )
            }
            
            // 步骤指示器
            item {
                StepIndicator(
                    currentStep = currentStep,
                    totalSteps = 2
                )
            }
            
            // 根据当前步骤显示不同内容
            item {
                when (currentStep) {
                    BindingStep.URL_INPUT -> {
                        UrlInputStep(
                            accountUrl = accountUrl,
                            onUrlChange = { accountUrl = it },
                            onNextStep = {
                                if (accountUrl.text.isNotBlank()) {
                                    currentStep = BindingStep.ACCOUNT_PREVIEW
                                }
                            },
                            isEnabled = accountUrl.text.isNotBlank()
                        )
                    }
                    BindingStep.ACCOUNT_PREVIEW -> {
                        AccountPreviewStep(
                            accountInfo = accountInfo,
                            isLoading = isLoading,
                            onBack = {
                                currentStep = BindingStep.URL_INPUT
                                accountInfo = null
                            },
                            onBind = {
                                isLoading = true
                                bindingTrigger = true
                            },
                            bindingResult = bindingResult
                        )
                    }
                }
            }

            // 帮助信息
            item {
                HelpSection()
            }
        }
    }
}

// URL输入步骤组件
@Composable
private fun UrlInputStep(
    accountUrl: TextFieldValue,
    onUrlChange: (TextFieldValue) -> Unit,
    onNextStep: () -> Unit,
    isEnabled: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 步骤标题
        Text(
            text = "步骤一：填写抖音账户主页地址",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 输入框标签
        Text(
            text = "请输入您的抖音账户主页地址：",
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
                onValueChange = onUrlChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .defaultMinSize(minHeight = 72.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                minLines = 3,
                maxLines = 5,
                decorationBox = { innerTextField ->
                    if (accountUrl.text.isEmpty()) {
                        Text(
                            text = "例如：#在抖音，记录美好生活#【拼音账号名称】正在直播，来和我一起支持Ta吧。复制下方链接，打开【抖音】，直接观看直播！ https://v.douyin.com/xxxxxx/ 5@7.com :0am",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            )
        }
        
        // 下一步按钮
        Button(
            onClick = onNextStep,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = isEnabled
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "下一步",
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = "下一步",
                    modifier = Modifier.size(20.dp)
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

// 步骤指示器组件
@Composable
private fun StepIndicator(
    currentStep: BindingStep,
    totalSteps: Int
) {
    val currentStepNumber = when (currentStep) {
        BindingStep.URL_INPUT -> 1
        BindingStep.ACCOUNT_PREVIEW -> 2
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSteps) {
                // 步骤圆圈
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (i <= currentStepNumber) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (i < currentStepNumber) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "已完成",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = i.toString(),
                            color = if (i <= currentStepNumber) Color.White else MaterialTheme.colorScheme.outline,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // 连接线
                if (i < totalSteps) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(2.dp)
                            .background(
                                if (i < currentStepNumber) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                    )
                }
            }
        }
    }
}

// 账户预览步骤组件
@Composable
private fun AccountPreviewStep(
    accountInfo: UserAccountInfo?,
    isLoading: Boolean,
    onBack: () -> Unit,
    onBind: () -> Unit,
    bindingResult: String?
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 步骤标题
        Text(
            text = "步骤二：确认账户信息",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        // 账户信息卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "正在加载账户信息...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                accountInfo?.let { info ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 头像
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "头像",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // 账户信息
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = info.nickname,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            // 直播状态
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (info.isLive) Color.Green else Color.Gray
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (info.isLive) "正在直播" else "未开播",
                                    fontSize = 14.sp,
                                    color = if (info.isLive) Color.Green else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            Text(
                                text = "抖音认证账户",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
        
        // 绑定结果提示
        bindingResult?.let {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (it.contains("成功")) 
                        MaterialTheme.colorScheme.primaryContainer
                    else 
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (it.contains("成功")) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (it.contains("成功")) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = it,
                        fontSize = 16.sp,
                        color = if (it.contains("成功")) 
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else 
                            MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        // 按钮组
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 返回按钮
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "返回",
                        fontSize = 16.sp
                    )
                }
            }
            
            // 立即绑定按钮
            Button(
                onClick = onBind,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                enabled = !isLoading && accountInfo != null && bindingResult == null
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Link,
                            contentDescription = "绑定",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "立即绑定",
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

// WebView控制组件
@Composable
private fun WebViewControlSection(
    url: String,
    isVisible: Boolean,
    onVisibilityChange: (Boolean) -> Unit = {}
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(durationMillis = 300)
        ) + fadeIn(animationSpec = tween(durationMillis = 300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(durationMillis = 300)
        ) + fadeOut(animationSpec = tween(durationMillis = 300))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // WebView标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "网页",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "抖音主页预览",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "调试模式",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isVisible,
                        onCheckedChange = onVisibilityChange,
                        modifier = Modifier.height(24.dp),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                            uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )
                }
                
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                
                // WebView内容 - 使用PC浏览器模式
                var webViewHeight by remember { mutableStateOf(400.dp) }
                
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    // 执行JavaScript获取网页内容高度
                                    view?.evaluateJavascript(
                                        "(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, 400);})();"
                                    ) { result ->
                                        try {
                                            val height = result.toFloatOrNull() ?: 400f
                                            val density = context.resources.displayMetrics.density
                                            // 限制最大高度为600dp，最小高度为400dp
                                            val newHeight = ((height / density).coerceIn(400f, 600f)).dp
                                            webViewHeight = newHeight
                                        } catch (e: Exception) {
                                            webViewHeight = 400.dp
                                        }
                                    }
                                }
                            }
                            settings.apply {
                                javaScriptEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                setSupportZoom(true)
                                // 设置为PC浏览器模式
                                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            }
                            loadUrl(url.ifEmpty { "https://www.douyin.com" })
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(webViewHeight)
                        .padding(8.dp)
                )
            }
        }
    }
}

// 帮助信息组件
@Composable
private fun HelpSection() {
    Text(
        text = "帮助信息",
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    
    var webViewHeight by remember { mutableStateOf(500.dp) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            // 执行JavaScript获取网页内容高度
                            view?.evaluateJavascript(
                                "(function(){return Math.max(document.body.scrollHeight, document.documentElement.scrollHeight, 500);})();"
                            ) { result ->
                                try {
                                    val height = result.toFloatOrNull() ?: 500f
                                    val density = context.resources.displayMetrics.density
                                    // 限制最大高度为800dp，最小高度为500dp
                                    val newHeight = ((height / density).coerceIn(500f, 800f)).dp
                                    webViewHeight = newHeight
                                } catch (e: Exception) {
                                    webViewHeight = 500.dp
                                }
                            }
                        }
                    }
                    settings.javaScriptEnabled = true
                    loadUrl("https://vip.hc2099.com/android/help/v2/dy-user-home.htm")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(webViewHeight)
        )
    }
}