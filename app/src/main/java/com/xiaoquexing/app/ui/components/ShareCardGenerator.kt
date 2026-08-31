package com.xiaoquexing.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiaoquexing.app.data.entity.PlantType
import com.xiaoquexing.app.data.model.MoodTag
import com.xiaoquexing.app.data.model.ShareCardData
import com.xiaoquexing.app.util.ShareCardRenderer

@Composable
fun ShareCardPreview(
    data: ShareCardData,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(data) {
        ShareCardRenderer.render(data, width = 720, context = context)
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "分享卡片",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ShareCardCompose(
    data: ShareCardData,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.714f)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFE8F5E9), Color(0xFFC8E6C9))
                )
            )
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = data.dateStr,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575),
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = data.plantType.emoji,
                    fontSize = 32.sp,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Mood emoji
            if (data.moodEmoji.isNotEmpty()) {
                Text(text = data.moodEmoji, fontSize = 48.sp)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Text content
            if (data.recordText.isNotEmpty()) {
                Text(
                    text = if (data.recordText.length > 120) data.recordText.take(117) + "..." else data.recordText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF212121),
                    lineHeight = 24.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Photo placeholder
            if (data.photoUris.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "📷 ${data.photoUris.size} 张照片",
                        color = Color(0xFF757575)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Music bar
            if (data.musicTitle != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF3E5F5))
                        .padding(12.dp)
                ) {
                    Text(text = "🎵 ${data.musicTitle} - ${data.musicArtist ?: ""}", color = Color(0xFF212121))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            // GP
            Text(
                text = "🌱 ${data.totalGp} GP",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = data.footerText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF4CAF50)
                )
            }
        }
    }
}
