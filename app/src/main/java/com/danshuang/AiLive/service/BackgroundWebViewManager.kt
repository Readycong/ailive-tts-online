package com.danshuang.AiLive.service

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import com.danshuang.AiLive.util.Utils

// 添加WebViewJsInterface导入
import com.danshuang.AiLive.service.WebViewJsInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope

/**
 * 后台WebView管理器
 * 用于在LiveChattingService中创建后台WebView，加载抖音直播间页面并捕获相关请求
 */
class BackgroundWebViewManager(private val context: Context) {
    private var webView: WebView? = null
    private var isWebViewInitialized = false
    
    companion object {
        private const val TAG = "BackgroundWebViewManager"
        private const val LIVE_PAGE_URL = "https://eos.douyin.com/livesite/live/current"
    }
    
    fun initializeWebView(scope: CoroutineScope) {
        if (isWebViewInitialized) return


        webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(1920, 720)
            // 配置WebView设置
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.databaseEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setSupportZoom(true)
            settings.setGeolocationEnabled(true)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            settings.allowContentAccess = true
            settings.allowFileAccess = true

            // 设置PC模式User-Agent
            val pcUserAgent = Utils.userAgent
            settings.userAgentString = pcUserAgent

            // 启用WebView调试（仅在调试模式下）
            // 移除BuildConfig引用，使用其他方式判断是否为调试模式
            try {
                val buildConfigClass = Class.forName(context.packageName + ".BuildConfig")
                val debugField = buildConfigClass.getDeclaredField("DEBUG")
                val isDebug = debugField.getBoolean(null)
                if (isDebug) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }
            } catch (e: Exception) {
                // 如果无法获取BuildConfig信息，则默认不启用调试
                Log.d(TAG, "无法获取BuildConfig信息，不启用WebView调试")
            }
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handlePageFinished(url)
                    injectXhrInterceptor()
                }
                
                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    super.onReceivedError(view, request, error)
                    // 处理页面加载错误
                    Log.e(TAG, "页面加载错误: ${error?.description}")
                }
            }
        }

        // 添加JavaScript接口
        val webAppInterface = WebViewJsInterface(context, scope)
        webView?.addJavascriptInterface(webAppInterface, "Android")

        isWebViewInitialized = true
    }


    public fun switchCard(id: Int) {
        val jsCode = "document.querySelector('#live-card-list > div > div > div > div:nth-child(3) > div > div.drag-cotent-ohRc5x > div > div.card-data-and-operate-Si6qcs > div.operate-area-VZ_EbJ > span:nth-child(${id}) > span > div').click()"

        webView?.evaluateJavascript(jsCode, null)
    }

    fun loadLivePage() {
        webView?.loadUrl(LIVE_PAGE_URL)
    }
    
    private fun injectXhrInterceptor() {
        val jsCode = """
            (function() {
                var originalOpen = XMLHttpRequest.prototype.open;
                var originalSend = XMLHttpRequest.prototype.send;
                
                XMLHttpRequest.prototype.open = function(method, url) {
                    this._url = url;
                    this._method = method;
                    return originalOpen.apply(this, arguments);
                };
                
                XMLHttpRequest.prototype.send = function(data) {
                    var self = this;
                    this.addEventListener('load', function() {
                        var response = self.response;
                        if (typeof response === 'object') {
                            response = JSON.stringify(response);
                        }
                        // 发送拦截到的XHR信息到Android
                        try {
                            Android.onXhrResponse(self._method, self._url, self.status, response);
                        } catch (e) {
                            console.error('调用Android接口失败:', e);
                        }
                    });
                    
                    // 拦截错误响应
                    this.addEventListener('error', function() {
                        try {
                            Android.onXhrResponse(self._method, self._url, -1, 'ERROR');
                        } catch (e) {
                            console.error('调用Android接口失败:', e);
                        }
                    });
                    
                    return originalSend.apply(this, arguments);
                };
            })();
        """.trimIndent()
        webView?.evaluateJavascript(jsCode, null)
    }
    
    private fun handlePageFinished(url: String?) {
        url?.let {
            Log.d(TAG, "页面加载完成: $it")
            if (it.contains("login")) {
                // 触发AILiveScreen提示
                triggerLoginPrompt()
            }
        }
    }
    
    private fun triggerLoginPrompt() {
        // 通过广播或其他机制通知AILiveScreen显示提示
        val intent = Intent("com.danshuang.AiLive.LIVE_LOGIN_PROMPT")
        intent.putExtra("message", "来客直播商品获取失败，请登录")
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }
    
    fun getWebView(): WebView? {
        return webView
    }
    
    fun destroy() {
        webView?.stopLoading()
        webView?.destroy()
        webView = null
        isWebViewInitialized = false
    }
}