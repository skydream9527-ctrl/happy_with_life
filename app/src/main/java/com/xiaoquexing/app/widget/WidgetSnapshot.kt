package com.xiaoquexing.app.widget

data class WidgetSnapshot(
    val title: String,
    val body: String,
    val footer: String,
)

object WidgetCopy {
    fun of(todayCount: Int, mood: String?, text: String?, totalGp: Int): WidgetSnapshot {
        val title = if (todayCount == 0) "今天还没记" else "今天 $todayCount 条"
        val snippet = text?.trim().orEmpty().replace('\n', ' ')
        val body = when {
            snippet.isNotBlank() -> listOfNotNull(mood, snippet.take(18)).joinToString(" · ")
            !mood.isNullOrBlank() -> mood
            else -> "点一下，写下小确幸"
        }
        return WidgetSnapshot(title = title, body = body, footer = "$totalGp GP")
    }
}
