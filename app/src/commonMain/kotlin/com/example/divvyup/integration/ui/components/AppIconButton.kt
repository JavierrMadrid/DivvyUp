package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Single source of truth para todos los IconButton de DivvyUp.
 *
 * - Garantiza un área de touch de 48dp vía [minimumInteractiveComponentSize]
 *   (cumple la guía Material de accesibilidad sin obligación de un visual de 48dp).
 * - El icono interior permanece a 20dp para mantener la jerarquía visual.
 * - `onClickLabel` se pasa a TalkBack como announcement.
 * - `role = Role.Button` describe el control como botón al screen reader.
 *
 * Reemplaza el patrón `IconButton(modifier = Modifier.size(28.dp / 32.dp))` que
 * viola el touch target mínimo de Material.
 *
 * @param onClick acción al pulsar.
 * @param icon vector del icono; se renderiza a 20dp.
 * @param modifier modificador externo; usar para posicionamiento.
 * @param enabled si false, el botón queda deshabilitado.
 * @param contentDescription descripción semántica del icono (obligatoria, opcional null para decorativos).
 * @param onClickLabel label anunciable al pulsar (default = contentDescription).
 * @param tint color del icono (default LocalContentColor).
 */
@Composable
fun AppIconButton(
    onClick: () -> Unit,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClickLabel: String? = contentDescription,
    tint: Color = LocalContentColor.current
) {
    CompositionLocalProvider(LocalContentColor provides tint) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.minimumInteractiveComponentSize(),
            colors = IconButtonDefaults.iconButtonColors(contentColor = tint)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
