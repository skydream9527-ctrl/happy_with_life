@file:OptIn(ExperimentalFoundationApi::class)

package com.xiaoquexing.app.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumViewerScreen(
    albumId: Long,
    onBack: () -> Unit
) {
    val pages = listOf("cover", "growth", "mood", "tags", "location", "music", "links", "back")
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text("我的画册", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (pages[page]) {
                    "cover" -> AlbumCoverPage()
                    "growth" -> AlbumGrowthPage()
                    "mood" -> AlbumMoodPage()
                    "tags" -> AlbumTagsPage()
                    "location" -> AlbumLocationPage()
                    "music" -> AlbumMusicPage()
                    "links" -> AlbumLinksPage()
                    "back" -> AlbumBackPage()
                }
            }

            // Page indicator
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                HorizontalPageIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                    modifier = Modifier,
                    activeColor = MaterialTheme.colorScheme.primary,
                    inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun AlbumPage(title: String, emoji: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            content()
        }
    }
}

@Composable
private fun AlbumCoverPage() {
    AlbumPage(title = "我的小确幸", emoji = "🌱") {
        Text("2024年1月", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "5条记录 · 116 GP",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text("小确幸之树 · 幼苗 🌳", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlbumGrowthPage() {
    AlbumPage(title = "成长时间轴", emoji = "📈") {
        Column {
            listOf(
                "第1天: 种子发芽了 🌱" to "+15 GP",
                "第2天: 小芽冒头 🌿" to "+20 GP",
                "第3天: 长出新叶 🍃" to "+28 GP",
                "第4天: 茁壮成长 🌳" to "+25 GP",
                "第5天: 越来越茂盛 🌲" to "+30 GP"
            ).forEach { (text, gp) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    Text(gp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AlbumMoodPage() {
    AlbumPage(title = "心情合集", emoji = "😊") {
        val moods = listOf("😊 开心" to 2, "😐 平静" to 1, "🤩 兴奋" to 1, "🥹 感动" to 1)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            moods.forEach { (mood, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mood, style = MaterialTheme.typography.bodyLarge)
                    Text("$count 次", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AlbumTagsPage() {
    AlbumPage(title = "标签精选", emoji = "🏷️") {
        val tags = listOf("自然" to 2, "美食" to 2, "居家" to 1, "聚会" to 1, "运动" to 1, "电影" to 1, "阅读" to 1)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(8.dp)
        ) {
            tags.forEach { (tag, count) ->
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(16.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("#$tag $count", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AlbumLocationPage() {
    AlbumPage(title = "足迹", emoji = "📍") {
        Column {
            listOf("🏠 家里" to "2次", "🌳 公园" to "1次", "🍜 餐厅" to "1次", "🏃 跑道" to "1次").forEach { (loc, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(loc, style = MaterialTheme.typography.bodyLarge)
                    Text(count, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AlbumMusicPage() {
    AlbumPage(title = "BGM精选", emoji = "🎵") {
        Column {
            listOf("🎵 晴天 - 周杰伦", "🎵 稻香 - 周杰伦", "🎵 小幸运 - 田馥甄").forEach { music ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF3E5F5))
                ) {
                    Text(music, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun AlbumLinksPage() {
    AlbumPage(title = "精选链接", emoji = "🔗") {
        Text(
            "这本画册里还没有分享链接哦～",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AlbumBackPage() {
    AlbumPage(title = "继续记录吧", emoji = "💚") {
        Text(
            "每一个小确幸，都是生活给你的礼物",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "用小确幸记录生活",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

// Add a HorizontalPageIndicator since Material3 doesn't have one built-in
@Composable
private fun HorizontalPageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.Gray,
    inactiveColor: Color = Color.LightGray
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { i ->
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .then(
                        if (i == currentPage) Modifier.padding(horizontal = 0.dp).fillMaxWidth(0.08f)
                        else Modifier.size(6.dp)
                    )
                    .background(
                        if (i == currentPage) activeColor else inactiveColor,
                        RoundedCornerShape(3.dp)
                    )
            )
        }
    }
}
