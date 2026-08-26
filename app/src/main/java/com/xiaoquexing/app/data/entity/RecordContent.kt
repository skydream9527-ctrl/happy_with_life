package com.xiaoquexing.app.data.entity

sealed class RecordContent {
    data class Text(val content: String) : RecordContent()
    data class Photo(val uris: List<String>) : RecordContent()
    data class Voice(val uri: String, val durationMs: Long) : RecordContent()
    data class Music(val title: String, val artist: String, val uri: String? = null) : RecordContent()
    data class Link(val url: String, val title: String? = null, val summary: String? = null) : RecordContent()
    data class Location(val name: String, val lat: Double? = null, val lng: Double? = null) : RecordContent()
}
