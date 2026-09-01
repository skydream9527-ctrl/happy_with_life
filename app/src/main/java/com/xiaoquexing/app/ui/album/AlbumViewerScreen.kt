@file:OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.viewmodel.AlbumSnapshot
import com.xiaoquexing.app.viewmodel.AlbumViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumViewerScreen(
    albumId: Long,
    onBack: () -> Unit,
    viewModel: AlbumViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory()),
) {
    val ui by viewModel.uiState.collectAsState()
    LaunchedEffect(albumId) { viewModel.open(albumId) }
    val album = ui.snapshot
    val pages = listOf("cover", "growth", "mood", "tags", "location", "music", "links", "back")
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(album?.title ?: "我的画册", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.exportImage() },
                    enabled = !ui.exporting,
                ) { Text("长图") }
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.exportPdf() },
                    enabled = !ui.exporting,
                ) { Text("PDF") }
            }
        )
        ui.message?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val snap = album
                when (pages[page]) {
                    "cover" -> AlbumCoverPage(snap)
                    "growth" -> AlbumGrowthPage(snap)
                    "mood" -> AlbumMoodPage(snap)
                    "tags" -> AlbumTagsPage(snap)
                    "location" -> AlbumLocationPage(snap)
                    "music" -> AlbumMusicPage(snap)
                    "links" -> AlbumLinksPage(snap)
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
private fun AlbumCoverPage(album: AlbumSnapshot?) {
    AlbumPage(title = album?.title ?: "我的小确幸", emoji = "🌱") {
        Text(album?.dateRange ?: "暂无记录", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "${album?.recordCount ?: 0}条记录 · ${album?.totalGp ?: 0} GP",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(album?.plantLabel ?: "小确幸之树", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        album?.let { /* keep for recomposition */ }
    }
}

@Composable
private fun AlbumGrowthPage(album: AlbumSnapshot?) {
    AlbumPage(title = "成长时间轴", emoji = "📈") {
        Column {
            val rows = album?.days.orEmpty().ifEmpty { listOf("还没有记录" to 0) }
            rows.take(8).forEach { (text, gp) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text, style = MaterialTheme.typography.bodyMedium)
                    if (gp > 0) Text("+$gp GP", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AlbumMoodPage(album: AlbumSnapshot?) {
    AlbumPage(title = "心情合集", emoji = "😊") {
        val moods = album?.moods.orEmpty().ifEmpty { listOf("还没有心情" to 0) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            moods.take(8).forEach { (mood, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(mood, style = MaterialTheme.typography.bodyLarge)
                    if (count > 0) Text("$count 次", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AlbumTagsPage(album: AlbumSnapshot?) {
    AlbumPage(title = "标签精选", emoji = "🏷️") {
        val tags = album?.tags.orEmpty().ifEmpty { listOf("暂无" to 0) }
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
private fun AlbumLocationPage(album: AlbumSnapshot?) {
    AlbumPage(title = "足迹", emoji = "📍") {
        Column {
            val rows = album?.locations.orEmpty().ifEmpty { listOf("还没有地点" to 0) }
            rows.take(8).forEach { (loc, count) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(loc, style = MaterialTheme.typography.bodyLarge)
                    if (count > 0) Text("${count}次", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun AlbumMusicPage(album: AlbumSnapshot?) {
    AlbumPage(title = "BGM精选", emoji = "🎵") {
        Column {
            val rows = album?.music.orEmpty().ifEmpty { listOf("还没有音乐") }
            rows.take(8).forEach { music ->
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
private fun AlbumLinksPage(album: AlbumSnapshot?) {
    AlbumPage(title = "精选链接", emoji = "🔗") {
        Text(
            album?.links?.joinToString("\n")?.ifBlank { null } ?: "这本画册里还没有分享链接哦～",
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
