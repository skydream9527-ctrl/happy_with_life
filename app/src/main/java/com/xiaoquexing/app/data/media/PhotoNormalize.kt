package com.xiaoquexing.app.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.io.File

object PhotoNormalize {
    const val MAX_EDGE = 1920
    const val MAX_BYTES = 5 * 1024 * 1024

    fun degrees(orientation: Int): Float = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }

    fun write(context: Context, source: Uri, dest: File): File {
        dest.parentFile?.mkdirs()
        val normalized = runCatching { encode(context, source) }.getOrNull()
        if (normalized != null) {
            dest.outputStream().use { it.write(normalized) }
            return dest
        }
        context.contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法打开输入流: $source")
        return dest
    }

    fun writeFile(source: File, dest: File): File {
        dest.parentFile?.mkdirs()
        val normalized = runCatching { encodeFile(source) }.getOrNull()
        if (normalized != null) {
            dest.outputStream().use { it.write(normalized) }
            return dest
        }
        source.copyTo(dest, overwrite = true)
        return dest
    }

    fun compressExisting(file: File): Packed {
        val bytes = runCatching { encodeFile(file) }.getOrNull() ?: file.readBytes()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        return Packed(bytes, bounds.outWidth.coerceAtLeast(1), bounds.outHeight.coerceAtLeast(1))
    }

    private fun encode(context: Context, uri: Uri): ByteArray {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = sampleFor(bounds.outWidth, bounds.outHeight)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: error("decode failed")
        return jpeg(rotate(raw, degrees(orientation)))
    }

    private fun encodeFile(file: File): ByteArray {
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val sample = sampleFor(bounds.outWidth, bounds.outHeight)
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val raw = BitmapFactory.decodeFile(file.absolutePath, opts) ?: error("decode failed")
        return jpeg(rotate(raw, degrees(orientation)))
    }

    private fun sampleFor(w: Int, h: Int): Int {
        var sample = 1
        val maxSide = maxOf(w, h).coerceAtLeast(1)
        while (maxSide / sample > MAX_EDGE) sample *= 2
        return sample
    }

    private fun rotate(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val matrix = Matrix().apply { postRotate(degrees) }
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (out != src && !src.isRecycled) src.recycle()
        return out
    }

    private fun jpeg(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        var quality = 82
        do {
            out.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            quality -= 8
        } while (out.size() > MAX_BYTES && quality >= 50)
        if (!bitmap.isRecycled) bitmap.recycle()
        return out.toByteArray()
    }

    data class Packed(val bytes: ByteArray, val width: Int, val height: Int)
}
