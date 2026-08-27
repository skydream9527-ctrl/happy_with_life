package com.xiaoquexing.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.data.entity.Achievement
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory())
) {
    val achievements by viewModel.achievements.collectAsState(initial = emptyList())
    val unlockedCount = achievements.count { it.isUnlocked }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("成就墙") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // 总进度
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🏆 成就进度",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "已解锁 $unlockedCount / ${achievements.size}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (achievements.isNotEmpty()) {
                        LinearProgressIndicator(
                            progress = { unlockedCount.toFloat() / achievements.size },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "我的成就",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementItem(achievement = achievement)
                }
            }
        }
    }
}

@Composable
fun AchievementItem(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked)
                MaterialTheme.colorScheme.surfaceVariant
            else Color(0xFFF5F5F5)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (achievement.isUnlocked) Color(0xFFFFF8E1)
                        else Color(0xFFE0E0E0)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (achievement.isUnlocked) {
                    Text(
                        text = achievement.emoji,
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF9E9E9E),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                color = if (achievement.isUnlocked)
                    MaterialTheme.colorScheme.onSurface
                else Color(0xFF9E9E9E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = achievement.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
            if (!achievement.isUnlocked && achievement.requirement > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                val progressFraction = (achievement.progress.toFloat() / achievement.requirement).coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Text(
                    text = "${achievement.progress}/${achievement.requirement}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF9E9E9E),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
