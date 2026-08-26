package com.xiaoquexing.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AddContentButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emoji: String? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (emoji != null) {
                Text(text = emoji, fontSize = 24.sp)
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.size(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun AddContentRow(
    onPhotoClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onMusicClick: () -> Unit,
    onLinkClick: () -> Unit,
    onLocationClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly
    ) {
        AddContentButton(
            icon = androidx.compose.material.icons.Icons.Default.Photo,
            label = "照片",
            emoji = "📷",
            onClick = onPhotoClick
        )
        AddContentButton(
            icon = androidx.compose.material.icons.Icons.Default.MusicNote,
            label = "语音",
            emoji = "🎤",
            onClick = onVoiceClick
        )
        AddContentButton(
            icon = androidx.compose.material.icons.Icons.Default.MusicNote,
            label = "音乐",
            emoji = "🎵",
            onClick = onMusicClick
        )
        AddContentButton(
            icon = androidx.compose.material.icons.Icons.Default.Place,
            label = "链接",
            emoji = "🔗",
            onClick = onLinkClick
        )
        AddContentButton(
            icon = androidx.compose.material.icons.Icons.Default.Place,
            label = "地点",
            emoji = "📍",
            onClick = onLocationClick
        )
    }
}
