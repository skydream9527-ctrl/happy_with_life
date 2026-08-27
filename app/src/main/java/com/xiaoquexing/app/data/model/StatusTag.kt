package com.xiaoquexing.app.data.model

data class StatusTag(
    val name: String,
    val emoji: String
) {
    companion object {
        val defaults = listOf(
            StatusTag("旅游", "✈️"),
            StatusTag("工作", "💼"),
            StatusTag("美食", "🍜"),
            StatusTag("运动", "🏃"),
            StatusTag("阅读", "📖"),
            StatusTag("音乐", "🎵"),
            StatusTag("电影", "🎬"),
            StatusTag("约会", "💕"),
            StatusTag("独处", "🧘"),
            StatusTag("聚会", "👥"),
            StatusTag("自然", "🌿"),
            StatusTag("居家", "🏠")
        )
    }
}
