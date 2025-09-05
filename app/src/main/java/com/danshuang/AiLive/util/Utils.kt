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

        @JvmStatic
        suspend fun getRemoteContent(url: String): String? {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response: Response =
                withContext(Dispatchers.IO) { client.newCall(request).execute() }

            if (!response.isSuccessful) return null
            return response.body?.string()
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