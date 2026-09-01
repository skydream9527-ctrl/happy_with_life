package com.xiaoquexing.app.util

object PublishGuard {
    fun hasContent(
        text: String,
        photoCount: Int,
        hasVoice: Boolean,
        hasMusic: Boolean,
        hasLink: Boolean,
        hasLocation: Boolean,
    ): Boolean = text.isNotBlank() || photoCount > 0 || hasVoice || hasMusic || hasLink || hasLocation

    fun missingHint(mood: String?, hasContent: Boolean): String? = when {
        mood.isNullOrBlank() -> "请先选择一个心情～"
        !hasContent -> "请至少记录一些内容～"
        else -> null
    }
}
