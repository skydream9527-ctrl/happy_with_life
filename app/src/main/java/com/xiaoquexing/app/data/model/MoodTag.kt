package com.xiaoquexing.app.data.model

data class MoodTag(
    val name: String,
    val emoji: String,
    val color: Long
) {
    companion object {
        val defaults = listOf(
            MoodTag("开心", "😊", 0xFFFFEB3B),
            MoodTag("平静", "😐", 0xFF90A4AE),
            MoodTag("兴奋", "🤩", 0xFFFF9800),
            MoodTag("感动", "🥹", 0xFFE91E63),
            MoodTag("想念", "💭", 0xFF9C27B0),
            MoodTag("疲惫", "😮‍💨", 0xFF78909C),
            MoodTag("难过", "😔", 0xFF42A5F5),
            MoodTag("愤怒", "😤", 0xFFF44336),
            MoodTag("惊喜", "🎉", 0xFFFF5722)
        )

        fun fromName(name: String?): MoodTag? {
            if (name == null) return null
            return defaults.firstOrNull { it.name == name }
        }
    }
}
