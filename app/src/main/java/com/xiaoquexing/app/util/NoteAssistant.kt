package com.xiaoquexing.app.util

data class CaptionHint(
    val mood: String? = null,
    val text: String = "",
    val location: String? = null,
    val hour: Int = 12,
)

fun interface CaptionWriter {
    fun write(hint: CaptionHint): String
}

/** 规则版小记。以后换成模型时只替换 [writer]。 */
object NoteAssistant {
    var writer: CaptionWriter = CaptionWriter(::ruleWrite)

    fun suggest(hint: CaptionHint): String = writer.write(hint).trim()

    fun ruleWrite(hint: CaptionHint): String {
        val whenWord = when (hint.hour) {
            in 5..10 -> "清晨"
            in 11..13 -> "正午"
            in 14..17 -> "午后"
            in 18..21 -> "傍晚"
            else -> "夜里"
        }
        val moodBit = when (hint.mood) {
            "开心" -> "心里暖了一下"
            "平静" -> "世界安静了几秒"
            "感激" -> "想把这一幕记下"
            "期待" -> "后面的日子也有光"
            "疲惫" -> "还是给自己留了一点温柔"
            "难过" -> "允许自己慢一点"
            else -> "有一件很小的好事"
        }
        val place = hint.location?.trim().orEmpty().takeIf { it.isNotBlank() }?.let { "在$it，" } ?: ""
        val existing = hint.text.trim()
        return if (existing.isBlank()) {
            "${place}${whenWord}，$moodBit。"
        } else {
            "$existing\n$place$whenWord，就这样记下来。"
        }
    }
}
