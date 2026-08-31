package com.xiaoquexing.app.media

import android.media.MediaPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.io.File

class VoicePlayer {
    private var player: MediaPlayer? = null

    fun play(path: String, onProgress: (Float) -> Unit, onDone: () -> Unit): Boolean {
        stop()
        val file = File(path)
        if (!file.exists() || file.length() < 256) return false
        return try {
            val mp = MediaPlayer().apply {
                setDataSource(path)
                setOnCompletionListener {
                    onProgress(1f)
                    onDone()
                    releaseInternal()
                }
                setOnErrorListener { _, _, _ ->
                    onDone()
                    releaseInternal()
                    true
                }
                prepare()
                start()
            }
            player = mp
            true
        } catch (_: Throwable) {
            releaseInternal()
            false
        }
    }

    fun pause() {
        runCatching { player?.pause() }
    }

    fun resume() {
        runCatching { if (player?.isPlaying == false) player?.start() }
    }

    fun isPlaying(): Boolean = player?.isPlaying == true

    fun progress(): Float {
        val mp = player ?: return 0f
        val dur = mp.duration.takeIf { it > 0 } ?: return 0f
        return (mp.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
    }

    fun stop() = releaseInternal()

    private fun releaseInternal() {
        try { player?.stop() } catch (_: Throwable) {}
        try { player?.release() } catch (_: Throwable) {}
        player = null
    }
}

@Composable
fun rememberVoicePlayer(): VoicePlayer {
    val player = remember { VoicePlayer() }
    DisposableEffect(player) {
        onDispose { player.stop() }
    }
    return player
}
