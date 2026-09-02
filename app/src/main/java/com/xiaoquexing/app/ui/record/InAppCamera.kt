package com.xiaoquexing.app.ui.record

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.xiaoquexing.app.util.FileUtil

@Composable
fun InAppCamera(
    onCaptured: (path: String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(lifecycleOwner) {
        val future = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                provider.unbindAll()
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
            }.onFailure {
                Log.e("InAppCamera", "bind failed", it)
                error = "无法打开相机"
            }
        }, executor)
        onDispose {
            runCatching { future.get().unbindAll() }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
        ) {
            Icon(Icons.Default.Close, contentDescription = "关闭", tint = Color.White)
        }
        if (error != null) {
            TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.Center)) {
                Text(error ?: "", color = Color.White)
            }
        }
        Surface(
            onClick = {
                if (busy || error != null) return@Surface
                val dest = runCatching { FileUtil.createImageFile(context) }.getOrNull() ?: return@Surface
                busy = true
                imageCapture.takePicture(
                    ImageCapture.OutputFileOptions.Builder(dest).build(),
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            busy = false
                            if (dest.exists() && dest.length() > 0) onCaptured(dest.absolutePath) else onCancel()
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Log.e("InAppCamera", "capture failed", exception)
                            busy = false
                            error = "拍照失败"
                        }
                    },
                )
            },
            shape = CircleShape,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .size(72.dp),
        ) {}
    }
}
