package com.danshuang.AiLive.util

import android.annotation.SuppressLint
import android.content.Context
import org.json.JSONObject
import com.squareup.duktape.Duktape
import java.security.MessageDigest
import kotlin.collections.joinToString
import kotlin.io.bufferedReader
import kotlin.io.readText
import kotlin.io.use
import kotlin.text.format
import kotlin.text.isNotEmpty
import kotlin.text.replace
import kotlin.text.split
import kotlin.text.startsWith
import kotlin.text.substringAfter
import kotlin.text.substringBefore
import kotlin.text.toByteArray
import kotlin.text.toRegex
import kotlin.toString

open class DouyinHackUtil(open var docHtml: String) {
    // Cookies
    //ttwid=1%7CbEAUDOhVg5wznpGI_w-KkO94fU4MW1mh61DqmDdrHmw%7C1748422932%7C9f93796060ea84a970c4d9fa8559a372ba7559a60e1b717a670fae0971ea6b91
    //x-web-secsdk-uid=13884994-6ffd-4910-9820-bee868ba10cc
    //xgplayer_device_id=38512839550
    //xgplayer_user_id=569733357804
    //has_avx2=null
    //device_web_cpu_core=4
    //device_web_memory_size=2
    //webcast_local_quality=null
    //live_can_add_dy_2_desktop=%220%22
    //live_use_vvc=%22false%22
    //csrf_session_id=597bbff84ce6af4d8dfacd5279b4e938
    //hevc_supported=true
    //fpk1=U2FsdGVkX19ciuyGqW9MWgrfQAmVatiColW8vRDvihpTYIj2hoGysZV7rIduNrlESdUVxXxWaIfyzPxOGXuEcQ==
    //fpk2=dd8345dd8c6efc3a0250a3f030abaa9a
    //FORCE_LOGIN=%7B%22videoConsumedRemainSeconds%22%3A180%7D
    //__live_version__=%221.1.3.2991%22
    //xg_device_score=6.905294117647059
    //IsDouyinActive=true
    //__ac_nonce=06837dd9100a6df01b448
    //__ac_signature=_02B4Z6wo00f01T4h2NgAAIDD11nvMmUSsKk-EdxAACfC8d

    var html: String = docHtml
        set(value) {
            field = value
        }

    fun getRoomId(): String? {
        val roomIDRegex  = """roomId":"(\d+)"""".toRegex()
        val matchResult = roomIDRegex.find(html)
        if (matchResult != null) {
            val roomId = matchResult.groupValues[1]
            return roomId
        } else {
            return null
        }
    }

    fun getPushID(): String? {
        val pushIDRegex  = """user_unique_id":"(\d+)"""".toRegex()
        val matchResult = pushIDRegex.find(html)
        if (matchResult != null) {
            val roomId = matchResult.groupValues[1]
            return roomId
        } else {
            return null
        }
    }

    fun getLiveName(): String? {
        val liveNameRegex  = """data-anchor-info=\\"([\s\S]*?)\\" data-room-info=\\"""".toRegex()
        val matchResult = liveNameRegex.find(html)
        if (matchResult != null) {
            val liveNameJson = matchResult.groupValues[1].replace("&quot;", "\"")
            val jsonInfo = JSONObject(liveNameJson)

            val liveName = jsonInfo.optString("nickname", "")
            if (liveName.isNotEmpty()) {
                return liveName
            }
        }
        return null
    }

    fun fetchTTWID(Cookies: String): String? {
        for (cookie in Cookies.split(';')) {
            if (cookie.startsWith("ttwid=")) {
                return cookie.substringAfter("ttwid=").substringBefore(";")
            }
        }
        return null
    }

    fun jsSignature(md5String: String): String? {
        try {
            val jsEngine: Duktape = Duktape.create()
            val script = readScriptFromAssets("assets/webmssdk.js")
            jsEngine.evaluate(script)
            var sign = jsEngine.evaluate("get_sign('$md5String')") as CharSequence?
            jsEngine.close()
            return sign.toString()

        }catch (e: Exception) {
            return null
        }
    }

    fun signature(roomId: String, pushId: String): String? {
        var smap = listOf(
            "live_id=1",
            "aid=6383",
            "version_code=180800",
            "webcast_sdk_version=1.0.14-beta.0",
            "room_id=" + roomId,
            "sub_room_id=",
            "sub_channel_id=",
            "did_rule=3",
            "user_unique_id=" + pushId,
            "device_platform=web",
            "device_type=",
            "ac=",
            "identity=audience",
        ).joinToString(",")
        val md5string = md5(smap)

        return jsSignature(md5string)
    }

    fun readScriptFromAssets(filename: String): String {
        return this::class.java.classLoader?.getResourceAsStream(filename)?.bufferedReader().use { it?.readText() } ?: ""
    }

    @SuppressLint("DefaultLocale")
    fun getWssURL(userAgent: String, roomId: String, pushId: String): String {
        val wssURLTemplate          = "wss://webcast5-ws-web-lf.douyin.com/webcast/im/push/v2/" +
                "?app_name=douyin_web&version_code=180800&webcast_sdk_version=1.0.14-beta.0" +
                "&update_version_code=1.0.14-beta.0&compress=gzip&device_platform=web" +
                "&cookie_enabled=true&screen_width=1920&screen_height=1080&browser_language=zh-CN" +
                "&browser_platform=Win32&browser_name=Mozilla&browser_version=%s&browser_online=true" +
                "&tz_name=Asia/Shanghai&cursor=d-1_u-1_fh-7383731312643626035_t-1719159695790_r-1" +
                "&internal_ext=internal_src:dim|wss_push_room_id:%s|wss_push_did:%s|first_req_ms:%d" +
                "|fetch_time:%d|seq:1|wss_info:0-%d-0-0|wrds_v:7382620942951772256&host=https://live.douyin.com" +
                "&aid=6383&live_id=1&did_rule=3&endpoint=live_pc&support_wrds=1&user_unique_id=%s" +
                "&im_path=/webcast/im/fetch/&identity=audience&need_persist_msg_count=15" +
                "&insert_task_id=&live_reason=&room_id=%s&heartbeatDuration=0&signature=%s"
        val currentTime = System.currentTimeMillis()

        return String.format(
            wssURLTemplate,
            "5.0%20(Windows%20NT%2010.0;%20Win64;%20x64)%20AppleWebKit/537.36%20(KHTML,%20like%20Gecko)%20Chrome/136.0.0.0%20Safari/537.36",
            roomId,
            pushId,
            currentTime,
            currentTime,
            currentTime,
            pushId,
            roomId,
            signature(roomId, pushId) ?: ""
        )
    }

    fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}