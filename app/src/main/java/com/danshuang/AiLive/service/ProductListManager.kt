package com.danshuang.AiLive.service

import androidx.compose.runtime.mutableStateListOf

/**
 * 商品列表管理类
 */
object ProductListManager {
    private val _products = mutableStateListOf<ProductInfo>()
    val products: List<ProductInfo> get() = _products

    fun updateProducts(newProducts: List<ProductInfo>) {
        _products.clear()
        _products.addAll(newProducts)
    }

    fun clearProducts() {
        _products.clear()
    }

    fun getProductById(productId: String): ProductInfo? {
        return _products.find { it.productId == productId }
    }

    val productCount: Int get() = _products.size
}