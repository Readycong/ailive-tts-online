package com.danshuang.AiLive.ui.screens

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.navigation.NavigationState
import com.danshuang.AiLive.config.AppConfig
import java.net.URL

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    navController: NavigationState,
    url: String,
    title: String = "页面详情"
) {
    val context = LocalContext.current

    // 获取登录仓库实例
    val loginRepository = remember { LoginRepository(context) }
    // 订阅认证token
    val authToken by loginRepository.authToken.collectAsState(initial = null)

    // 获取配置
    val siteId = remember { AppConfig.getSiteId(context) }

    // 确保在加载页面时使用最新的token
    LaunchedEffect(key1 = url, key2 = authToken) {
        // 当URL或token更新时触发
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                title = {
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val webView = WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                if (pageUrl != null) {
                                    Log.i("load page", pageUrl)
                                }
                            }
                        }
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowContentAccess = true
                            allowFileAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            // 启用混合内容模式（如果需要）
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            // 缓存设置
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        // 启用Cookie
                        CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                        }
                    }

                    // 准备请求头，包含token - 使用当前组合时的token值
                    val extraHeaders = mutableMapOf<String, String>()
                    extraHeaders["site-id"] = siteId

                    // 添加认证token到请求头（如果可用）
                    // 在factory中直接使用从组合中获取的authToken值
                    authToken?.takeIf { it.isNotBlank() }?.let { token ->
                        extraHeaders["token"] = token
                        Log.i("TOKEN", token)
                    }

                    // 加载URL带请求头
                    webView.loadUrl(url, extraHeaders.toMap())

                    webView
                },
                update = { view ->
                    // 当token更新时，重新设置请求头
                    val extraHeaders = mutableMapOf<String, String>()
                    extraHeaders["site-id"] = siteId
                    // 添加认证token到请求头（如果可用）
                    authToken?.takeIf { it.isNotBlank() }?.let { token ->
                        extraHeaders["token"] = token
                        Log.i("TOKEN", token)
                    }

                    // 重新加载页面以应用新的token
                    if (view.url?.contains(url) != true) {
                        view.loadUrl(url, extraHeaders.toMap())
                    } else {
                        // 如果URL相同但token变化，重新加载当前页面
                        view.loadUrl(view.url ?: url, extraHeaders.toMap())
                    }
                }
            )
        }
    }
}
