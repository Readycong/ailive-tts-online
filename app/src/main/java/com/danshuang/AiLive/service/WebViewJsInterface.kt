package com.danshuang.AiLive.service

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * WebView JavaScript接口
 * 用于处理WebView中拦截到的XHR请求
 */
class WebViewJsInterface(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "WebViewJsInterface"
        private const val PRODUCT_LIST_URL = "/data/life/live/shelves/anchor/"
    }

    @JavascriptInterface
    fun onXhrResponse(method: String?, url: String?, status: Int, response: String?) {
        scope.launch {
            try {
                Log.d("WebViewJsInterface", "XHR Response - Method: $method, URL: $url, Status: $status, Response: $response")
                // 检查是否为商品列表请求
                if (url?.contains(PRODUCT_LIST_URL) == true && status == 200) {
                    // 解析商品列表数据
                    parseProductList(response)
                }
            } catch (e: Exception) {
                Log.e(TAG, "处理XHR响应时出错", e)
            }
        }
    }

    private fun parseProductList(jsonResponse: String?) {
        jsonResponse?.let {
            try {
                val jsonObject = JSONObject(it)
                val cardList = jsonObject.getJSONArray("card_list")

                // 解析商品列表并存储
                val productList = mutableListOf<ProductInfo>()
                for (i in 0 until cardList.length()) {
                    val card = cardList.getJSONObject(i)
                    val productBaseInfo = card.getJSONObject("product_base_info")

                    // 获取图片URL
                    var imageUrl: String? = null
                    if (productBaseInfo.has("image_info") && !productBaseInfo.isNull("image_info")) {
                        val imageInfoArray = productBaseInfo.getJSONArray("image_info")
                        if (imageInfoArray.length() > 0) {
                            val imageInfo = imageInfoArray.getJSONObject(0)
                            imageUrl = imageInfo.optString("web_url", "")
                            // 如果获取到的URL为空字符串，则设为null
                            if (imageUrl?.isEmpty() == true) {
                                imageUrl = null
                            }
                        }
                    }

                    // 获取折扣信息
                    var discount: String? = null
                    if (card.has("price_info") && !card.isNull("price_info")) {
                        val priceInfo = card.getJSONObject("price_info")
                        discount = priceInfo.optString("discount", "")
                        // 如果获取到的折扣信息为空字符串，则设为null
                        if (discount?.isEmpty() == true) {
                            discount = null
                        }
                    }

                    // 获取已售数量
                    var soldCount: String? = null
                    if (card.has("stock_info") && !card.isNull("stock_info")) {
                        val stockInfo = card.getJSONObject("stock_info")
                        if (stockInfo.has("sold") && !stockInfo.isNull("sold")) {
                            val soldArray = stockInfo.getJSONArray("sold")
                            if (soldArray.length() > 1) {
                                soldCount = soldArray.optString(1, "")
                            }
                        }
                    }

                    // 获取库存信息
                    var stock: String? = null
                    if (card.has("stock_info") && !card.isNull("stock_info")) {
                        val stockInfo = card.getJSONObject("stock_info")
                        if (stockInfo.has("stock") && !stockInfo.isNull("stock")) {
                            val stockArray = stockInfo.getJSONArray("stock")
                            if (stockArray.length() > 1) {
                                stock = stockArray.optString(1, "")
                            }
                        }
                    }

                    // 获取标签信息
                    val labels = mutableListOf<String>()
                    if (card.has("label_info") && !card.isNull("label_info")) {
                        val labelInfo = card.getJSONObject("label_info")
                        if (labelInfo.has("right_list") && !labelInfo.isNull("right_list")) {
                            val rightList = labelInfo.getJSONArray("right_list")
                            for (j in 0 until rightList.length()) {
                                labels.add(rightList.getString(j))
                            }
                        }
                    }

                    val product = ProductInfo(
                        productId = productBaseInfo.getString("product_id"),
                        name = productBaseInfo.getString("name"),
                        price = card.getJSONObject("price_info").getLong("actual_price"),
                        originalPrice = if (card.getJSONObject("price_info").has("origin_price")) 
                            card.getJSONObject("price_info").getLong("origin_price") else null,
                        imageUrl = imageUrl,
                        discount = discount,
                        soldCount = soldCount,
                        stock = stock,
                        labels = labels
                    )

                    productList.add(product)
                }

                // 存储商品列表供后续使用
                storeProductList(productList)
                Log.d("WebViewJsInterface", "商品列表已更新，包含 ${productList.size} 个商品")
                Log.d("WebViewJsInterface", productList.toString())

                // 发送广播通知商品列表已更新
                val intent = Intent("com.danshuang.AiLive.PRODUCT_LIST_UPDATED")
                intent.setPackage(context.packageName)
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                Log.e(TAG, "解析商品列表时出错", e)
            }
        }
    }

    private fun storeProductList(products: List<ProductInfo>) {
        // 实现商品列表存储逻辑
        ProductListManager.updateProducts(products)
    }
}