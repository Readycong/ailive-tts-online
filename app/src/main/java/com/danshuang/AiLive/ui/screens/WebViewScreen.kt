package com.danshuang.AiLive.ui.screens

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
    val loginRepository = remember { LoginRepository(context) }
    val authToken by loginRepository.authToken.collectAsState(initial = null)
    
    // 获取配置
    val wapBaseUrl = remember { AppConfig.getWapBaseUrl(context) }
    val siteId = remember { AppConfig.getSiteId(context) }
    
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // 提取目标站点的origin（用于localStorage初始化）
    val targetOrigin = remember(url) {
        try {
            val urlObj = URL(url)
            "${urlObj.protocol}://${urlObj.host}${if (urlObj.port != -1 && urlObj.port != urlObj.defaultPort) ":${urlObj.port}" else ""}"
        } catch (e: Exception) {
            wapBaseUrl // 如果解析失败，使用wapBaseUrl
        }
    }

    // 在加载URL前设置storage中的wapToken和site-id
    LaunchedEffect(authToken, webView) {
        authToken?.let { token ->
            webView?.let { view ->
                // 等待WebView准备好后，通过JavaScript设置localStorage
                // 初始化的站点为wap_base_url配置的站点
                val jsCode = """
                    (function() {
                        try {
                            // 首先尝试在目标站点设置localStorage
                            localStorage.setItem('wapToken', '$token');
                            localStorage.setItem('site-id', '$siteId');
                            console.log('wapToken and site-id have been set to localStorage');
                            console.log('Target origin: $targetOrigin');
                            console.log('Site ID: $siteId');
                        } catch(e) {
                            console.error('Failed to set localStorage:', e);
                        }
                    })();
                """.trimIndent()
                
                view.evaluateJavascript(jsCode, null)
            }
        }
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
                    WebView(ctx).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                isLoading = false
                                
                                // 页面加载完成后，再次确保token和site-id已设置
                                authToken?.let { token ->
                                    val jsCode = """
                                        (function() {
                                            try {
                                                localStorage.setItem('wapToken', '$token');
                                                localStorage.setItem('site-id', '$siteId');
                                                console.log('wapToken and site-id have been set after page loaded');
                                            } catch(e) {
                                                console.error('Failed to set localStorage after page loaded:', e);
                                            }
                                        })();
                                    """.trimIndent()

                                    view?.evaluateJavascript(jsCode, null)
                                }
                            }

                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, pageUrl, favicon)
                                isLoading = true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                // 允许在当前WebView中加载所有链接
                                return false
                            }
                        }

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            allowContentAccess = true
                            allowFileAccess = true
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

                        // 设置第三方Cookie
                        //CookieManager.setAcceptThirdPartyCookies(this, true)
                        
                        // 保存WebView实例引用
                        webView = this
                        
                        // 加载URL
                        loadUrl(url)
                    }
                },
                update = { view ->
                    // 如果URL变化，重新加载
                    if (view.url != url) {
                        view.loadUrl(url)
                    }
                }
            )

            // 加载指示器
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
