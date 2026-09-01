package com.xiaoquexing.app.ui.theme

import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * 二级页跟手返回：系统预测性返回（Android 14+）+ 左缘滑动兜底。
 * 进行中页面缩小右移并露圆角；松手不够则回弹。
 */
@Composable
fun PredictiveBackPane(
    enabled: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val reduce = Motion.reduce()
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val edgePx = with(density) { 36.dp.toPx() }

    fun setProgress(value: Float) {
        scope.launch { progress.snapTo(value.coerceIn(0f, 1f)) }
    }

    fun cancel() {
        scope.launch { progress.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow)) }
    }

    fun commit() {
        scope.launch {
            if (!reduce) progress.animateTo(1f, tween(120))
            runCatching { onBack() }
            progress.snapTo(0f)
        }
    }

    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(dispatcher, lifecycleOwner, enabled) {
        if (dispatcher == null || !enabled) return@DisposableEffect onDispose { }
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                if (!reduce) setProgress(backEvent.progress)
            }

            override fun handleOnBackCancelled() {
                cancel()
            }

            override fun handleOnBackPressed() {
                commit()
            }
        }
        dispatcher.addCallback(lifecycleOwner, callback)
        onDispose { callback.remove() }
    }

    if (!enabled) {
        content()
        return
    }

    val p = if (reduce) 0f else progress.value
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.22f * p))
            .pointerInput(enabled, reduce) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (down.position.x > edgePx) return@awaitEachGesture
                    var drag = 0f
                    val width = size.width.coerceAtLeast(1)
                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        if (!change.pressed) {
                            if (drag / width > 0.18f) commit() else cancel()
                            break
                        }
                        val dx = change.position.x - down.position.x
                        if (dx > 0f) change.consume()
                        drag = dx
                        if (!reduce) setProgress((drag / width).coerceIn(0f, 1f))
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1f - 0.08f * p
                    scaleY = 1f - 0.08f * p
                    translationX = 80.dp.toPx() * p
                    alpha = 1f - 0.18f * p
                    shadowElevation = 16f * p
                    shape = RoundedCornerShape(28.dp * p)
                    clip = p > 0.01f
                },
        ) {
            content()
        }
    }
}
