package com.xiaoquexing.app.fixtures

import com.xiaoquexing.app.data.entity.Record

/** 测试用 Record 构造器：只覆写关心的字段，其余走合理默认值。 */
fun testRecord(
    id: Long = 0L,
    text: String = "一条测试小确幸",
    moodTag: String? = "平静",
    statusTags: String = "",
    photoUris: String = "",
    voiceUri: String? = null,
    musicTitle: String? = null,
    linkUrl: String? = null,
    locationName: String? = null,
    gpEarned: Int = 10,
    createdAt: Long = System.currentTimeMillis(),
    isBackdated: Boolean = false
): Record = Record(
    id = id,
    text = text,
    moodTag = moodTag,
    statusTags = statusTags,
    photoUris = photoUris,
    voiceUri = voiceUri,
    voiceDuration = 0,
    musicTitle = musicTitle,
    musicArtist = null,
    musicUri = null,
    linkUrl = linkUrl,
    linkTitle = null,
    linkSummary = null,
    locationName = locationName,
    locationLat = null,
    locationLng = null,
    gpEarned = gpEarned,
    createdAt = createdAt,
    isBackdated = isBackdated
)
