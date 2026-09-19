package com.example.divvyup.integration.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
@Composable
actual fun rememberImagePickerLauncher(onImageSelected: (ByteArray?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.readBytes()
            }.getOrNull()
            onImageSelected(bytes)
        } else {
            onImageSelected(null)
        }
    }
    return { launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
}
