package com.xiaoquexing.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @POST("/api/v1/auth/sms/send")
    suspend fun smsSend(@Body body: SmsSendReq): Envelope<Map<String, Boolean>>

    @POST("/api/v1/auth/sms/verify")
    suspend fun smsVerify(@Body body: SmsVerifyReq): Envelope<TokenPair>

    @POST("/api/v1/auth/register")
    suspend fun register(@Body body: PasswordAuthReq): Envelope<TokenPair>

    @POST("/api/v1/auth/login")
    suspend fun login(@Body body: PasswordAuthReq): Envelope<TokenPair>

    @POST("/api/v1/auth/password/change")
    suspend fun changePassword(@Body body: ChangePasswordReq): Envelope<Map<String, Boolean>>

    @POST("/api/v1/auth/password/reset-on-device")
    suspend fun resetPasswordOnDevice(@Body body: ResetPasswordReq): Envelope<Map<String, Boolean>>

    @POST("/api/v1/auth/token/refresh")
    suspend fun refresh(@Body body: RefreshReq): Envelope<TokenPair>

    @POST("/api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshReq): Envelope<Map<String, Boolean>>

    @retrofit2.http.DELETE("/api/v1/account")
    suspend fun deleteAccount(): Envelope<AccountDeleteRes>

    @GET("/api/v1/me")
    suspend fun me(): Envelope<Profile>

    @PATCH("/api/v1/me")
    suspend fun patchMe(@Body body: Map<String, String>): Envelope<Profile>

    @GET("/api/v1/spaces")
    suspend fun spaces(): Envelope<SpaceList>

    @POST("/api/v1/spaces")
    suspend fun createSpace(@Body body: Map<String, String>): Envelope<SpaceDto>

    @GET("/api/v1/spaces/{id}/members")
    suspend fun members(@Path("id") id: String): Envelope<MemberList>

    @POST("/api/v1/spaces/{id}/invites")
    suspend fun createInvite(@Path("id") id: String): Envelope<InviteDto>

    @POST("/api/v1/invites/accept")
    suspend fun acceptInvite(@Body body: Map<String, String>): Envelope<SpaceDto>

    @GET("/api/v1/invites/{token}")
    suspend fun peekInvite(@Path("token") token: String): Envelope<InvitePeek>

    @GET("/api/v1/spaces/{id}/plant")
    suspend fun plant(@Path("id") id: String): Envelope<Map<String, String>>

    @GET("/api/v1/stats/calendar")
    suspend fun calendar(
        @Query("spaceId") spaceId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
    ): Envelope<CalendarDto>

    @GET("/api/v1/records")
    suspend fun records(
        @Query("spaceId") spaceId: String,
        @Query("from") from: String? = null,
        @Query("to") to: String? = null,
        @Query("mood") mood: String? = null,
        @Query("cursor") cursor: String? = null,
    ): Envelope<RecordList>

    @GET("/api/v1/records/{id}")
    suspend fun record(@Path("id") id: String): Envelope<RecordDto>

    @POST("/api/v1/records")
    suspend fun createRecord(
        @Header("Idempotency-Key") mutationId: String,
        @Body body: RecordWrite,
    ): Envelope<MutationResult>

    @PATCH("/api/v1/records/{id}")
    suspend fun patchRecord(
        @Path("id") id: String,
        @Body body: RecordWrite,
    ): Envelope<MutationResult>

    @retrofit2.http.DELETE("/api/v1/records/{id}")
    suspend fun deleteRecord(@Path("id") id: String): Envelope<MutationResult>

    @POST("/api/v1/sync/push")
    suspend fun syncPush(@Body body: SyncPushReq): Envelope<SyncPushRes>

    @GET("/api/v1/sync/pull")
    suspend fun syncPull(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = null,
    ): Envelope<SyncPullRes>

    @POST("/api/v1/media/sts")
    suspend fun mediaSts(@Body body: MediaStsReq): Envelope<MediaSts>

    @POST("/api/v1/media/complete")
    suspend fun mediaComplete(@Body body: Map<String, String>): Envelope<MediaObject>

    @GET("/api/v1/media/quota")
    suspend fun mediaQuota(): Envelope<MediaQuota>

    @GET("/api/v1/media/{id}/download-url")
    suspend fun mediaDownloadUrl(@Path("id") id: String): Envelope<MediaDownload>

    @retrofit2.http.DELETE("/api/v1/media/{id}")
    suspend fun mediaDelete(@Path("id") id: String): Envelope<Map<String, Boolean>>
}
