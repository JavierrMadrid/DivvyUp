package com.example.divvyup.integration.cache

import kotlin.time.Clock

/**
 * Caché en memoria con TTL (Time-To-Live) por entrada.
 * Pure Kotlin / KMP-compatible — sin dependencias de plataforma.
 *
 * Uso:
 *   val cache = InMemoryCache<String, List<Foo>>(ttlMillis = 60_000)
 *   val result = cache.getOrLoad("key") { repository.fetchFromNetwork() }
 *
 * La caché se invalida automáticamente cuando expira el TTL o al llamar a [invalidate].
 *
 * @param ttlMillis Tiempo de vida de cada entrada en milisegundos.
 */
class InMemoryCache<K, V>(private val ttlMillis: Long) {

    private data class Entry<V>(val value: V, val expiresAt: Long)

    private val store = mutableMapOf<K, Entry<V>>()

    /**
     * Devuelve el valor cacheado para [key] si existe y no ha expirado.
     * Si no existe o expiró, ejecuta [loader], almacena el resultado y lo devuelve.
     */
    suspend fun getOrLoad(key: K, loader: suspend () -> V): V {
        val now = Clock.System.now().toEpochMilliseconds()
        val entry = store[key]
        if (entry != null && now < entry.expiresAt) {
            return entry.value
        }
        val value = loader()
        store[key] = Entry(value, now + ttlMillis)
        return value
    }

    /** Invalida una clave concreta (e.g. tras una escritura). */
    fun invalidate(key: K) {
        store.remove(key)
    }

    /** Limpia toda la caché. */
    fun clear() {
        store.clear()
    }
}

