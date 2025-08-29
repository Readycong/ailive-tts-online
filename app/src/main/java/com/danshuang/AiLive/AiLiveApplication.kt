package com.danshuang.AiLive

import android.app.Application
import com.danshuang.AiLive.util.AppLifecycleManager

/**
 * 自定义Application类
 * 用于初始化全局组件和监听应用生命周期
 */
class AiLiveApplication : Application() {
    
    private lateinit var appLifecycleManager: AppLifecycleManager
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化应用生命周期管理器
        appLifecycleManager = AppLifecycleManager(this)
        registerActivityLifecycleCallbacks(appLifecycleManager)
    }
    
    /**
     * 获取应用生命周期管理器实例
     */
    fun getAppLifecycleManager(): AppLifecycleManager {
        return appLifecycleManager
    }
}