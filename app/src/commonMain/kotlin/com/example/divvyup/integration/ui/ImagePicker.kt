package com.example.divvyup.integration.ui

import androidx.compose.runtime.Composable

/**
 * Devuelve una función lambda que, al invocarse, abre el selector de imágenes del sistema.
 * [onImageSelected] recibe los bytes de la imagen seleccionada, o null si se canceló.
 */
@Composable
expect fun rememberImagePickerLauncher(onImageSelected: (ByteArray?) -> Unit): () -> Unit

