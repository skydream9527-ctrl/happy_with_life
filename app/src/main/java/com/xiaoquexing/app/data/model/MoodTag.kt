package com.xiaoquexing.app.data.model

data class MoodTag(
    val name: String,
    val emoji: String,
    val color: Long
) {
    companion object {
        val defaults = listOf(
            MoodTag("开心", "😊", 0xFFF5C842),
            MoodTag("平静", "😌", 0xFF7EC8CE),
            MoodTag("兴奋", "🤩", 0xFFFF7043),
            MoodTag("感动", "🥹", 0xFFF4A261),
            MoodTag("想念", "💭", 0xFFB0ADA6),
            MoodTag("疲惫", "😮‍💨", 0xFF8A8780),
            MoodTag("难过", "😔", 0xFF87A6C4),
            MoodTag("愤怒", "😤", 0xFFFF6B5E),
            MoodTag("惊喜", "🎉", 0xFFE8865E)
        )

        fun fromName(name: String?): MoodTag? {
            if (name == null) return null
            return defaults.firstOrNull { it.name == name }
        }
    }
}
