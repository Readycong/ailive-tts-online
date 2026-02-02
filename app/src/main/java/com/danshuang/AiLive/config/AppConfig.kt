package com.danshuang.AiLive.config

import android.content.Context
import com.danshuang.AiLive.R

/**
 * 应用配置管理类
 * 统一管理从 strings.xml 读取的配置信息
 * 
 * 根据项目规范要求：
 * - 实现统一的配置管理
 * - 支持不同环境的配置切换
 * - 提供类型安全的配置访问
 */
class AppConfig private constructor(private val context: Context) {
    
    /**
     * API 相关配置
     */
    object Api {
        /**
         * 获取当前使用的 API 基础 URL
         */
        fun getBaseUrl(context: Context): String {
            return context.getString(R.string.api_base_url)
        }
        
        /**
         * 获取开发环境 API URL
         */
        fun getDevUrl(context: Context): String {
            return context.getString(R.string.api_base_url1)
        }
        
        /**
         * 获取测试环境 API URL  
         */
        fun getTestUrl(context: Context): String {
            return context.getString(R.string.api_base_url2)
        }
        
        /**
         * 获取图片基础 URL
         */
        fun getImageBaseUrl(context: Context): String {
            return context.getString(R.string.image_base_url)
        }

        fun getSiteId(context: Context): String {
            return context.getString(R.string.site_id)
        }
        
        /**
         * 获取API超时时间（秒）
         */
        fun getTimeout(context: Context): Long {
            return try {
                context.getString(R.string.api_timeout).toLong()
            } catch (e: NumberFormatException) {
                20L // 默认20秒
            }
        }
    }
    
    /**
     * 抖音相关配置
     */
    object Douyin {
        /**
         * 获取抖音搜索模板 URL
         */
        fun getSearchPattern(context: Context): String {
            return context.getString(R.string.douyin_search_pattern)
        }
        
        /**
         * 获取 JS 搜索 URL
         */
        fun getJsSearchUrl(context: Context): String {
            return context.getString(R.string.js_search_url)
        }
        
        /**
         * 获取 JS 用户 URL
         */
        fun getJsUserUrl(context: Context): String {
            return context.getString(R.string.js_user_url)
        }
    }
    
    /**
     * 应用基础配置
     */
    object App {
        /**
         * 获取应用名称
         */
        fun getName(context: Context): String {
            return context.getString(R.string.app_name)
        }
        
        /**
         * 获取应用 Logo URL
         */
        fun getLogoUrl(context: Context): String {
            return context.getString(R.string.app_logo)
        }
    }
    
    /**
     * 环境管理
     */
    enum class Environment {
        DEVELOPMENT,    // 开发环境
        TESTING,        // 测试环境  
        PRODUCTION      // 生产环境
    }
    
    /**
     * 获取当前环境对应的 API URL
     */
    fun getApiUrlForEnvironment(environment: Environment): String {
        return when (environment) {
            Environment.DEVELOPMENT -> Api.getDevUrl(context)
            Environment.TESTING -> Api.getTestUrl(context)
            Environment.PRODUCTION -> Api.getBaseUrl(context)
        }
    }
    
    /**
     * 检查是否为开发模式
     */
    fun isDevelopmentMode(): Boolean {
        // 暂时使用硬编码的debug判断，BuildConfig在首次编译时可能不可用
        return false // TODO: 在成功编译后改为 BuildConfig.DEBUG
    }
    
    /**
     * 获取当前环境
     */
    fun getCurrentEnvironment(): Environment {
        return if (isDevelopmentMode()) {
            Environment.DEVELOPMENT
        } else {
            Environment.PRODUCTION
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: AppConfig? = null
        
        /**
         * 获取配置管理器实例
         */
        fun getInstance(context: Context): AppConfig {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppConfig(context.applicationContext).also { INSTANCE = it }
            }
        }
        
        /**
         * 快速获取 API 基础 URL 的便捷方法
         */
        fun getApiBaseUrl(context: Context): String {
            return Api.getBaseUrl(context)
        }
        
        /**
         * 根据环境获取 API URL
         */
        fun getApiUrlByEnvironment(context: Context, environment: Environment): String {
            return getInstance(context).getApiUrlForEnvironment(environment)
        }
    }
}

/**
 * 扩展函数：为 Context 添加便捷的配置访问方法
 */
fun Context.getApiBaseUrl(): String = AppConfig.getApiBaseUrl(this)
fun Context.getImageBaseUrl(): String = AppConfig.Api.getImageBaseUrl(this)
fun Context.getAppConfig(): AppConfig = AppConfig.getInstance(this)

/**
 * 使用示例：
 * 
 * 1. 在 ViewModel 中使用：
 * ```kotlin
 * class LoginViewModel(application: Application) : AndroidViewModel(application) {
 *     private val apiUtils = ServerApiUtilsV2.createFromContext(
 *         application, tokenManager, loginRepository
 *     )
 * }
 * ```
 * 
 * 2. 在其他地方使用：
 * ```kotlin
 * val apiUrl = context.getApiBaseUrl()
 * val config = context.getAppConfig()
 * val prodUrl = config.getApiUrlForEnvironment(AppConfig.Environment.PRODUCTION)
 * ```
 * 
 * 3. 环境切换：
 * ```kotlin
 * val config = AppConfig.getInstance(context)
 * val currentEnv = config.getCurrentEnvironment()
 * val apiUrl = config.getApiUrlForEnvironment(currentEnv)
 * ```
 */