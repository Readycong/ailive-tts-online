package com.danshuang.AiLive.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.service.LiveChattingService

/**
 * 应用生命周期管理器
 * 负责监听应用的前后台切换和退出事件
 */
class AppLifecycleManager(private val context: Context) : Application.ActivityLifecycleCallbacks {
    
    companion object {
        private const val TAG = "AppLifecycleManager"
        private var foregroundActivities = 0
        private var isForeground = false
    }
    
    override fun onActivityStarted(activity: Activity) {
        foregroundActivities++
        if (!isForeground) {
            isForeground = true
            onAppForeground()
        }
    }
    
    override fun onActivityStopped(activity: Activity) {
        foregroundActivities--
        if (isForeground && foregroundActivities <= 0) {
            isForeground = false
            onAppBackground()
        }
    }
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
    
    /**
     * 应用进入前台时的回调
     */
    private fun onAppForeground() {
        Log.d(TAG, "App entered foreground")
        // 应用进入前台时，保持服务运行状态
        // 不需要特殊处理，服务会根据之前的状态继续运行
    }
    
    /**
     * 应用进入后台时的回调
     */
    private fun onAppBackground() {
        Log.d(TAG, "App entered background")
        // 应用进入后台时，保持服务运行状态
        // 不需要特殊处理，服务会在后台继续运行
    }
    
    /**
     * 应用退出时的回调
     * 注意：这个方法需要在适当的地方手动调用
     */
    fun onAppExit() {
        Log.d(TAG, "App exiting, stopping services")
        // 停止所有服务
        try {
            // 停止直播间监控服务
            LiveRoomPullingService.stopService(context)
                
            // 停止AI直播语音播放服务
            LiveChattingService.stopService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping services", e)
        }
    }
}