package com.xiaoquexing.app.data.media

import com.xiaoquexing.app.data.db.AppDatabase
import com.xiaoquexing.app.data.remote.ApiService
import com.xiaoquexing.app.data.remote.MediaRef
import com.xiaoquexing.app.data.remote.MediaStsReq
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.concurrent.TimeUnit

class PhotoUploader(
    private val db: AppDatabase,
    private val api: ApiService,
    private val apiBase: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    suspend fun uploadForRecord(recordId: Long): List<MediaRef> = withContext(Dispatchers.IO) {
        val photos = db.mediaDao().readyPhotos(recordId)
        val refs = mutableListOf<MediaRef>()
        val now = System.currentTimeMillis()
        for (photo in photos) {
            photo.serverId?.let {
                refs += MediaRef(it, "PHOTO")
                continue
            }
            val path = photo.localPath ?: continue
            val file = File(path)
            if (!file.exists()) continue
            val packed = PhotoNormalize.compressExisting(file)
            val stsEnv = api.mediaSts(
                MediaStsReq(
                    type = "PHOTO",
                    mimeType = "image/jpeg",
                    sizeBytes = packed.bytes.size.toLong(),
                    width = packed.width,
                    height = packed.height,
                )
            )
            val sts = stsEnv.data ?: continue
            val url = resolveUrl(sts.uploadUrl)
            val put = Request.Builder().url(url).put(packed.bytes.toRequestBody("image/jpeg".toMediaType()))
            sts.headers.forEach { (k, v) -> put.header(k, v) }
            if (!sts.headers.keys.any { it.equals("Content-Type", true) }) {
                put.header("Content-Type", "image/jpeg")
            }
            val res = http.newCall(put.build()).execute()
            res.close()
            if (!res.isSuccessful) continue
            val done = api.mediaComplete(mapOf("mediaId" to sts.mediaId)).data
            if (done == null) continue
            db.mediaDao().bindServerId(photo.localId, sts.mediaId, now)
            refs += MediaRef(sts.mediaId, "PHOTO")
        }
        refs
    }

    private fun resolveUrl(raw: String): String {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
        val base = if (apiBase.endsWith("/")) apiBase.dropLast(1) else apiBase
        return if (raw.startsWith("/")) base + raw else "$base/$raw"
    }

    companion object {
        const val MAX_EDGE = PhotoNormalize.MAX_EDGE
        const val MAX_BYTES = PhotoNormalize.MAX_BYTES
    }
}
