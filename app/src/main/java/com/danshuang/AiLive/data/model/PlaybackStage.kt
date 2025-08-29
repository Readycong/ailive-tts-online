package com.danshuang.AiLive.data.model

/**
 * 播放阶段枚举
 */
enum class PlaybackStage {
    WELCOME,    // 欢迎阶段
    ROOM,       // 直播间介绍阶段
    PRODUCT,    // 产品介绍阶段
    KEEP,       // 留人话术阶段
    ORDER,      // 促单阶段
    THANKS,     // 感谢阶段
    COMMENT     // 评论互动阶段
}