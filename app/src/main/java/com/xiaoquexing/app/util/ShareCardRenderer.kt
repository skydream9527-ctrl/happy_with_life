package com.xiaoquexing.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import java.io.File
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.model.ShareCardData
import kotlin.math.min

object ShareCardRenderer {
    private val BG_COLOR = android.graphics.Color.parseColor("#E8F5E9")
    private val CARD_COLOR = android.graphics.Color.WHITE
    private val PRIMARY = android.graphics.Color.parseColor("#4CAF50")
    private val TEXT_PRIMARY = android.graphics.Color.parseColor("#212121")
    private val TEXT_SECONDARY = android.graphics.Color.parseColor("#757575")
    private val ACCENT_PINK = android.graphics.Color.parseColor("#F48FB1")

    fun render(data: ShareCardData, width: Int = 720, context: Context? = null): Bitmap {
        val height = (width * 1.4f).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = width / 360f

        // Background
        canvas.drawColor(BG_COLOR)

        val padding = (24 * density).toInt()
        val cardRect = RectF(
            padding.toFloat(),
            padding.toFloat(),
            (width - padding).toFloat(),
            (height - padding).toFloat()
        )
        val cardPaint = Paint().apply {
            color = CARD_COLOR
            isAntiAlias = true
            setShadowLayer(12f * density, 0f, 4f * density, android.graphics.Color.argb(30, 0, 0, 0))
        }
        canvas.drawRoundRect(cardRect, 24f * density, 24f * density, cardPaint)

        val innerPadding = 30f * density
        var y = cardRect.top + innerPadding

        // Header: date + plant emoji
        val headerPaint = Paint().apply {
            color = TEXT_SECONDARY
            textSize = 24f * density
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(data.dateStr, cardRect.left + innerPadding, y + 24f * density, headerPaint)

        val emojiPaint = Paint().apply {
            textSize = 48f * density
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText(data.plantType.emoji, cardRect.right - innerPadding, y + 40f * density, emojiPaint)

        y += 60f * density

        // Mood emoji (large)
        if (data.moodEmoji.isNotEmpty()) {
            val moodPaint = Paint().apply {
                textSize = 72f * density
                isAntiAlias = true
            }
            canvas.drawText(data.moodEmoji, cardRect.left + innerPadding, y + 70f * density, moodPaint)
            y += 90f * density
        }

        // Record text
        if (data.recordText.isNotEmpty()) {
            val textPaint = Paint().apply {
                color = TEXT_PRIMARY
                textSize = 32f * density
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val maxTextWidth = cardRect.width() - innerPadding * 2
            val textToShow = if (data.recordText.length > 120) data.recordText.take(117) + "..." else data.recordText
            val textY = drawMultilineText(canvas, textToShow, cardRect.left + innerPadding, y.toFloat(), maxTextWidth, textPaint)
            y = textY + 20f * density
        }

        if (data.photoUris.isNotEmpty()) {
            val photoAreaTop = y.toFloat()
            val photoAreaH = 220f * density
            val photoRect = RectF(
                cardRect.left + innerPadding, photoAreaTop,
                cardRect.right - innerPadding, photoAreaTop + photoAreaH
            )
            val decoded = context?.let { decodePhoto(it, data.photoUris.first(), photoRect.width().toInt(), photoRect.height().toInt()) }
            if (decoded != null) {
                canvas.save()
                canvas.clipRect(photoRect)
                val src = Rect(0, 0, decoded.width, decoded.height)
                canvas.drawBitmap(decoded, src, photoRect, Paint(Paint.FILTER_BITMAP_FLAG))
                canvas.restore()
                decoded.recycle()
            } else {
                val photoPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#F5F5F5")
                    style = Paint.Style.FILL
                }
                canvas.drawRoundRect(photoRect, 12f * density, 12f * density, photoPaint)
                val placeholderPaint = Paint().apply {
                    color = TEXT_SECONDARY
                    textSize = 20f * density
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("📷 ${data.photoUris.size} 张照片", photoRect.centerX(), photoRect.centerY() + 8f * density, placeholderPaint)
            }
            y += photoAreaH + 20f * density
        }

        // Music bar
        if (data.musicTitle != null) {
            val musicH = 80f * density
            val musicPaint = Paint().apply {
                color = android.graphics.Color.parseColor("#F3E5F5")
                isAntiAlias = true
            }
            val musicRect = RectF(
                cardRect.left + innerPadding, y.toFloat(),
                cardRect.right - innerPadding, y + musicH
            )
            canvas.drawRoundRect(musicRect, 12f * density, 12f * density, musicPaint)
            val mIconPaint = Paint().apply { textSize = 36f * density; isAntiAlias = true }
            canvas.drawText("🎵", musicRect.left + 20f * density, musicRect.centerY() + 12f * density, mIconPaint)
            val mTextPaint = Paint().apply {
                color = TEXT_PRIMARY
                textSize = 24f * density
                isAntiAlias = true
            }
            canvas.drawText(data.musicTitle, musicRect.left + 70f * density, musicRect.centerY() - 4f * density, mTextPaint)
            if (data.musicArtist != null) {
                val subPaint = Paint().apply {
                    color = TEXT_SECONDARY
                    textSize = 20f * density
                    isAntiAlias = true
                }
                canvas.drawText(data.musicArtist, musicRect.left + 70f * density, musicRect.centerY() + 22f * density, subPaint)
            }
            y += musicH + 20f * density
        }

        // GP info
        val gpPaint = Paint().apply {
            color = PRIMARY
            textSize = 28f * density
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("🌱 ${data.totalGp} GP", cardRect.left + innerPadding, y + 30f * density, gpPaint)

        // Divider
        y += 50f * density
        val dividerPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#E0E0E0")
            strokeWidth = 1f * density
        }
        canvas.drawLine(
            cardRect.left + innerPadding, y.toFloat(),
            cardRect.right - innerPadding, y.toFloat(),
            dividerPaint
        )
        y += 20f * density

        // Footer
        val footerPaint = Paint().apply {
            color = PRIMARY
            textSize = 24f * density
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            data.footerText,
            cardRect.centerX(),
            (cardRect.bottom - innerPadding - 20f * density),
            footerPaint
        )

        // QR code placeholder (small square)
        val qrSize = 80f * density
        val qrRect = RectF(
            cardRect.right - innerPadding - qrSize,
            cardRect.bottom - innerPadding - qrSize - 40f * density,
            cardRect.right - innerPadding,
            cardRect.bottom - innerPadding - 40f * density
        )
        val qrBgPaint = Paint().apply { color = android.graphics.Color.parseColor("#F5F5F5") }
        canvas.drawRoundRect(qrRect, 8f * density, 8f * density, qrBgPaint)
        val qrBmp = QrBitmaps.render(data.qrPayload.ifBlank { QrBitmaps.APP_LINK }, qrSize.toInt())
        if (qrBmp != null) {
            canvas.drawBitmap(qrBmp, null, qrRect, null)
            if (!qrBmp.isRecycled) qrBmp.recycle()
        } else {
            val qrTextPaint = Paint().apply {
                color = TEXT_SECONDARY
                textSize = 16f * density
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("二维码", qrRect.centerX(), qrRect.centerY() + 6f * density, qrTextPaint)
        }

        return bitmap
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: Paint
    ): Float {
        var currentY = y
        val lineHeight = paint.textSize + paint.fontSpacing - paint.textSize
        val chars = text.toCharArray()
        var line = StringBuilder()
        for (ch in chars) {
            if (ch == '\n') {
                canvas.drawText(line.toString(), x, currentY + paint.textSize, paint)
                currentY += lineHeight
                line = StringBuilder()
                continue
            }
            val testLine = line.toString() + ch
            val testWidth = paint.measureText(testLine)
            if (testWidth > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, currentY + paint.textSize, paint)
                currentY += lineHeight
                line = StringBuilder().append(ch)
            } else {
                line.append(ch)
            }
        }
        if (line.isNotEmpty()) {
            canvas.drawText(line.toString(), x, currentY + paint.textSize, paint)
            currentY += lineHeight
        }
        return currentY
    }

    fun decodePhoto(context: Context, uriOrPath: String, maxW: Int, maxH: Int): Bitmap? {
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(context, uriOrPath)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val w = bounds.outWidth.coerceAtLeast(1)
            val h = bounds.outHeight.coerceAtLeast(1)
            while (w / sample > maxW * 2 || h / sample > maxH * 2) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            openStream(context, uriOrPath)?.use { BitmapFactory.decodeStream(it, null, opts) }
        }.getOrNull()
    }

    private fun openStream(context: Context, uriOrPath: String) = when {
        uriOrPath.startsWith("content://") || uriOrPath.startsWith("file://") ->
            context.contentResolver.openInputStream(Uri.parse(uriOrPath))
        else -> File(uriOrPath).takeIf { it.exists() }?.inputStream()
    }
}
