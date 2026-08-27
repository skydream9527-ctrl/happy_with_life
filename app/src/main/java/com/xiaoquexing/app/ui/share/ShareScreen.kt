package com.xiaoquexing.app.ui.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.di.rememberXiaoQueXingViewModelFactory
import com.xiaoquexing.app.ui.components.ShareCardCompose
import com.xiaoquexing.app.util.ShareCardRenderer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(
    recordId: Long,
    onBack: () -> Unit,
    viewModel: ShareViewModel = viewModel(factory = rememberXiaoQueXingViewModelFactory())
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() }

    LaunchedEffect(recordId) {
        // 没有真实 recordId（0 表示 Demo）就回退到 Demo 数据
        if (recordId > 0) viewModel.loadByRecordId(recordId) else viewModel.loadDemo()
    }

    LaunchedEffect(uiState.saveState) {
        when (val s = uiState.saveState) {
            is ShareSaveState.Success -> {
                snackbarHostState.showSnackbar("已保存到 ${s.path}")
                viewModel.resetSaveState()
            }
            ShareSaveState.Failed -> {
                snackbarHostState.showSnackbar("保存失败，请重试")
                viewModel.resetSaveState()
            }
            else -> Unit
        }
    }

    val demoData = uiState.cardData

    Scaffold(
        snackbarHost = { androidx.compose.material3.SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("分享") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "✨ 分享你的小确幸",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // 分享卡片预览
            ShareCardCompose(
                data = demoData ?: com.xiaoquexing.app.data.model.ShareCardData(),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .aspectRatio(1080f / 1920f)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 分享渠道
            Text(
                text = "分享到",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ShareChannelButton(
                    icon = "💬",
                    label = "微信",
                    color = Color(0xFF07C160)
                )
                ShareChannelButton(
                    icon = "🌏",
                    label = "朋友圈",
                    color = Color(0xFF07C160)
                )
                ShareChannelButton(
                    icon = "📕",
                    label = "小红书",
                    color = Color(0xFFFF2442)
                )
                ShareChannelButton(
                    icon = "🐦",
                    label = "微博",
                    color = Color(0xFFE6162D)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // 其他操作
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { /* 复制链接 */ },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("复制链接")
                }
                Button(
                    onClick = { viewModel.saveToGallery() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    enabled = uiState.saveState !is ShareSaveState.Saving
                ) {
                    if (uiState.saveState is ShareSaveState.Saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存图片")
                    }
                }
            }
        }
    }
}

@Composable
fun ShareChannelButton(
    icon: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .border(0.dp, Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 28.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
