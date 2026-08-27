package com.xiaoquexing.app.data

import androidx.room.withTransaction
import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.db.entity.MemberRoles
import com.xiaoquexing.app.data.db.entity.SpaceEntity
import com.xiaoquexing.app.data.db.entity.SpaceMemberEntity
import com.xiaoquexing.app.data.db.entity.SpacePlantEntity
import com.xiaoquexing.app.data.db.entity.UserEntity
import com.xiaoquexing.app.data.entity.PlantType

/**
 * 首启初始化（room-v2-schema §5 之外的另一条入口：全新安装时 Room onCreate 不会跑
 * Migration，种子在这里补齐）。迁移过的库 users 非空，直接跳过。
 *
 * 返回 true = 本次执行了首次种子（供调用方决定是否随后注入 Debug Demo 数据）。
 */
class DataBootstrap(private val db: AppDatabase) {

    suspend fun ensureSeeded(): Boolean {
        if (db.userDao().countUsers() > 0) return false
        var seeded = false
        db.withTransaction {
            if (db.userDao().countUsers() > 0) return@withTransaction

            val userId = db.userDao().insert(
                UserEntity(displayName = SeedData.LOCAL_USER_NAME, createdAt = now(), updatedAt = now())
            )
            val spaceId = db.spaceDao().insert(
                SpaceEntity(name = SeedData.DEFAULT_SPACE_NAME, isDefault = true, createdAt = now(), updatedAt = now())
            )
            db.spaceMemberDao().insert(
                SpaceMemberEntity(
                    spaceId = spaceId, userId = userId, role = MemberRoles.OWNER,
                    joinedAt = now(), createdAt = now(), updatedAt = now()
                )
            )
            db.plantDao().upsertPlantDefs(SeedData.plantDefs(now()))
            db.plantDao().insertSpacePlant(
                SpacePlantEntity(
                    spaceId = spaceId, plantType = PlantType.TREE.name,
                    isActive = true, startedAt = now(), createdAt = now(), updatedAt = now()
                )
            )
            db.achievementDao().upsertDefs(SeedData.achievementDefs(now()))
            SeedData.tags(now()).forEach { db.tagDao().insert(it) }
            seeded = true
        }
        return seeded
    }

    private fun now(): Long = System.currentTimeMillis()
}
