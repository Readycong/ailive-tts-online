package com.danshuang.AiLive.example

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.danshuang.AiLive.navigation.NavigationState

/**
 * WebViewScreen使用示例
 * 
 * WebViewScreen是一个通用的WebView页面组件，用于显示业务页面。
 * 特点：
 * 1. 在加载URL前，会自动在localStorage中设置wapToken参数
 * 2. wapToken的值来自当前登录用户的authToken
 * 3. 支持自定义页面标题
 * 4. 支持返回导航
 * 
 * 使用方法：
 * 在任何需要打开WebView页面的地方，调用navigationState.navigateToWebView(url, context, title)
 */
@Composable
fun WebViewScreenUsageExample(navigationState: NavigationState) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "WebViewScreen使用示例",
            style = MaterialTheme.typography.headlineMedium
        )
        
        // 示例1：打开业务页面，使用默认标题
        Button(
            onClick = {
                navigationState.navigateToWebView(
                    url = "https://example.com/business-page",
                    context = context
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("打开业务页面（默认标题）")
        }
        
        // 示例2：打开业务页面，使用自定义标题
        Button(
            onClick = {
                navigationState.navigateToWebView(
                    url = "https://example.com/order-detail",
                    context = context,
                    title = "订单详情"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("打开订单详情页")
        }
        
        // 示例3：打开用户信息页面
        Button(
            onClick = {
                navigationState.navigateToWebView(
                    url = "https://example.com/user-profile",
                    context = context,
                    title = "个人信息"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("打开个人信息页")
        }
        
        // 示例4：打开商品详情页
        Button(
            onClick = {
                val productId = "12345"
                navigationState.navigateToWebView(
                    url = "https://example.com/product/$productId",
                    context = context,
                    title = "商品详情"
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("打开商品详情页")
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "技术说明",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• WebView会在加载URL前自动设置localStorage中的wapToken",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• wapToken的值为当前用户的authToken",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• 页面加载完成后，会再次设置wapToken以确保设置成功",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• 支持JavaScript、Cookie、DOM Storage等特性",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• 页面加载时会显示加载指示器",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * 在实际业务中的使用示例
 */
@Composable
fun BusinessUsageExample(navigationState: NavigationState) {
    val context = LocalContext.current
    
    // 示例：从商品列表点击进入商品详情页
    fun openProductDetail(productId: String) {
        navigationState.navigateToWebView(
            url = "https://api.example.com/product/detail?id=$productId",
            context = context,
            title = "商品详情"
        )
    }
    
    // 示例：从订单列表点击进入订单详情页
    fun openOrderDetail(orderId: String) {
        navigationState.navigateToWebView(
            url = "https://api.example.com/order/detail?id=$orderId",
            context = context,
            title = "订单详情"
        )
    }
    
    // 示例：打开活动页面
    fun openActivityPage(activityId: String) {
        navigationState.navigateToWebView(
            url = "https://api.example.com/activity?id=$activityId",
            context = context,
            title = "活动详情"
        )
    }
    
    // 示例：打开帮助中心
    fun openHelpCenter() {
        navigationState.navigateToWebView(
            url = "https://help.example.com",
            context = context,
            title = "帮助中心"
        )
    }
}
