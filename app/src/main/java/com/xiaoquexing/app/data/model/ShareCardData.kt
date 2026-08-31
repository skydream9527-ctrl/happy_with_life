package com.xiaoquexing.app.data.model

import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.util.QrBitmaps

data class ShareCardData(
    val recordText: String = "",
    val moodEmoji: String = "",
    val dateStr: String = "",
    val plantType: PlantType = PlantType.TREE,
    val plantStage: Int = 0,
    val photoUris: List<String> = emptyList(),
    val musicTitle: String? = null,
    val musicArtist: String? = null,
    val totalGp: Int = 0,
    val footerText: String = "用小确幸记录生活",
    val qrPayload: String = QrBitmaps.APP_LINK,
)
