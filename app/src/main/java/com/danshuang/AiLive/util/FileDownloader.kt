package com.danshuang.AiLive.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 文件下载工具类
 * 负责从网络下载文件并保存到本地
 */
class FileDownloader {
    
    companion object {
        private const val TAG = "FileDownloader"
        private const val BUFFER_SIZE = 4096
    }
    
    /**
     * 下载文件并保存到指定路径
     * @param fileUrl 文件URL
     * @param destinationPath 目标文件路径
     * @return 下载是否成功
     */
    suspend fun downloadFile(fileUrl: String, destinationPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting download: $fileUrl to $destinationPath")
                
                // 创建目标文件
                val destinationFile = File(destinationPath)
                destinationFile.parentFile?.let { parentDir ->
                    if (!parentDir.exists()) {
                        parentDir.mkdirs()
                    }
                }
                
                // 创建文件输出流
                val outputStream = FileOutputStream(destinationFile)
                
                // 建立网络连接
                val url = URL(fileUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                
                // 检查响应码
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Failed to download file. Response code: ${connection.responseCode}")
                    outputStream.close()
                    return@withContext false
                }
                
                // 获取输入流
                val inputStream = connection.inputStream
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                
                // 读取并写入文件
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
                
                // 关闭流
                inputStream.close()
                outputStream.close()
                connection.disconnect()
                
                Log.d(TAG, "Successfully downloaded file: $fileUrl")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading file: $fileUrl", e)
                false
            }
        }
    }
}