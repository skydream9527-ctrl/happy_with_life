package com.xiaoquexing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.EmojiNature
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocalFlorist
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onNavigateToPlantSelection: () -> Unit,
    onNavigateToAchievement: () -> Unit,
    onNavigateToPlantGuide: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
    onNavigateToSpaces: () -> Unit = {},
    onNavigateToReview: () -> Unit = {},
    onNavigateToConflicts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 120.dp)
    ) {
        // Header with avatar and stats
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌱", fontSize = 40.sp)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.nickname,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(value = "${uiState.totalRecords}", label = "总记录")
                    StatItem(value = "${uiState.totalGp}", label = "总GP")
                    StatItem(value = "${uiState.streakDays}天", label = "连续记录")
                    StatItem(value = "${uiState.unlockedPlantCount}", label = "植物")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // Menu items
        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MenuItem(
                    icon = Icons.Default.LocalFlorist,
                    title = "我的植物",
                    subtitle = "选择和查看你的植物",
                    onClick = onNavigateToPlantSelection,
                    iconTint = Color(0xFF5E9B6A)
                )
                MenuItem(
                    icon = Icons.Default.EmojiEvents,
                    title = "成就墙",
                    subtitle = "${uiState.unlockedAchievementCount} 个成就已解锁",
                    onClick = onNavigateToAchievement,
                    iconTint = Color(0xFFF4A261)
                )
                MenuItem(
                    icon = Icons.Default.EmojiNature,
                    title = "植物图鉴",
                    subtitle = "了解每种植物的故事",
                    onClick = onNavigateToPlantGuide,
                    iconTint = Color(0xFF7AB886)
                )
                MenuItem(
                    icon = Icons.Default.Group,
                    title = "共享空间",
                    subtitle = "和朋友一起记录小确幸",
                    onClick = onNavigateToSpaces,
                    iconTint = Color(0xFF7EC8CE)
                )
                MenuItem(
                    icon = Icons.Default.DateRange,
                    title = "月年回顾",
                    subtitle = "本月热力与年度 GP",
                    onClick = onNavigateToReview,
                    iconTint = Color(0xFF5E9B6A)
                )
                MenuItem(
                    icon = Icons.Default.Settings,
                    title = "同步冲突",
                    subtitle = "本地和云端不一致时在这里选择",
                    onClick = onNavigateToConflicts,
                    iconTint = Color(0xFFE07A3D)
                )
                MenuItem(
                    icon = Icons.Default.Settings,
                    title = "登录与同步",
                    subtitle = "账号密码登录后把本地记录合并到云端",
                    onClick = onNavigateToLogin,
                    iconTint = Color(0xFF5E9B6A)
                )
                MenuItem(
                    icon = Icons.Default.Settings,
                    title = "设置",
                    subtitle = "通知、隐私、主题",
                    onClick = onNavigateToSettings,
                    iconTint = Color(0xFF9E9E9E)
                )
                MenuItem(
                    icon = Icons.Default.Share,
                    title = "分享App",
                    subtitle = "把小确幸分享给朋友",
                    onClick = { /* TODO */ },
                    iconTint = Color(0xFFE91E63),
                    showDivider = false
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "小确幸 v1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 72.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )
        }
    }
}
