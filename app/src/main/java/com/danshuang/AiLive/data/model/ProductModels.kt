package com.danshuang.AiLive.data.model

import androidx.annotation.Keep

/**
 * 商品信息
 */
@Keep
data class ProductItem(
    val id: Int,
    val productName: String,
    val price: String,
    val coverImage: String?,
    val description: String,
    val isOnline: Int,           // 1=上架, 0=下架
    val enableAiVoice: Int,      // 1=讲解, 0=不讲解
    val weight: Int,
    val stock: Int,              // 库存数量
    val aiDescription: String?,
    val aiKbsId: Int,
    val sort: Int,
    val siteId: Int,
    val createTime: String,
    val updateTime: String
) {
    /** 格式化显示价格 */
    val displayPrice: String get() = "¥$price"
    
    /** 上架状态文字 */
    val statusText: String get() = if (isOnline == 1) "上架中" else "已下架"
    
    /** AI讲解状态文字 */
    val aiVoiceText: String get() = if (enableAiVoice == 1) "AI讲解" else "不讲解"
    
    /** 格式化编号显示 */
    val sortDisplayText: String get() = "#${sort.toString().padStart(2, '0')}"
    
    /** 库存显示文字 */
    val stockText: String get() = "库存: $stock"
    
    /** 获取完整的图片URL，自动补全前缀 */
    fun getFullCoverImageUrl(wapBaseUrl: String): String? {
        val img = coverImage ?: return null
        if (img.isBlank()) return null
        return if (img.startsWith("http://") || img.startsWith("https://")) {
            img
        } else {
            val base = wapBaseUrl.trimEnd('/')
            val path = img.trimStart('/')
            "$base/$path"
        }
    }
}

/**
 * 商品列表分页响应
 */
@Keep
data class ProductListData(
    val total: Int,
    val data: List<ProductItem>
)

/**
 * AI讲解文案生成结果
 */
@Keep
data class AiDescriptionResult(
    val aiDescription: String
)

/**
 * 新增商品响应
 */
@Keep
data class ProductAddResult(
    val id: Int
)
