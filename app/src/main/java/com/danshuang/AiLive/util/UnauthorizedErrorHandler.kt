package com.danshuang.AiLive.util

import android.content.Context
import android.util.Log
import com.danshuang.AiLive.service.LiveRoomPullingService
import com.danshuang.AiLive.service.LiveChattingService
import com.danshuang.AiLive.data.LoginRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 未授权错误处理器
 * 处理401错误，停止所有服务，清除登录状态
 */
class UnauthorizedErrorHandler private constructor() {
    
    companion object {
        private const val TAG = "UnauthorizedErrorHandler"
        @Volatile
        private var INSTANCE: UnauthorizedErrorHandler? = null
        
        fun getInstance(): UnauthorizedErrorHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UnauthorizedErrorHandler().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 处理未授权错误
     */
    fun handleUnauthorizedError(context: Context) {
        try {
            Log.d(TAG, "Handling unauthorized error")
            
            // 1. 停止所有服务
            stopAllServices(context)
            
            // 2. 清除登录状态
            clearLoginState(context)
            
            // 3. 发送全局事件通知UI跳转到登录页
            GlobalEventManager.getInstance().sendUnauthorizedErrorEvent(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling unauthorized error", e)
        }
    }
    
    /**
     * 停止所有服务
     */
    private fun stopAllServices(context: Context) {
        try {
            Log.d(TAG, "Stopping all services")
            // 停止直播间监控服务
            LiveRoomPullingService.stopService(context)
            
            // 停止AI直播语音播放服务
            LiveChattingService.stopService(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping services", e)
        }
    }
    
    /**
     * 清除登录状态
     */
    private fun clearLoginState(context: Context) {
        try {
            Log.d(TAG, "Clearing login state")
            val loginRepository = LoginRepository(context)
            // 在IO线程中清除登录信息
            CoroutineScope(Dispatchers.IO).launch {
                loginRepository.clearLoginInfo()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing login state", e)
        }
    }
}