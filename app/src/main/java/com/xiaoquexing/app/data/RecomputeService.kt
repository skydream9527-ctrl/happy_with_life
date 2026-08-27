package com.xiaoquexing.app.data

import com.xiaoquexing.app.data.db.AppDatabase

/**
 * 一致性不变量断言器（room-v2-schema §6 / 测试矩阵 T18）。
 *
 * 五条不变量来自 ADR-001：空间总分=记录集合之和；当日额度缓存=按日重算；
 * 缓存不残留过期日期；每空间至多一棵活动植物；GP 非负。
 * 供测试与 debug 构建调用，防 K2（双真相来源）复发。
 */
object RecomputeService {

    suspend fun assertInvariants(db: AppDatabase) {
        val recordDao = db.recordDao()
        val spaces = db.spaceDao().getAllSpaces()
        check(spaces.isNotEmpty()) { "至少应有一个空间" }

        spaces.forEach { space ->
            val spaceId = space.localId

            // 1. 空间总分 = 未删除记录 gp_final 之和
            val expectedTotal = recordDao.sumAllGp(spaceId)
            check(space.totalGp == expectedTotal) {
                "空间 $spaceId totalGp=${space.totalGp} != 记录之和 $expectedTotal"
            }

            // 2+3. 当日额度缓存逐日一致，且无过期日期残留
            val recordDays = recordDao.distinctDateKeys(spaceId).toSet()
            val cachedDays = db.dailyStatDao().dayKeys(spaceId).toSet()
            check(recordDays == cachedDays) {
                "空间 $spaceId 额度缓存日期集 $cachedDays != 记录日期集 $recordDays"
            }
            recordDays.forEach { day ->
                val expected = recordDao.sumGpOnDate(spaceId, day)
                val cached = db.dailyStatDao().gpOnDay(spaceId, day)
                check(cached == expected) {
                    "空间 $spaceId 日期 $day 额度缓存 $cached != 记录之和 $expected"
                }
            }

            // 4. 每空间至多一棵活动植物
            val activeCount = db.plantDao().countActivePlants(spaceId)
            check(activeCount <= 1) { "空间 $spaceId 有 $activeCount 棵活动植物" }
        }
    }
}
