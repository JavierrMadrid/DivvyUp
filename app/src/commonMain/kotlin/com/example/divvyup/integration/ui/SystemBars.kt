package com.example.divvyup.integration.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.luminance

/**
 * Controla el color de los iconos de las barras del sistema (status/navigation).
 *
 * La app dibuja cabeceras verde oscuro bajo la status bar en varias pantallas y
 * permite override manual de tema, por lo que el color de iconos debe decidirse
 * por pantalla y no sólo por el tema del sistema.
 *
 * @param useDarkIcons `true` → iconos oscuros (fondos claros); `false` → iconos claros (fondos oscuros).
 */
@Composable
expect fun SetSystemBarAppearance(useDarkIcons: Boolean)

/**
 * Controla el color de los iconos de la barra de navegación inferior.
 *
 * Se decide por tema resuelto (no por pantalla) ya que el área inferior siempre
 * muestra el fondo de la app.
 */
@Composable
expect fun SetNavigationBarAppearance(useDarkIcons: Boolean)

/**
 * Aplica iconos de status bar acordes al tema resuelto. Para pantallas con
 * top bar claro (surface/background). Las cabeceras de gradiente oscuro deben
 * usar [SetSystemBarAppearance] con `useDarkIcons = false`.
 */
@Composable
fun ThemedSystemBarAppearance() {
    SetSystemBarAppearance(useDarkIcons = MaterialTheme.colorScheme.surface.luminance() >= 0.5f)
}
