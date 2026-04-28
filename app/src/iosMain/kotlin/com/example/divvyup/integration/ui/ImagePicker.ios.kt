package com.example.divvyup.integration.ui
import androidx.compose.runtime.Composable
@Composable
actual fun rememberImagePickerLauncher(onImageSelected: (ByteArray?) -> Unit): () -> Unit {
    // TODO: implementar selector de imagenes para iOS
    return {}
}
