package com.xiaoquexing.app.ui.album

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.provider.MediaStore
import com.xiaoquexing.app.util.FileUtil
import com.xiaoquexing.app.viewmodel.AlbumSnapshot

object AlbumExporter {
    fun exportLongImage(context: Context, album: AlbumSnapshot): Boolean {
        val pages = pageTexts(album)
        val width = 1080
        val pageH = 420
        val bmp = Bitmap.createBitmap(width, pageH * pages.size + 80, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.parseColor("#F7F4EE"))
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2F5D3A")
            textSize = 48f
            isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 36f
        }
        pages.forEachIndexed { i, lines ->
            val top = 60f + i * pageH
            canvas.drawText(lines.first(), 48f, top, title)
            lines.drop(1).forEachIndexed { j, line ->
                canvas.drawText(line, 48f, top + 64f + j * 48f, body)
            }
        }
        return FileUtil.saveBitmapToGallery(context, bmp, album.title) != null
    }

    fun exportPdf(context: Context, album: AlbumSnapshot): Boolean {
        val pages = pageTexts(album)
        val doc = PdfDocument()
        val title = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2F5D3A")
            textSize = 18f
            isFakeBoldText = true
        }
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#333333")
            textSize = 12f
        }
        pages.forEachIndexed { i, lines ->
            val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)
            canvas.drawText(lines.first(), 48f, 72f, title)
            lines.drop(1).forEachIndexed { j, line ->
                canvas.drawText(line, 48f, 110f + j * 22f, body)
            }
            doc.finishPage(page)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "${album.title}.pdf")
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/XiaoQueXing")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            return try {
                if (uri == null) return false
                context.contentResolver.openOutputStream(uri)?.use { doc.writeTo(it) }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                true
            } catch (_: Throwable) {
                false
            } finally {
                doc.close()
            }
        }
        return try {
            val file = java.io.File(context.getExternalFilesDir(null), "${album.title}.pdf")
            file.outputStream().use { doc.writeTo(it) }
            true
        } catch (_: Throwable) {
            false
        } finally {
            doc.close()
        }
    }

    fun pageTexts(album: AlbumSnapshot): List<List<String>> = listOf(
        listOf(album.title, album.dateRange, "${album.recordCount} 条记录 · ${album.totalGp} GP", album.plantLabel),
        listOf("成长时间轴") + album.days.take(8).map { "${it.first}  +${it.second} GP" }.ifEmpty { listOf("还没有记录") },
        listOf("心情合集") + album.moods.take(8).map { "${it.first}  ${it.second} 次" }.ifEmpty { listOf("还没有心情") },
        listOf("标签精选") + album.tags.take(10).map { "#${it.first} ${it.second}" }.ifEmpty { listOf("还没有标签") },
        listOf("足迹") + album.locations.take(8).map { "${it.first}  ${it.second}次" }.ifEmpty { listOf("还没有地点") },
        listOf("BGM") + album.music.take(8).ifEmpty { listOf("还没有音乐") },
        listOf("链接") + album.links.take(8).ifEmpty { listOf("还没有链接") },
        listOf("继续记录吧", "每一个小确幸，都是生活给你的礼物"),
    )
}
