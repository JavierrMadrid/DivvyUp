package com.example.divvyup.integration.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Tokens de movimiento de DivvyUp.
 *
 * Un único origen para duraciones y curvas evita animaciones dispares entre
 * pantallas. Las curvas siguen la escala "emphasized" de Material.
 */
object DivvyUpMotion {
    /** Cambios de estado diminutos (color de chip, iconos). */
    const val Short = 150
    /** Micro-interacciones y transiciones pequeñas. */
    const val Medium = 250
    /** Transiciones de navegación y entradas de contenido. */
    const val Long = 400
    /** Celebraciones / entradas hero. */
    const val ExtraLong = 700

    /** Curva estándar para la mayoría de transiciones. */
    val Standard: Easing = FastOutSlowInEasing
    /** Entrada con desaceleración marcada — contenido que aparece. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    /** Salida con aceleración marcada — contenido que desaparece. */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    /** Curva enfática completa para nav y héroes. */
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}
