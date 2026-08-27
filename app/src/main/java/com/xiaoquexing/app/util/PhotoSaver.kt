package com.xiaoquexing.app.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把分享卡片 Bitmap 存到系统相册。
 *
 * 包装 [FileUtil.saveBitmapToGallery] 并把 IO 移到 IO 线程，
 * 返回 [SaveResult] 表达成功/失败/取消。
 *
 * 失败原因通常：
 * 1) Android 9 及以下且未声明 WRITE_EXTERNAL_STORAGE —— 我们已声明，且 minSdk=26
 *    但低于 Q 的设备需要权限。这里抛回到调用方弹 snackbar 提示去授权。
 * 2) MediaStore 写入失败（磁盘满等）。
 */
object PhotoSaver {

    sealed class SaveResult {
        data class Success(val uri: Uri, val displayPath: String) : SaveResult()
        object Failed : SaveResult()
    }

    suspend fun saveShareCard(
        context: Context,
        bitmap: Bitmap,
        title: String = "XiaoQueXing_${System.currentTimeMillis()}"
    ): SaveResult = withContext(Dispatchers.IO) {
        try {
            val uri = FileUtil.saveBitmapToGallery(context, bitmap, title)
            if (uri != null) {
                SaveResult.Success(uri, "相册/Pictures/XiaoQueXing/$title.jpg")
            } else {
                SaveResult.Failed
            }
        } catch (t: Throwable) {
            android.util.Log.e("PhotoSaver", "save failed", t)
            SaveResult.Failed
        }
    }
}
