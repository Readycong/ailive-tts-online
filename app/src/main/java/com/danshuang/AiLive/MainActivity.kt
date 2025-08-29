package com.danshuang.AiLive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.danshuang.AiLive.navigation.AppNavigation
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.service.LiveChattingService
import com.danshuang.AiLive.ui.theme.AiLiveTheme
import com.danshuang.AiLive.AiLiveApplication

class MainActivity : ComponentActivity() {
    private lateinit var app: AiLiveApplication
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 获取Application实例
        app = this.application as AiLiveApplication
        
        // 应用启动时自动启动直播间监控服务
        LiveRoomPullingService.startService(this)
        
        setContent {
            AiLiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // 检查是否是最后一个Activity销毁，如果是则认为应用退出
        if (isFinishing) {
            app.getAppLifecycleManager().onAppExit()
        }
    }
}