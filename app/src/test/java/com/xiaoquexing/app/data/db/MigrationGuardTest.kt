package com.xiaoquexing.app.data.db

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * 迁移前备份测试（room-v2-schema §8 / Z1-07 轮落地 MigrationGuard）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MigrationGuardTest {

    private val dbName = "guard-test.db"

    @Test
    fun `v1库在Room打开前被完整备份（含wal）`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        // 造一个带未检查点内容的 v1 库
        val v1 = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        v1.execSQL("CREATE TABLE t (id INTEGER PRIMARY KEY)")
        v1.execSQL("INSERT INTO t (id) VALUES (1)")
        File(v1.path + "-wal").writeBytes("wal-bytes".toByteArray())
        v1.version = 1
        val originalBytes = File(v1.path).readBytes()
        v1.close()

        val backup = MigrationGuard.backupV1IfPresent(context, dbName)

        assertNotNull(backup)
        assertEquals(originalBytes.size.toLong(), backup!!.length())
        assertTrue(File(backup.path + "-wal").exists())

        // 幂等：再次调用会生成新备份（时间戳不同），不破坏旧备份
        assertNotNull(MigrationGuard.backupV1IfPresent(context, dbName))
        assertTrue(backup.exists())
    }

    @Test
    fun `v2及以上或库不存在时不备份`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(dbName)

        // 库不存在
        assertNull(MigrationGuard.backupV1IfPresent(context, dbName))

        // version=2
        val db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null)
        db.version = 2
        db.close()
        assertNull(MigrationGuard.backupV1IfPresent(context, dbName))
        context.deleteDatabase(dbName)
    }
}
