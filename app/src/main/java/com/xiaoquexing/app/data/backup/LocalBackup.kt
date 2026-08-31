package com.xiaoquexing.app.data.backup

import android.content.Context
import android.net.Uri
import com.xiaoquexing.app.data.entity.Record
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@Serializable
data class BackupEnvelope(
    val version: Int = 1,
    val exportedAt: Long = 0,
    val records: List<BackupRecord> = emptyList(),
)

@Serializable
data class BackupRecord(
    val text: String = "",
    val moodTag: String? = null,
    val statusTags: String = "",
    val photos: List<String> = emptyList(),
    val voice: String? = null,
    val voiceDuration: Long = 0,
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val linkUrl: String? = null,
    val linkTitle: String? = null,
    val locationName: String? = null,
    val locationLat: Double? = null,
    val locationLng: Double? = null,
    val gpEarned: Int = 0,
    val createdAt: Long = 0,
    val isBackdated: Boolean = false,
)

object LocalBackup {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun fingerprint(text: String, createdAt: Long) = "$createdAt|${text.trim()}"

    fun shouldSkip(existing: Set<String>, incoming: BackupRecord): Boolean =
        fingerprint(incoming.text, incoming.createdAt) in existing

    fun pack(context: Context, records: List<Record>, dest: File): File {
        dest.parentFile?.mkdirs()
        val items = mutableListOf<BackupRecord>()
        ZipOutputStream(dest.outputStream()).use { zip ->
            records.forEachIndexed { index, rec ->
                val photos = rec.getPhotoUriList().mapIndexedNotNull { i, path ->
                    copyIntoZip(zip, File(path), "media/${index}_p$i${ext(path)}")
                }
                val voice = rec.voiceUri?.let { copyIntoZip(zip, File(it), "media/${index}_voice${ext(it)}") }
                items += BackupRecord(
                    text = rec.text,
                    moodTag = rec.moodTag,
                    statusTags = rec.statusTags,
                    photos = photos,
                    voice = voice,
                    voiceDuration = rec.voiceDuration,
                    musicTitle = rec.musicTitle,
                    musicArtist = rec.musicArtist,
                    linkUrl = rec.linkUrl,
                    linkTitle = rec.linkTitle,
                    locationName = rec.locationName,
                    locationLat = rec.locationLat,
                    locationLng = rec.locationLng,
                    gpEarned = rec.gpEarned,
                    createdAt = rec.createdAt,
                    isBackdated = rec.isBackdated,
                )
            }
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(json.encodeToString(BackupEnvelope.serializer(), BackupEnvelope(1, System.currentTimeMillis(), items)).toByteArray())
            zip.closeEntry()
        }
        return dest
    }

    fun unpack(context: Context, uri: Uri): Pair<BackupEnvelope, File> {
        val dir = File(context.cacheDir, "restore_${System.currentTimeMillis()}").apply { mkdirs() }
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val out = File(dir, entry.name)
                    out.parentFile?.mkdirs()
                    if (!entry.isDirectory) out.outputStream().use { zip.copyTo(it) }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("无法打开备份文件")
        val manifest = File(dir, "backup.json")
        if (!manifest.exists()) error("备份缺少 backup.json")
        val envelope = json.decodeFromString(BackupEnvelope.serializer(), manifest.readText())
        return envelope to dir
    }

    fun toDraft(row: BackupRecord, dir: File): Record {
        val photos = row.photos.map { File(dir, it).absolutePath }.joinToString("|")
        val voice = row.voice?.let { File(dir, it).absolutePath }
        return Record(
            text = row.text,
            moodTag = row.moodTag,
            statusTags = row.statusTags,
            photoUris = photos,
            voiceUri = voice,
            voiceDuration = row.voiceDuration,
            musicTitle = row.musicTitle,
            musicArtist = row.musicArtist,
            linkUrl = row.linkUrl,
            linkTitle = row.linkTitle,
            locationName = row.locationName,
            locationLat = row.locationLat,
            locationLng = row.locationLng,
            createdAt = row.createdAt,
            isBackdated = row.isBackdated,
        )
    }

    private fun copyIntoZip(zip: ZipOutputStream, file: File, name: String): String? {
        if (!file.exists() || !file.isFile) return null
        zip.putNextEntry(ZipEntry(name))
        file.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
        return name
    }

    private fun ext(path: String): String {
        val raw = path.substringAfterLast('.', "")
        return if (raw.length in 2..4) ".${raw.lowercase()}" else ""
    }
}
