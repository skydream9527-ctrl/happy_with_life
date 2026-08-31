package com.xiaoquexing.app.data.media

import java.io.File

object VoiceFiles {
    const val MAX_DURATION_MS = 60_000L
    const val MIN_DURATION_MS = 400L
    const val MIN_BYTES = 256L

    data class Result(
        val ok: Boolean,
        val path: String? = null,
        val durationMs: Long = 0,
        val reason: String? = null,
    )

    fun accept(path: String?, durationMs: Long): Result {
        if (path.isNullOrBlank()) return Result(false, reason = "没有录音文件")
        val file = File(path)
        if (!file.exists() || file.length() < MIN_BYTES) {
            file.delete()
            return Result(false, reason = "录音文件损坏或过短")
        }
        if (durationMs < MIN_DURATION_MS) {
            file.delete()
            return Result(false, reason = "录音太短")
        }
        return Result(
            ok = true,
            path = path,
            durationMs = durationMs.coerceAtMost(MAX_DURATION_MS),
        )
    }

    fun remainingMs(elapsedMs: Long): Long =
        (MAX_DURATION_MS - elapsedMs).coerceAtLeast(0)
}
