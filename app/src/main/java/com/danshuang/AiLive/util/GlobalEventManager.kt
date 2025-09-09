package com.danshuang.AiLive.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.danshuang.AiLive.MainActivity

/**
 * 全局事件管理器
 * 用于在应用的不同组件之间发送和接收全局事件
 */
class GlobalEventManager private constructor() {
    
    companion object {
        private const val TAG = "GlobalEventManager"
        
        // 事件Action定义
        const val ACTION_UNAUTHORIZED_ERROR = "com.danshuang.AiLive.UNAUTHORIZED_ERROR"
        
        @Volatile
        private var INSTANCE: GlobalEventManager? = null
        
        fun getInstance(): GlobalEventManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GlobalEventManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 发送未授权错误事件
     * 当API返回401错误时调用此方法通知UI跳转到登录页
     */
    fun sendUnauthorizedErrorEvent(context: Context) {
        try {
            Log.d(TAG, "Sending unauthorized error event")
            val intent = Intent(ACTION_UNAUTHORIZED_ERROR)
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending unauthorized error event", e)
        }
    }
    
    /**
     * 跳转到登录页
     * 在UI层接收到未授权错误事件后调用此方法
     */
    fun navigateToLoginPage(context: Context) {
        try {
            Log.d(TAG, "Navigating to login page")
            val intent = Intent(context, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to login page", e)
        }
    }
}