package com.danshuang.AiLive.service

/**
 * 商品信息数据类
 */
data class ProductInfo(
    val productId: String,
    val name: String,
    val price: Long, // 实际价格，单位为分
    val originalPrice: Long? = null, // 原价，单位为分
    val imageUrl: String? = null,
    val discount: String? = null, // 折扣信息，如"9.7折"
    val soldCount: String? = null, // 已售数量
    val stock: String? = null, // 库存信息
    val labels: List<String> = emptyList() // 商品标签，如["周一至周日可用", "随时退"]
)