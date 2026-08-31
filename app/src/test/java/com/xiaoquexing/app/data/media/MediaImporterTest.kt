package com.xiaoquexing.app.data.media

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.xiaoquexing.app.data.DataBootstrap
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.MediaStatus
import com.xiaoquexing.app.fixtures.testRecord
import com.xiaoquexing.app.data.repository.RecordRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.File

/**
 * 媒体落盘测试（Z1-05 / ADR K5）。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MediaImporterTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: RecordRepository
    private lateinit var importer: MediaImporter

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RecordRepository(db)
        importer = MediaImporter(context, db)
        runBlocking { DataBootstrap(db).ensureSeeded() }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `content图片复制到私有目录后置READY且localPath可读`() = runBlocking {
        val bytes = "fake-jpeg-bytes".toByteArray()
        val uri = Uri.parse("content://media/external/images/media/42")
        Shadows.shadowOf(context.contentResolver)
            .registerInputStream(uri, ByteArrayInputStream(bytes))

        val r = repo.publish(testRecord(moodTag = "开心", text = "a", photoUris = uri.toString()))
        // 发布事务后：PENDING_COPY
        val pending = db.mediaDao().forRecord(r.recordId).single { it.type == "PHOTO" }
        assertEquals(MediaStatus.PENDING_COPY, pending.mediaStatus)

        val copied = importer.importPending(r.recordId)

        assertEquals(1, copied)
        val done = db.mediaDao().forRecord(r.recordId).single { it.type == "PHOTO" }
        assertEquals(MediaStatus.READY, done.mediaStatus)
        assertNotNull(done.localPath)
        val file = File(done.localPath!!)
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
        assertTrue(file.path.contains("media/photos"))
        // 溯源 URI 保留（K5：重试与审计用）
        assertEquals(uri.toString(), done.sourceUri)
    }

    @Test
    fun `源不可读时置MISSING且不阻断发布结果`() = runBlocking {
        val gone = "content://media/external/images/media/999"
        val r = repo.publish(testRecord(moodTag = "开心", text = "a", photoUris = gone))

        val copied = importer.importPending(r.recordId)

        assertEquals(0, copied)
        val media = db.mediaDao().forRecord(r.recordId).single { it.type == "PHOTO" }
        assertEquals(MediaStatus.MISSING, media.mediaStatus)
        // 记录本身与 GP 不受影响
        assertTrue(db.recordDao().getRawById(r.recordId)!!.gpFinal > 0)
    }

    @Test
    fun `导入幂等：READY的行不会被重复处理`() = runBlocking {
        val uri = Uri.parse("content://media/external/images/media/7")
        Shadows.shadowOf(context.contentResolver)
            .registerInputStream(uri, ByteArrayInputStream("img".toByteArray()))
        val r = repo.publish(testRecord(moodTag = "开心", text = "a", photoUris = uri.toString()))

        assertEquals(1, importer.importPending(r.recordId))
        assertEquals(0, importer.importPending(r.recordId))
    }

    @Test
    fun `软删除超过保留期后清理孤儿文件`() = runBlocking {
        val voice = File(context.filesDir, "media/voice/old.m4a").apply {
            parentFile!!.mkdirs()
            writeText("voice")
        }
        val r = repo.publish(testRecord(moodTag = "开心", text = "a", voiceUri = voice.absolutePath))
        repo.softDelete(r.recordId)

        // 删除时间在保留期内：文件保留
        assertEquals(0, importer.cleanupOrphanFiles())
        assertTrue(voice.exists())

        // 直接把行改成过期删除时间（模拟 7 天后）：文件被清，local_path 置空
        db.openHelper.writableDatabase.execSQL(
            "UPDATE records SET deleted_at = ? WHERE local_id = ?",
            arrayOf(System.currentTimeMillis() - 8L * 24 * 3600 * 1000, r.recordId)
        )
        assertEquals(1, importer.cleanupOrphanFiles())
        assertFalse(voice.exists())
        assertTrue(db.mediaDao().forRecord(r.recordId).all { it.localPath == null })
    }
}
