package com.xiaoquexing.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TagChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    small: Boolean = false,
    selectedColor: Color = MaterialTheme.colorScheme.primary,
    leadingEmoji: String? = null
) {
    val hPad = if (small) 8.dp else 12.dp
    val vPad = if (small) 4.dp else 8.dp
    val fontSize = if (small) 12.sp else 14.sp
    val shape = RoundedCornerShape(if (small) 8.dp else 16.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(selectedColor)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .then(
                if (!selected) Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                    shape
                ) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = hPad, vertical = vPad)
    ) {
        val displayText = if (leadingEmoji != null) "$leadingEmoji $text" else text
        Text(
            text = displayText,
            fontSize = fontSize,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
