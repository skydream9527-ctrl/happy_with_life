package com.xiaoquexing.app.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
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
    // iOS 风格胶囊 Chip：灰底圆角胶囊，选中变色
    val hPad = if (small) 10.dp else 14.dp
    val vPad = if (small) 4.dp else 7.dp
    val fontSize = if (small) 11.sp else 13.sp
    val shape = RoundedCornerShape(50)

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (selected) Modifier.background(selectedColor)
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = hPad, vertical = vPad)
    ) {
        val displayText = if (leadingEmoji != null) "$leadingEmoji $text" else text
        // 根据底色亮度自动选择黑/白文字，保证对比度
        val onSelectedColor = if (selected) {
            val lum = 0.299f * selectedColor.red + 0.587f * selectedColor.green + 0.114f * selectedColor.blue
            if (lum > 0.72f) Color(0xFF3A3226) else Color.White
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = displayText,
            fontSize = fontSize,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = onSelectedColor
        )
    }
}
