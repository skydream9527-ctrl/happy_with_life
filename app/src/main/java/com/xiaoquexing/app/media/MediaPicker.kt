package com.xiaoquexing.app.media

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.xiaoquexing.app.data.media.VoiceFiles
import com.xiaoquexing.app.util.FileUtil
import com.xiaoquexing.app.util.findComponentActivity
import java.io.File

/**
 * 媒体采集门面。
 *
 * 为什么不直接写在 Composable 里：
 * 1) ActivityResultLauncher 必须按生命周期管理（不能在 Composable 里随便 new）。
 * 2) 录音需要持有 MediaRecorder 引用，并在 onStop 时显式释放。
 * 3) Composable 拿不到 application Context，但能用 LocalContext 拿 Activity。
 *
 * 用法：
 *   val picker = rememberMediaPicker(activity)
 *   Button(onClick = { picker.pickPhoto { uri -> ... } })
 *
 * 注意：pickPhoto 内部会启动系统 PhotoPicker（minSdk=26 上 ActivityResultContracts.PickVisualMedia
 * 自动降级到 ACTION_OPEN_DOCUMENT，零运行时依赖）。
 */
class MediaPicker(private val activity: ComponentActivity) {

    private var pendingPhotoCallback: ((String?) -> Unit)? = null
    private var pendingMultiPhotoCallback: ((List<String>) -> Unit)? = null
    private var pendingCameraCallback: ((String?) -> Unit)? = null
    private var pendingAudioPermissionCallback: ((Boolean) -> Unit)? = null
    private var pendingCameraPermissionCallback: ((Boolean) -> Unit)? = null

    // 录音状态
    private var recorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var recordingStartedAt: Long = 0
    private var recordStoppedCallback: ((String?, Long) -> Unit)? = null

    // Launchers 必须由 Compose rememberLauncherForActivityResult 在组合时创建。
    // Activity 已 RESUMED 后再 registerForActivityResult 会直接闪退。
    var pickSinglePhotoLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    var pickMultiplePhotoLauncher: ActivityResultLauncher<PickVisualMediaRequest>? = null
    var takePictureLauncher: ActivityResultLauncher<Uri>? = null
    var takePreviewLauncher: ActivityResultLauncher<Void?>? = null
    var requestAudioPermLauncher: ActivityResultLauncher<String>? = null
    var requestCameraPermLauncher: ActivityResultLauncher<String>? = null

    fun onPickedSingle(uri: Uri?) {
        val cb = pendingPhotoCallback
        pendingPhotoCallback = null
        cb?.invoke(uri?.toString())
    }

    fun onPickedMultiple(uris: List<Uri>) {
        val cb = pendingMultiPhotoCallback
        pendingMultiPhotoCallback = null
        cb?.invoke(uris.map { it.toString() })
    }

    fun onTakenPicture(success: Boolean) {
        val cb = pendingCameraCallback
        val path = pendingCameraSavedPath
        val uri = pendingCameraUri
        pendingCameraUri = null
        pendingCameraSavedPath = null
        pendingCameraCallback = null
        if (!success) {
            uri?.let { runCatching { activity.contentResolver.delete(it, null, null) } }
            cb?.invoke(null)
            return
        }
        val fromFile = path?.let { File(it) }?.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        val copied = fromFile ?: uri?.let { copyUriToPrivate(it) }
        cb?.invoke(copied)
    }

    fun onPreviewTaken(bitmap: Bitmap?) {
        val cb = pendingCameraCallback
        pendingCameraCallback = null
        if (bitmap == null) {
            cb?.invoke(null)
            return
        }
        val dest = runCatching { FileUtil.createImageFile(activity) }.getOrNull()
        if (dest == null) {
            cb?.invoke(null)
            return
        }
        val ok = runCatching {
            dest.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            dest.length() > 0
        }.getOrDefault(false)
        cb?.invoke(if (ok) dest.absolutePath else null)
    }

    fun onAudioPermission(granted: Boolean) {
        val cb = pendingAudioPermissionCallback
        pendingAudioPermissionCallback = null
        cb?.invoke(granted)
    }

    fun onCameraPermission(granted: Boolean) {
        val cb = pendingCameraPermissionCallback
        pendingCameraPermissionCallback = null
        cb?.invoke(granted)
    }

    // ---- 公共 API ----

    /** 从相册单选。callback 收到的是可被 Coil 直接加载的 content:// uri 字符串。 */
    fun pickPhoto(callback: (uri: String?) -> Unit) {
        pendingPhotoCallback = callback
        pickSinglePhotoLauncher?.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        ) ?: callback(null)
    }

    /** 从相册多选（最多 9 张）。 */
    fun pickPhotos(callback: (uris: List<String>) -> Unit) {
        pendingMultiPhotoCallback = callback
        pickMultiplePhotoLauncher?.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        ) ?: callback(emptyList())
    }

    /**
     * 调起相机拍照，保存到应用专属目录，返回绝对路径。
     * 没有 CAMERA 权限时会先请求权限。
     */
    fun takePhoto(callback: (path: String?) -> Unit) {
        if (!hasPermission(Manifest.permission.CAMERA)) {
            pendingCameraPermissionCallback = { granted ->
                if (granted) launchCameraInternal(callback) else callback(null)
            }
            requestCameraPermLauncher?.launch(Manifest.permission.CAMERA) ?: callback(null)
            return
        }
        launchCameraInternal(callback)
    }

    private var pendingCameraUri: Uri? = null
    private var pendingCameraSavedPath: String? = null

    private fun launchCameraInternal(callback: (path: String?) -> Unit) {
        pendingCameraCallback = callback
        val probe = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val hasCameraApp = probe.resolveActivity(activity.packageManager) != null
        if (!hasCameraApp) {
            takePreviewLauncher?.launch(null) ?: callback(null)
            return
        }
        try {
            val (uri, path) = createCaptureTarget()
            pendingCameraUri = uri
            pendingCameraSavedPath = path
            grantCameraUri(activity, uri)
            val launched = runCatching { takePictureLauncher?.launch(uri) }.getOrNull() != null && takePictureLauncher != null
            if (!launched) {
                takePreviewLauncher?.launch(null) ?: callback(null)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "takePhoto failed", t)
            runCatching { takePreviewLauncher?.launch(null) }.onFailure { callback(null) }
        }
    }

    private fun createCaptureTarget(): Pair<Uri, String?> {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "xqx_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/XiaoQueXing")
            }
        }
        val media = runCatching {
            activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        }.getOrNull()
        if (media != null) return media to null
        val file = FileUtil.createImageFile(activity)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file,
        )
        return uri to file.absolutePath
    }

    private fun copyUriToPrivate(uri: Uri): String? = runCatching {
        val dest = FileUtil.createImageFile(activity)
        activity.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
        dest.takeIf { it.exists() && it.length() > 0 }?.absolutePath
    }.getOrNull()

    /**
     * 开始录音。返回是否成功（false 通常是权限被拒）。
     * 调 [stopRecording] 拿到文件路径和时长。
     */
    fun startRecording(): Boolean {
        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            // 由 UI 层先请求权限，简化处理：这里返回 false，UI 走 requestAudioPermission
            return false
        }
        return try {
            val file = FileUtil.createAudioFile(activity)
            val rec = newMediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96_000)
                setAudioSamplingRate(44_100)
                setMaxDuration(VoiceFiles.MAX_DURATION_MS.toInt())
                setOutputFile(file.absolutePath)
                setOnInfoListener { _, what, _ ->
                    if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                        stopRecording()
                    }
                }
                prepare()
                start()
            }
            recorder = rec
            currentAudioFile = file
            recordingStartedAt = System.currentTimeMillis()
            true
        } catch (t: Throwable) {
            Log.e(TAG, "startRecording failed", t)
            releaseRecorder()
            false
        }
    }

    fun stopRecording() {
        val rec = recorder ?: return
        val file = currentAudioFile
        val duration = System.currentTimeMillis() - recordingStartedAt
        try {
            rec.stop()
        } catch (_: Throwable) {
            // start() 之后很快就 stop() 会抛 RuntimeException，这里吞掉按 0 时长处理
        } finally {
            releaseRecorder()
            currentAudioFile = null
            recorder = null
        }
        val accepted = VoiceFiles.accept(file?.absolutePath, duration)
        if (accepted.ok) {
            recordStoppedCallback?.invoke(accepted.path, accepted.durationMs)
        } else {
            recordStoppedCallback?.invoke(null, 0)
        }
    }

    fun cancelRecording() {
        try {
            recorder?.stop()
        } catch (_: Throwable) { }
        releaseRecorder()
        currentAudioFile?.delete()
        currentAudioFile = null
        recorder = null
    }

    fun isRecording(): Boolean = recorder != null

    /** 请求录音权限，结果通过 callback 回调。 */
    fun requestAudioPermission(callback: (Boolean) -> Unit) {
        if (hasPermission(Manifest.permission.RECORD_AUDIO)) {
            callback(true)
            return
        }
        pendingAudioPermissionCallback = callback
        requestAudioPermLauncher?.launch(Manifest.permission.RECORD_AUDIO) ?: callback(false)
    }

    fun setOnRecordStoppedCallback(callback: (path: String?, durationMs: Long) -> Unit) {
        recordStoppedCallback = callback
    }

    // ---- 工具 ----

    fun hasPermission(name: String): Boolean =
        ContextCompat.checkSelfPermission(activity, name) == PackageManager.PERMISSION_GRANTED

    private fun releaseRecorder() {
        try { recorder?.reset() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
    }

    @SuppressLint("NewApi") // API 31 才有的 new MediaRecorder(Context) 走版本兼容
    private fun newMediaRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(activity)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

    /**
     * 在 Activity 销毁时调用。Launchers 跟 Activity 生命周期绑，
     * 正常 Compose rotation 重建 Activity 时 registerForActivityResult 不会泄漏。
     */
    fun onDestroy() {
        // 旋转会重建 Activity：停录并保留文件，不要 cancel 删掉
        if (isRecording()) stopRecording() else releaseRecorder()
    }

    companion object {
        private const val TAG = "MediaPicker"
    }
}

/**
 * 工厂：在 Composable 里 `val picker = rememberMediaPicker()` 拿一个跟当前 Activity 绑定的实例。
 * Activity 重建时 Compose 会自动重建 launcher，URL 缓存通过 picker 的成员字段保留。
 */
@Composable
fun rememberMediaPicker(): MediaPicker {
    val context = LocalContext.current
    val activity = context.findComponentActivity()
        ?: error("MediaPicker 必须在 ComponentActivity 内使用")
    val picker = remember(activity) { MediaPicker(activity) }
    picker.pickSinglePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { picker.onPickedSingle(it) }
    picker.pickMultiplePhotoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9),
    ) { picker.onPickedMultiple(it) }
    picker.takePictureLauncher = rememberLauncherForActivityResult(
        CapturePictureContract(),
    ) { picker.onTakenPicture(it) }
    picker.takePreviewLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview(),
    ) { picker.onPreviewTaken(it) }
    picker.requestAudioPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { picker.onAudioPermission(it) }
    picker.requestCameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { picker.onCameraPermission(it) }
    DisposableEffect(picker) {
        onDispose { picker.onDestroy() }
    }
    return picker
}

/** 取应用专属文件目录的 helper，方便在 Composable 里取 photo/audio 路径。 */
private fun grantCameraUri(context: Context, uri: Uri) {
    val flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
    val probe = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
    context.packageManager.queryIntentActivities(probe, PackageManager.MATCH_DEFAULT_ONLY).forEach { info ->
        runCatching {
            context.grantUriPermission(info.activityInfo.packageName, uri, flags)
        }
    }
}

class CapturePictureContract : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return super.createIntent(context, input).apply {
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = android.content.ClipData.newUri(context.contentResolver, "photo", input)
        }
    }
}

fun Context.mediaCacheDir(type: String): File {
    val base = getExternalFilesDir(type) ?: filesDir
    if (!base.exists()) base.mkdirs()
    return base
}
