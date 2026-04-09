package com.example.divvyup.integration.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ── Tokens verdes ─────────────────────────────────────────────────────────────
val JungleGreen        = Color(0xFF2D6A4F)
val JungleGreenDark    = Color(0xFF1B4332)
val JungleGreenMid     = Color(0xFF40916C)
val JungleGreenLight   = Color(0xFF74C69D)
val JungleGreen100     = Color(0xFFD8F3DC)
val JungleGreen50      = Color(0xFFEFF8F2)

// ── Tokens marrones ───────────────────────────────────────────────────────────
val BarkBrown          = Color(0xFF8B5E3C)
val BarkBrownDark      = Color(0xFF5C3D1E)
val BarkBrownLight     = Color(0xFFD4A57A)
val BarkBrown100       = Color(0xFFF5E6D3)
val Soil               = Color(0xFF6B4423)
val SandBeige          = Color(0xFFF5EFE6)

// ── Tokens dorados / luz solar ────────────────────────────────────────────────
val MossGold           = Color(0xFFD4A017)
val MossGold100        = Color(0xFFFAF0CC)
val Amber              = Color(0xFFE8B84B)

// ── Rojo unificado ────────────────────────────────────────────────────────────
// Un único token "AppRed" que se usa en toda la app (errores, deudas, barra menor)
val AppRed             = Color(0xFFD32F2F)   // rojo Material claro
val AppRedLight        = Color(0xFFEF5350)   // rojo claro para modo oscuro
val AppRedContainer    = Color(0xFFFADAD7)   // contenedor rojo pálido (modo claro)
val AppRedContainerDark= Color(0xFF7A1B10)   // contenedor rojo oscuro (modo oscuro)

// ── Texto / neutros ───────────────────────────────────────────────────────────
val ForestDark         = Color(0xFF1B2E1F)
val MossGrey           = Color(0xFF6B7C6E)
val ParchmentWhite     = Color(0xFFFAF8F2)
val White              = Color(0xFFFFFFFF)

// ── Tokens modo oscuro ────────────────────────────────────────────────────────
val DarkJungleBg       = Color(0xFF0D1F13)
val DarkJungleSurface  = Color(0xFF264734)

// ── Beige de texto oscuro: de blanco puro (primario) a beige claro (secundario) ──
// Nunca marrón, nunca verdoso. Escala: White → Beige100 → Beige200
val DarkTextBeige100   = Color(0xFFF0EBE3)   // beige muy claro — textos secundarios
val DarkTextBeige200   = Color(0xFFD9D0C4)   // beige suave — textos terciarios / hints
val DarkSurfaceNeutral = Color(0xFF2E2A26)   // fondo de burbujas/iconos: gris cálido sin verde
val DarkBorderNeutral  = Color(0xFF544E47)   // borde neutro cálido

internal val LightColors = lightColorScheme(
    primary                = JungleGreen,
    onPrimary              = White,
    primaryContainer       = JungleGreen100,
    onPrimaryContainer     = JungleGreenDark,

    secondary              = BarkBrown,
    onSecondary            = White,
    secondaryContainer     = BarkBrown100,
    onSecondaryContainer   = BarkBrownDark,

    tertiary               = MossGold,
    onTertiary             = ForestDark,
    tertiaryContainer      = MossGold100,
    onTertiaryContainer    = BarkBrownDark,

    error                  = AppRed,
    onError                = White,
    errorContainer         = AppRedContainer,
    onErrorContainer       = Color(0xFF5C0A00),

    background             = SandBeige,
    onBackground           = ForestDark,

    surface                = ParchmentWhite,
    onSurface              = ForestDark,
    surfaceVariant         = Color(0xFFEAF0EB),
    onSurfaceVariant       = MossGrey,
    surfaceContainerHigh   = Color(0xFFE0EBE2),
    surfaceContainerLow    = SandBeige,
    surfaceContainerHighest= Color(0xFFD6E6D8),

    outline                = Color(0xFFB0C4B3),
    outlineVariant         = Color(0xFFCCDDCE),
    inverseSurface         = JungleGreenDark,
    inverseOnSurface       = JungleGreen50,
    inversePrimary         = JungleGreenLight,
    scrim                  = Color(0x52000000),
)

internal val DarkColors = darkColorScheme(
    primary                = JungleGreenLight,
    onPrimary              = White,
    primaryContainer       = JungleGreenMid,
    onPrimaryContainer     = JungleGreen100,

    secondary              = BarkBrownLight,
    onSecondary            = White,
    secondaryContainer     = Soil,
    onSecondaryContainer   = BarkBrown100,

    tertiary               = Amber,
    onTertiary             = ForestDark,
    tertiaryContainer      = Color(0xFF4A3300),
    onTertiaryContainer    = MossGold100,

    error                  = AppRedLight,
    onError                = Color(0xFF4C0000),
    errorContainer         = AppRedContainerDark,
    onErrorContainer       = AppRedContainer,

    background             = DarkJungleBg,
    onBackground           = White,

    surface                = DarkJungleSurface,
    onSurface              = White,

    // burbujas/iconos: gris cálido neutro — sin verde, sin marrón
    surfaceVariant         = DarkSurfaceNeutral,
    // texto secundario: beige claro — escala blanco→beige, nunca marrón
    onSurfaceVariant       = DarkTextBeige100,
    // Contenedores de superficie: tonos neutros oscuros (sin marrón)
    surfaceContainerHigh   = Color(0xFF252525),
    surfaceContainerLow    = DarkJungleBg,
    surfaceContainerHighest= Color(0xFF2C2C2C),

    outline                = DarkBorderNeutral,
    outlineVariant         = Color(0xFF3D3730),
    inverseSurface         = Color(0xFFF5EFE6),
    inverseOnSurface       = ForestDark,
    inversePrimary         = JungleGreen,
    scrim                  = Color(0x52000000),
)
