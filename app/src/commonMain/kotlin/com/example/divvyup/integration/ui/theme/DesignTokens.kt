package com.example.divvyup.integration.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Design tokens centralizados de DivvyUp.
 *
 * Todos los valores de tamaño, forma e icono compartidos por las pantallas
 * deben referenciarse desde aquí para mantener consistencia visual y
 * facilitar cambios globales.
 */
object DivvyUpTokens {

    // ── Alturas de controles interactivos ──────────────────────────────────────
    /** Altura estándar para campos de texto (OutlinedTextField compacto), dropdowns y botones de filtro. */
    val ControlHeight = 44.dp
    /** Altura del botón principal (CTA, bottom bar). */
    val PrimaryButtonHeight = 54.dp
    /** Altura de chips / pills (categorías, períodos, personas). */
    val ChipHeight = 36.dp
    /** Altura de chips pequeños (dialogs de borrado avanzado). */
    val ChipSmallHeight = 32.dp

    // ── Tamaños de avatares / íconos ───────────────────────────────────────────
    /** Avatar grande (tarjetas de grupo). */
    val AvatarLg = 52.dp
    /** Avatar mediano (tarjetas de gasto / participante). */
    val AvatarMd = 40.dp
    /** Avatar pequeño (filas de reparto en AddSpendScreen). */
    val AvatarSm = 36.dp
    /** Avatar mini (chips de personas en analíticas). */
    val AvatarXs = 22.dp

    /** Tamaño de icono estándar en botones y leading/trailing. */
    val IconSm = 18.dp
    /** Tamaño de icono mediano (FABs, action buttons). */
    val IconMd = 20.dp
    /** Tamaño de icono grande. */
    val IconLg = 24.dp
    /** Tamaño del ícono pequeño dentro de chips/badges. */
    val IconXs = 14.dp

    // ── Radios de esquinas ─────────────────────────────────────────────────────
    /** Esquinas para pill / chip. */
    val RadiusPill = 50.dp
    /** Esquinas para cards grandes. */
    val RadiusCard = 20.dp
    /** Esquinas para cards medianas / dialogs. */
    val RadiusCardMd = 16.dp
    /** Esquinas para controles (inputs, dropdowns, botones de filtro). */
    val RadiusControl = 12.dp
    /** Esquinas para cards de participante / fila. */
    val RadiusRow = 14.dp
    /** Esquinas para dialogs. */
    val RadiusDialog = 24.dp

    // ── Espaciados frecuentes ──────────────────────────────────────────────────
    /** Gap horizontal estándar entre controles en una fila. */
    val GapSm = 8.dp
    /** Gap entre secciones de formulario. */
    val GapMd = 12.dp
    /** Gap entre cards de lista. */
    val GapLg = 20.dp

    // ── Padding de pantalla ────────────────────────────────────────────────────
    /** Padding horizontal estándar de pantallas (LazyColumn, forms). */
    val ScreenPaddingH = 16.dp
    val ScreenPaddingHLg = 20.dp
}

