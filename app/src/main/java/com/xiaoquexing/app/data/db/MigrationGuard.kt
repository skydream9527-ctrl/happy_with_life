package com.xiaoquexing.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * 迁移前备份（room-v2-schema §8）：在 Room 打开数据库之前执行。
 *
 * 必须在 App 任何 DAO 访问前调用（AppDatabase.database 是 lazy 的，因此在
 * Application.onCreate 里先跑这里）。备份保留 7 天；v2 及以上的库不备份。
 *
 * 注：失败页/重试上限（§7）需要 UI 设计，与 M1-02 记录详情页一起交付；
 * 本类先保证「迁移失败也有完整 v1 副本可回滚」这一数据安全底线。
 */
object MigrationGuard {

    private const val BACKUP_DIR = "db_backup"
    private const val RETENTION_DAYS = 7

    /** 返回备份文件路径；库不存在或已是 v2+ 返回 null。 */
    fun backupV1IfPresent(context: Context, dbName: String): File? {
        val dbFile = context.getDatabasePath(dbName)
        if (!dbFile.exists()) return null

        val version = readVersionWithoutOpening(dbFile) ?: return null
        if (version != 1) return null

        val dir = File(context.filesDir, BACKUP_DIR).apply { mkdirs() }
        pruneOldBackups(dir)
        val stamp = System.currentTimeMillis()
        var copied: File? = null
        copyIfPresent(dbFile, File(dir, "$dbName-v1-$stamp.db"))?.let { copied = it }
        // -wal/-shm 是潜在的未检查点内容，必须一并备份
        copyIfPresent(File(dbFile.path + "-wal"), File(dir, "$dbName-v1-$stamp.db-wal"))
        copyIfPresent(File(dbFile.path + "-shm"), File(dir, "$dbName-v1-$stamp.db-shm"))
        return copied
    }

    /** 只读方式读 user_version；用只读句柄，避免触发 Room 的 onCreate/onUpgrade 路径。 */
    private fun readVersionWithoutOpening(dbFile: File): Int? = try {
        val db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
        try {
            db.version
        } finally {
            db.close()
        }
    } catch (_: Exception) {
        null
    }

    private fun copyIfPresent(src: File, dst: File): File? {
        if (!src.exists()) return null
        return try {
            src.copyTo(dst, overwrite = true)
        } catch (_: Exception) {
            // 备份失败按 §8 约定中止迁移安全网，但不应阻塞启动；调用方依赖 Room 自身回滚
            null
        }
    }

    private fun pruneOldBackups(dir: File) {
        val cutoff = System.currentTimeMillis() - RETENTION_DAYS * 24L * 3600 * 1000
        dir.listFiles()?.forEach { if (it.lastModified() < cutoff) it.delete() }
    }
}
