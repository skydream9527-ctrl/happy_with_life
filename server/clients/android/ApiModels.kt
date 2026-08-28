package com.xiaoquexing.app.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(val data: T? = null, val error: ApiError? = null)

@Serializable
data class ApiError(val code: String, val message: String, val retryable: Boolean = false)

@Serializable
data class SmsSendReq(val phone: String, val deviceId: String)

@Serializable
data class SmsVerifyReq(
    val phone: String,
    val code: String,
    val deviceId: String,
    val platform: String = "android",
    val appVersion: String = "",
)

@Serializable
data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Int,
    val userId: String,
    val deviceId: String,
    val displayName: String = "",
)

@Serializable
data class RefreshReq(val refreshToken: String, val deviceId: String)

@Serializable
data class Profile(
    val userId: String,
    val displayName: String,
    val status: String,
    val maskedPhone: String,
    val personalSpaceId: String,
    val createdAt: String,
)

@Serializable
data class SpaceList(val items: List<SpaceDto> = emptyList())

@Serializable
data class SpaceDto(
    val id: String,
    val name: String,
    val spaceType: String,
    val totalGp: Long = 0,
    val plantStage: String = "SEED",
    val timezone: String = "Asia/Shanghai",
)

@Serializable
data class MemberList(val items: List<MemberDto> = emptyList())

@Serializable
data class MemberDto(
    val userId: String,
    val role: String,
    val status: String,
    val contributedGp: Long = 0,
)

@Serializable
data class InviteDto(
    val inviteId: String,
    val token: String,
    val link: String,
    val expiresAt: String = "",
    val maxUses: Int = 10,
)

@Serializable
data class InvitePeek(
    val spaceId: String,
    val spaceName: String,
    val spaceType: String,
    val plantType: String = "",
    val seatsLeft: Int = 0,
)

@Serializable
data class RecordWrite(
    val spaceId: String,
    val moodTag: String,
    val contentText: String = "",
    val timezone: String = "Asia/Shanghai",
    val occurredAt: String? = null,
    val occurredDate: String? = null,
    val statusTags: List<String> = emptyList(),
    val media: List<MediaRef> = emptyList(),
    val baseVersion: Long = 0,
)

@Serializable
data class MediaRef(val mediaId: String, val type: String = "PHOTO")

@Serializable
data class MediaStsReq(
    val type: String = "PHOTO",
    val mimeType: String,
    val sizeBytes: Long,
    val width: Int = 0,
    val height: Int = 0,
)

@Serializable
data class MediaSts(
    val mediaId: String,
    val objectKey: String = "",
    val method: String = "PUT",
    val uploadUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val expiresAt: String = "",
    val provider: String = "mock",
    val quotaUsed: Long = 0,
    val quotaMax: Long = 209715200,
)

@Serializable
data class MediaObject(
    val id: String,
    val uploadStatus: String,
    val sizeBytes: Long = 0,
    val mimeType: String = "",
    val type: String = "PHOTO",
)

@Serializable
data class MediaQuota(val usedBytes: Long = 0, val maxBytes: Long = 209715200)

@Serializable
data class MediaDownload(val url: String, val expiresAt: String = "")

@Serializable
data class MutationResult(
    val mutationId: String? = null,
    val status: String,
    val serverId: String? = null,
    val version: Long = 0,
    val authoritative: Authoritative? = null,
)

@Serializable
data class Authoritative(
    val gpFinal: Int = 0,
    val gpCapped: Boolean = false,
    val spaceTotalGp: Long = 0,
    val plantStage: String = "SEED",
    val streakDays: Int = 0,
)

@Serializable
data class RecordList(val items: List<RecordDto> = emptyList(), val nextCursor: String = "", val hasMore: Boolean = false)

@Serializable
data class RecordDto(
    val id: String,
    val spaceId: String,
    val contentText: String = "",
    val moodTag: String,
    val occurredDate: String,
    val gpFinal: Int = 0,
    val version: Long = 0,
)

@Serializable
data class CalendarDto(
    val spaceId: String,
    val from: String,
    val to: String,
    val totalGp: Long = 0,
    val plantStage: String = "SEED",
    val streakDays: Int = 0,
    val days: List<DayStat> = emptyList(),
)

@Serializable
data class DayStat(
    val date: String,
    val moodTag: String,
    val recordCount: Int,
    val gpTotal: Int,
)
