package com.xiaoquexing.app.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrBitmaps {
    const val APP_LINK = "https://github.com/skydream9527-ctrl/happy_with_life"

    fun payload(recordId: Long): String =
        if (recordId > 0) "$APP_LINK#record=$recordId" else APP_LINK

    fun render(text: String, size: Int): Bitmap? = runCatching {
        val side = size.coerceIn(32, 1024)
        val matrix = QRCodeWriter().encode(
            text,
            BarcodeFormat.QR_CODE,
            side,
            side,
            mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            ),
        )
        val bmp = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        for (x in 0 until side) {
            for (y in 0 until side) {
                bmp.setPixel(x, y, if (matrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        bmp
    }.getOrNull()
}
