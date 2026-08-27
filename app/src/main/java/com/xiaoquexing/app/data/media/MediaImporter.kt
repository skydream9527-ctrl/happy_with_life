package com.xiaoquexing.app.data.media

import android.content.Context
import android.net.Uri
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.MediaStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 媒体持久化（Z1-05 / ADR K5）：
 *
 * - 发布/编辑事务提交后，把 Photo Picker 返回的 content:// 复制到 App 私有目录，
 *   local_path 是渲染唯一来源，source_uri 仅溯源与重试；
 * - 复制失败（权限被收回、源被删除）置 MISSING，UI 显示占位图；
 * - 软删除记录超过保留期后清理其媒体文件（孤儿清理）。
 *
 * 不在 DB 事务内做文件 I/O：事务只写 PENDING_COPY 行，落盘是事务后的独立步骤。
 */
class MediaImporter(
    private val context: Context,
    private val db: AppDatabase
) {

    private val mediaDao = db.mediaDao()

    /** 落盘指定记录的待复制照片，返回成功张数。幂等：只处理 PENDING_COPY 行。 */
    suspend fun importPending(recordId: Long): Int = withContext(Dispatchers.IO) {
        val pending = mediaDao.pendingPhotos(recordId)
        var copied = 0
        pending.forEach { media ->
            val sourceUri = media.sourceUri ?: return@forEach
            runCatching {
                val file = copyToPrivateDir(Uri.parse(sourceUri), media.recordId, media.sortOrder, media.localId)
                mediaDao.updateLocalPath(media.localId, file.absolutePath, MediaStatus.READY, System.currentTimeMillis())
                copied++
            }.onFailure {
                mediaDao.updateLocalPath(media.localId, null, MediaStatus.MISSING, System.currentTimeMillis())
            }
        }
        copied
    }

    /** 删除软删除超过保留期的记录的媒体文件；行保留（同步元数据），local_path 置空。 */
    suspend fun cleanupOrphanFiles(retentionDays: Int = ORPHAN_RETENTION_DAYS): Int =
        withContext(Dispatchers.IO) {
            val cutoff = System.currentTimeMillis() - retentionDays * 24L * 3600 * 1000
            var removed = 0
            db.mediaDao().filesOfRecordsDeletedBefore(cutoff).forEach { media ->
                val path = media.localPath ?: return@forEach
                if (File(path).delete()) removed++
                mediaDao.updateLocalPath(media.localId, null, MediaStatus.MISSING, System.currentTimeMillis())
            }
            removed
        }

    private fun copyToPrivateDir(uri: Uri, recordId: Long, sortOrder: Int, mediaId: Long): File {
        val dir = File(context.filesDir, MEDIA_DIR).apply { mkdirs() }
        val target = File(dir, "r${recordId}_m${mediaId}_s${sortOrder}.${extensionOf(uri)}")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法打开输入流: $uri")
        return target
    }

    private fun extensionOf(uri: Uri): String {
        val fromPath = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase()
        return if (fromPath in KNOWN_EXTENSIONS) fromPath!! else "jpg"
    }

    companion object {
        const val MEDIA_DIR = "media/photos"
        const val ORPHAN_RETENTION_DAYS = 7
        private val KNOWN_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "gif")
    }
}
