package com.danshuang.AiLive.service

import android.content.Context
import android.util.Log
import com.danshuang.AiLive.R
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 文件下载和缓存管理组件
 * 负责语音文件的下载和本地缓存管理
 */
class FileDownloader(private val context: Context) {
    
    companion object {
        private const val TAG = "FileDownloader"
        private const val CACHE_DIR = "voice_cache"
    }
    
    /**
     * 获取缓存目录
     */
    private fun getCacheDir(): File {
        val cacheDir = File(context.cacheDir, CACHE_DIR)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return cacheDir
    }
    
    /**
     * 检查文件是否已缓存
     */
    fun isFileCached(fileId: String): Boolean {
        val cacheFile = File(getCacheDir(), fileId)
        return cacheFile.exists()
    }
    
    /**
     * 获取缓存文件路径
     */
    fun getCacheFilePath(fileId: String): String? {
        val cacheFile = File(getCacheDir(), fileId)
        return if (cacheFile.exists()) cacheFile.absolutePath else null
    }
    
    /**
     * 下载并缓存文件
     */
    fun downloadAndCacheFile(voiceFile: com.danshuang.AiLive.data.model.VoiceFileInfo): String? {
        return try {
            // 如果文件标记为已缓存且本地存在，则直接返回路径
            if (voiceFile.cache && isFileCached(voiceFile.id)) {
                return getCacheFilePath(voiceFile.id)
            }
            
            // 下载文件
            val url = URL(voiceFile.url)
            val connection = url.openConnection()
            connection.connect()
            
            val input = connection.getInputStream()
            val cacheFile = File(getCacheDir(), voiceFile.id)
            val output = FileOutputStream(cacheFile)
            
            input.copyTo(output)
            input.close()
            output.close()
            
            Log.d(TAG, "文件下载并缓存成功: ${voiceFile.id}")
            cacheFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "文件下载失败: ${voiceFile.id}", e)
            null
        }
    }
    
    /**
     * 清理缓存文件
     */
    fun clearCache() {
        val cacheDir = getCacheDir()
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
    
    /**
     * 处理URL，如果URL不是以http://或https://开头，则添加file_base_url作为前缀
     */
    fun processUrl(url: String): String {
        // 检查URL是否以http://或https://开头
        if (url.startsWith("http://") || url.startsWith("https://")) {
            return url
        }
        
        // 获取file_base_url
        val baseUrl = context.getString(R.string.file_base_url)
        
        // 确保baseUrl以/结尾
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        
        // 确保url不以/开头
        val formattedUrl = if (url.startsWith("/")) url.substring(1) else url
        
        // 拼接完整的URL
        return "$formattedBaseUrl$formattedUrl"
    }
    
    /**
     * 获取缓存使用情况
     */
    
    fun getCacheSize(): Long {
        val cacheDir = getCacheDir()
        return if (cacheDir.exists()) {
            cacheDir.walkTopDown().sumOf { if (it.isFile) it.length() else 0L }
        } else {
            0L
        }
    }
}