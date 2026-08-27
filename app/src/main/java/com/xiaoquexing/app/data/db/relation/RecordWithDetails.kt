package com.xiaoquexing.app.data.db.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.xiaoquexing.app.data.db.entity.RecordEntity
import com.xiaoquexing.app.data.db.entity.RecordMediaEntity
import com.xiaoquexing.app.data.db.entity.RecordTagCrossRef
import com.xiaoquexing.app.data.db.entity.TagEntity

/** 一次 @Transaction 查询取回记录 + 媒体 + 标签（Room 自动分批查询）。 */
data class RecordWithDetails(
    @Embedded val record: RecordEntity,
    @Relation(parentColumn = "local_id", entityColumn = "record_id")
    val media: List<RecordMediaEntity>,
    @Relation(
        parentColumn = "local_id",
        entityColumn = "local_id",
        associateBy = Junction(
            value = RecordTagCrossRef::class,
            parentColumn = "record_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity>
)
