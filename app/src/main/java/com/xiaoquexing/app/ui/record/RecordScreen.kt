@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)

package com.xiaoquexing.app.ui.record

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiaoquexing.app.ui.components.AddContentRow
import com.xiaoquexing.app.ui.components.LinkCard
import com.xiaoquexing.app.ui.components.LocationCard
import com.xiaoquexing.app.ui.components.MusicCard
import com.xiaoquexing.app.ui.components.PhotoGrid
import com.xiaoquexing.app.ui.components.TagChip
import com.xiaoquexing.app.ui.components.VoiceRecorder
import com.xiaoquexing.app.viewmodel.RecordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    onPublished: () -> Unit,
    viewModel: RecordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAddMusicDialog by remember { mutableStateOf(false) }
    var showAddLinkDialog by remember { mutableStateOf(false) }
    var showAddLocationDialog by remember { mutableStateOf(false) }
    var musicTitleInput by remember { mutableStateOf("") }
    var musicArtistInput by remember { mutableStateOf("") }
    var linkUrlInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // GP Animation overlay
    if (uiState.showGpAnimation) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "+${uiState.earnedGp} GP",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("小确幸已记录 🌱", color = Color.White, fontSize = 18.sp)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("记录小确幸", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Mood tags
            Text(
                text = "今天的心情是？",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.moodTags.forEach { mood ->
                    TagChip(
                        text = mood.name,
                        selected = uiState.selectedMood == mood.name,
                        onClick = {
                            viewModel.selectMood(if (uiState.selectedMood == mood.name) null else mood.name)
                        },
                        leadingEmoji = mood.emoji,
                        selectedColor = Color(mood.color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Status tags
            Text(
                text = "添加标签",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.statusTags.forEach { tag ->
                    TagChip(
                        text = tag.name,
                        selected = uiState.selectedStatusTags.contains(tag.name),
                        onClick = { viewModel.toggleStatusTag(tag.name) },
                        leadingEmoji = tag.emoji
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Text input
            OutlinedTextField(
                value = uiState.text,
                onValueChange = { viewModel.updateText(it) },
                placeholder = { Text("今天发生了什么小确幸呢？") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                maxLines = 8
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Content previews
            // Photos
            if (uiState.photoUris.isNotEmpty()) {
                PhotoGrid(
                    photos = uiState.photoUris,
                    onRemove = { viewModel.removePhoto(it) },
                    showAddButton = false
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Voice
            if (uiState.voiceUri != null || uiState.isRecording) {
                VoiceRecorder(
                    isRecording = uiState.isRecording,
                    onStartRecording = { viewModel.setVoiceRecording(true) },
                    onStopRecording = {
                        viewModel.setVoiceRecording(false)
                        viewModel.setVoice("demo_voice_${System.currentTimeMillis()}", 15000L)
                    },
                    recordedDurationMs = uiState.voiceDuration,
                    hasRecording = uiState.voiceUri != null,
                    onDelete = { viewModel.removeVoice() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Music
            if (uiState.musicTitle != null) {
                MusicCard(
                    title = uiState.musicTitle!!,
                    artist = uiState.musicArtist ?: "未知歌手",
                    onRemove = { viewModel.removeMusic() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Link
            if (uiState.linkUrl != null) {
                LinkCard(
                    url = uiState.linkUrl!!,
                    title = uiState.linkTitle,
                    summary = uiState.linkSummary,
                    onRemove = { viewModel.removeLink() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Location
            if (uiState.locationName != null) {
                LocationCard(
                    locationName = uiState.locationName!!,
                    onRemove = { viewModel.removeLocation() }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Add content buttons
            AddContentRow(
                onPhotoClick = { viewModel.addPhoto("demo_photo_${System.currentTimeMillis()}") },
                onVoiceClick = { viewModel.setVoiceRecording(true) },
                onMusicClick = { showAddMusicDialog = true },
                onLinkClick = { showAddLinkDialog = true },
                onLocationClick = { showAddLocationDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Estimated GP
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "预计获得",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "+${uiState.estimatedGp} GP 🌱",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Publish button
            Button(
                onClick = { viewModel.publish(onPublished) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                enabled = !uiState.isPublishing
            ) {
                if (uiState.isPublishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "发布",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Add Music Dialog
    if (showAddMusicDialog) {
        AlertDialog(
            onDismissRequest = { showAddMusicDialog = false },
            title = { Text("添加音乐") },
            text = {
                Column {
                    OutlinedTextField(
                        value = musicTitleInput,
                        onValueChange = { musicTitleInput = it },
                        label = { Text("歌曲名称") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = musicArtistInput,
                        onValueChange = { musicArtistInput = it },
                        label = { Text("歌手") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (musicTitleInput.isNotBlank()) {
                        viewModel.setMusic(musicTitleInput, musicArtistInput.ifBlank { "未知歌手" })
                        musicTitleInput = ""
                        musicArtistInput = ""
                        showAddMusicDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMusicDialog = false }) { Text("取消") }
            }
        )
    }

    // Add Link Dialog
    if (showAddLinkDialog) {
        AlertDialog(
            onDismissRequest = { showAddLinkDialog = false },
            title = { Text("添加链接") },
            text = {
                OutlinedTextField(
                    value = linkUrlInput,
                    onValueChange = { linkUrlInput = it },
                    label = { Text("链接URL或标题") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (linkUrlInput.isNotBlank()) {
                        viewModel.setLink(linkUrlInput, linkUrlInput)
                        linkUrlInput = ""
                        showAddLinkDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLinkDialog = false }) { Text("取消") }
            }
        )
    }

    // Add Location Dialog
    if (showAddLocationDialog) {
        AlertDialog(
            onDismissRequest = { showAddLocationDialog = false },
            title = { Text("添加地点") },
            text = {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = { locationInput = it },
                    label = { Text("地点名称") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (locationInput.isNotBlank()) {
                        viewModel.setLocation(locationInput)
                        locationInput = ""
                        showAddLocationDialog = false
                    }
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showAddLocationDialog = false }) { Text("取消") }
            }
        )
    }
}
