package com.xiaoquexing.app.data.remote

import android.content.Context

class PlanStore(context: Context) {
    private val prefs = context.getSharedPreferences("xqx_plan", Context.MODE_PRIVATE)

    var previewMember: Boolean
        get() = prefs.getBoolean(KEY, false)
        set(value) { prefs.edit().putBoolean(KEY, value).apply() }

    val tier: String get() = if (previewMember) TIER_MEMBER else TIER_FREE

    companion object {
        private const val KEY = "preview_member"
        const val TIER_FREE = "free"
        const val TIER_MEMBER = "member"

        val freeBenefits = listOf(
            "个人空间无限记录",
            "本地备份与恢复",
            "每日提醒 / 纪念日",
            "分享卡片与二维码",
        )
        val memberBenefits = listOf(
            "预留更多共享空间名额",
            "画册高清导出（后续开放）",
            "AI 小记助手（下一项）",
            "主题与装扮（后续开放）",
        )
    }
}
