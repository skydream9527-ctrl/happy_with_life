package com.xiaoquexing.app.data.media

import android.media.ExifInterface
import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoNormalizeTest {
    @Test
    fun degrees_mapsExifOrientation() {
        assertEquals(0f, PhotoNormalize.degrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(90f, PhotoNormalize.degrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180f, PhotoNormalize.degrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270f, PhotoNormalize.degrees(ExifInterface.ORIENTATION_ROTATE_270))
    }
}
