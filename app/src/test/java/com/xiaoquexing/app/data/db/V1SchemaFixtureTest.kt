package com.xiaoquexing.app.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.xiaoquexing.app.fixtures.V1SchemaFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * v1 schema fixture 自检（Z0-05）。
 *
 * 证明手写 DDL 能在真 SQLite 上建出 v1 四张表并接受 v1 形状的数据（含 moodTag=NULL
 * 这一已知缺陷，ADR-001 R4）。真实的 v1->v2 迁移断言在 [MigrationFromV1Test]。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class V1SchemaFixtureTest {

    @Test
    fun `v1 DDL 可建出全部四张表并接受 v1 形状数据`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.deleteDatabase(V1SchemaFixture.DB_NAME)
        val db = context.openOrCreateDatabase(V1SchemaFixture.DB_NAME, Context.MODE_PRIVATE, null)
        try {
            V1SchemaFixture.CREATE_STATEMENTS.forEach(db::execSQL)

            val tables = tableNames(db)
            assertTrue(tables.containsAll(listOf("records", "plant_states", "achievements", "spaces")))

            // v1 允许 moodTag 为 NULL：迁移必须回填而不是丢行（room-v2-schema §4.1）
            db.execSQL(
                "INSERT INTO records (text, moodTag, statusTags, photoUris, voiceDuration, " +
                    "gpEarned, createdAt, isBackdated) VALUES ('t', NULL, '自然', 'a|b', 0, 25, 1000, 0)"
            )
            db.execSQL(
                "INSERT INTO plant_states (plantType, totalGp, isActive, isUnlocked, plantedAt) " +
                    "VALUES ('TREE', 0, 1, 1, 1000)"
            )
            db.execSQL(
                "INSERT INTO achievements (code, title, description, emoji, requirement, progress, isUnlocked) " +
                    "VALUES ('first_record', '初次记录', '第一条小确幸', '🌱', 1, 1, 1)"
            )
            db.execSQL(
                "INSERT INTO spaces (name, description, type, memberCount, createdAt) " +
                    "VALUES ('我的空间', '', 'PERSONAL', 1, 1000)"
            )

            assertEquals(1, count(db, "SELECT COUNT(*) FROM records"))
            assertEquals(1, count(db, "SELECT COUNT(*) FROM plant_states"))
            assertEquals(1, count(db, "SELECT COUNT(*) FROM achievements"))
            assertEquals(1, count(db, "SELECT COUNT(*) FROM spaces"))
        } finally {
            db.close()
        }
    }

    private fun tableNames(db: SQLiteDatabase): List<String> =
        db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }

    private fun count(db: SQLiteDatabase, sql: String): Int =
        db.rawQuery(sql, null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }
}
