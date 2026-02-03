package com.danshuang.AiLive.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.text.substringAfter

class Utils {
    companion object {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
        val script = """
(function() {
    var infoObj = document.querySelector("#user_detail_element")
    var liveStatus = infoObj.querySelector("div > div:nth-child(2) > div").innerText.trim() == '直播中'
    var liveUrl = document.querySelector("#user_detail_element > div > div > div:nth-child(1) > div > a")
    return {
            js: "user_detail_element", 
            "load": infoObj ? "FOUND" : "ERROR", 
            status: liveStatus?'PLAYING':'NO', 
            url: liveUrl ? liveUrl.href : ''
            }
})()
""".trimIndent()

        val script_binding = """
                                        (function() {
    try {
        console.log('开始提取抖音账户信息...');
        var infoObj = document.querySelector("#user_detail_element")
        if (!infoObj) {
            console.log('未找到 #user_detail_element 元素');
            return {
                js: "user_detail_element", 
                load: "ERROR", 
                status: 'NO', 
                id: '',
                nickname: '',
                avatar: ''
            }
        }
        console.log('找到用户信息元素，开始解析...');
        var liveStatusElement = infoObj.querySelector("div > div:nth-child(2) > div");
        var liveStatus = liveStatusElement ? liveStatusElement.innerText == '直播中' : false;
        var nicknameElement = infoObj.querySelector("div:nth-child(2) > div:nth-child(2) > div");
        var userIdElement = infoObj.querySelector("div:nth-child(2) > div:nth-child(2) > p > span");
        var avatarElement = document.querySelector("#search-result-container > div:nth-child(3) > ul > li:nth-child(1) > div > a");
        
        var result = {
            js: "user_detail_element", 
            load: "FOUND", 
            status: liveStatus ? 'PLAYING' : 'NO', 
            id: userIdElement ? userIdElement.innerText.trim().replace('抖音号：', '') : '',
            nickname: nicknameElement ? nicknameElement.innerText : '未知用户',
            avatar: avatarElement ? avatarElement.src : '',
            html: document.body.outerHTML
        };
        console.log('账户信息提取完成:', result);
        return result;
    } catch (e) {
        console.error('提取账户信息时发生错误:', e);
        return {
            js: "user_detail_element", 
            load: "ERROR", 
            status: 'NO', 
            id: '',
            nickname: '',
            avatar: ''
        }
    }
})()
""".trimIndent()

        @JvmStatic
        suspend fun getRemoteContent(url: String): String? {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response: Response =
                withContext(Dispatchers.IO) { client.newCall(request).execute() }

            if (!response.isSuccessful) return null
            return try {
                response.body?.string()
            } catch (e: IllegalStateException) {
                e.printStackTrace()
                null
            }
        }

        fun loadBase64Image(imageView: ImageView, base64String: String) {
            // 移除开头的 "data:image/jpeg;base64," 前缀
            val base64Image = base64String.substringAfter("base64,")

            try {
                // 解码 Base64 为字节数组
                val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)

                // 转换为 Bitmap
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)

                // 设置到 ImageView
                imageView.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
                // 可以显示默认图片或提示加载失败
            }
        }

        fun showToast(context: Context, message: String) {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }
}