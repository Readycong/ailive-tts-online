package com.danshuang.AiLive.data

data class LiveScriptItem(
    val id: Int,
    val category: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)