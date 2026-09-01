package com.xiaoquexing.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

enum class MusicPlatform(val label: String, val artistFallback: String) {
    NETEASE("网易云音乐", "网易云音乐"),
    QQ("QQ音乐", "QQ音乐"),
}

data class PickedSong(
    val title: String,
    val artist: String,
    val uri: String,
    val platform: MusicPlatform,
)

object MusicShare {
    fun parse(raw: String, platform: MusicPlatform): PickedSong? {
        val text = raw.trim()
        if (text.isBlank()) return null
        val url = extractUrl(text)
        val inferred = when {
            url.contains("music.163.com") || url.contains("163cn.tv") -> MusicPlatform.NETEASE
            url.contains("y.qq.com") || url.contains("i.y.qq.com") || url.contains("qq.com/song") -> MusicPlatform.QQ
            else -> platform
        }
        val title = extractTitle(text, url).ifBlank { inferred.label + "歌曲" }
        return PickedSong(title = title.take(40), artist = inferred.artistFallback, uri = url.ifBlank { text }, platform = inferred)
    }

    fun open(context: Context, platform: MusicPlatform, query: String = "") {
        val encoded = Uri.encode(query.ifBlank { "热歌" })
        val app = when (platform) {
            MusicPlatform.NETEASE -> Intent(Intent.ACTION_VIEW, Uri.parse("orpheus://#/search?s=$encoded"))
            MusicPlatform.QQ -> Intent(Intent.ACTION_VIEW, Uri.parse("qqmusic://qq.com/media/search?key=$encoded"))
        }
        app.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val web = when (platform) {
            MusicPlatform.NETEASE -> Intent(Intent.ACTION_VIEW, Uri.parse("https://music.163.com/#/search/m/?s=$encoded"))
            MusicPlatform.QQ -> Intent(Intent.ACTION_VIEW, Uri.parse("https://y.qq.com/n/ryqq/search?w=$encoded"))
        }
        web.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(app) }.onFailure {
            runCatching { context.startActivity(web) }
        }
    }

    private fun extractUrl(text: String): String =
        Regex("https?://\\S+").find(text)?.value?.trimEnd(')', ']', ',', '。', '，') ?: text.takeIf { it.startsWith("http") }.orEmpty()

    private fun extractTitle(text: String, url: String): String {
        val withoutUrl = if (url.isBlank()) text.replace('\n', ' ').trim()
        else text.replace(url, " ").replace('\n', ' ').trim()
        if (withoutUrl.isNotBlank() && withoutUrl.length in 1..40 && !withoutUrl.startsWith("http")) {
            return withoutUrl.removePrefix("分享").trim()
        }
        val fromQuery = Regex("[?&](?:s|w|songname|title)=([^&]+)").find(url)?.groupValues?.getOrNull(1)
        if (!fromQuery.isNullOrBlank()) {
            return runCatching { java.net.URLDecoder.decode(fromQuery, Charsets.UTF_8.name()) }.getOrDefault(fromQuery)
        }
        return ""
    }
}
