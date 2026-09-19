@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.example.divvyup.integration.supabase

import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.SupabaseClient
import kotlin.uuid.Uuid

/**
 * Servicio para subir imágenes de recibos/tickets al bucket "receipts" de Supabase Storage.
 * El bucket debe existir y tener las políticas RLS adecuadas.
 */
class SupabaseStorageService(private val client: SupabaseClient) {

    /**
     * Sube los bytes de una imagen al bucket "receipts" y devuelve la URL pública.
     * @param groupId ID del grupo (para organizar por carpeta)
     * @param imageBytes bytes de la imagen
     * @param mimeType tipo MIME (p. ej. "image/jpeg", "image/png")
     * @return URL pública de la imagen subida
     */
    suspend fun uploadReceiptImage(
        groupId: Long,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): String {
        val fileName = "${Uuid.random()}.jpg"
        val path = "groups/$groupId/$fileName"
        client.storage["receipts"].upload(path, imageBytes) {
            contentType = io.ktor.http.ContentType.parse(mimeType)
            upsert = false
        }
        return client.storage["receipts"].publicUrl(path)
    }

    /**
     * Sube los bytes de una imagen al bucket "avatars" y devuelve la URL pública.
     * @param userId ID del usuario
     * @param imageBytes bytes de la imagen
     * @param mimeType tipo MIME (p. ej. "image/jpeg", "image/png")
     * @return URL pública de la imagen subida
     */
    suspend fun uploadAvatarImage(
        userId: String,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): String {
        val fileName = "${Uuid.random()}.jpg"
        val path = "users/$userId/$fileName"
        client.storage["avatars"].upload(path, imageBytes) {
            contentType = io.ktor.http.ContentType.parse(mimeType)
            upsert = true
        }
        return client.storage["avatars"].publicUrl(path)
    }

    /**
     * Sube los bytes de una imagen al bucket "group-photos" y devuelve la URL pública.
     * @param groupId ID del grupo
     * @param imageBytes bytes de la imagen
     * @param mimeType tipo MIME (p. ej. "image/jpeg", "image/png")
     * @return URL pública de la imagen subida
     */
    suspend fun uploadGroupPhoto(
        groupId: Long,
        imageBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): String {
        val path = "groups/$groupId/cover.jpg"
        client.storage["group-photos"].upload(path, imageBytes) {
            contentType = io.ktor.http.ContentType.parse(mimeType)
            upsert = true
        }
        return client.storage["group-photos"].publicUrl(path)
    }
}
