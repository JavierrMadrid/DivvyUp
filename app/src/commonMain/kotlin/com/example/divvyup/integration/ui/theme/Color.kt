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

// ── Semánticos: éxito / error / warning — light + dark ────────────────────────
// Éxito (verde menos saturado que JungleGreen — para badges/amounts positivos)
val SuccessGreen         = Color(0xFF16A34A)   // light: verde "le deben" — Tailwind green-600
val SuccessGreenDark     = Color(0xFF22C55E)   // dark: verde brillante para mantener contraste
val SuccessContainer     = Color(0xFF86EFAC)   // contenedor success pálido (light)
val SuccessContainerDark = Color(0xFF14532D)   // contenedor success oscuro

// Error (además del AppRed ya existente — para diferenciar "errores de sistema" vs "negativo financiero")
val ErrorRed             = Color(0xFFDC2626)   // light: rojo "debe dinero" — Tailwind red-600
val ErrorRedDark         = Color(0xFFFCA5A5)   // dark: rojo pálido para contraste
val ErrorContainerLight       = Color(0xFFFFE0E0)   // contenedor pálido (light)
val ErrorContainerLightDark   = Color(0xFF7F1D1D)   // contenedor oscuro (dark)
val ErrorOnContainerDark  = Color(0xFFB71C1C)   // texto sobre contenedor error en dark

// Warning (ámbar)
val WarningAmber         = Color(0xFFF59E0B)
val WarningAmberDark     = Color(0xFFFBBF24)
val WarningContainer     = Color(0xFFFFF3CD)
val WarningContainerDark = Color(0xFF78350F)
val WarningOnContainer   = Color(0xFF7C5200)

// Medallas / podio (gold, silver, bronze) — light + dark
val MedalGold            = Color(0xFFFFD700)
val MedalSilver          = Color(0xFFC0C0C0)
val MedalBronze          = Color(0xFFCD7F32)
val MedalGoldDark        = Color(0xFFFACC15)
val MedalSilverDark      = Color(0xFFD4D4D8)
val MedalBronzeDark      = Color(0xFFB45309)
val MedalGoldText        = Color(0xFF7A5700)
val MedalSilverText      = Color(0xFF4A4A4A)
val MedalBronzeText      = Color(0xFF5C3210)

// Tendencias (semánticas — verde-arriba / rojo-abajo, no literales)
val TrendUp              = Color(0xFF86EFAC)   // verde claro (light) — para "sube"
val TrendDown            = Color(0xFFFCA5A5)   // rojo claro (light) — para "baja"
val TrendUpDark          = Color(0xFF22C55E)
val TrendDownDark        = Color(0xFFF87171)

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

    // Fondo general más saturado para que las cards resalten
    background             = Color(0xFFDFEBE1),
    onBackground           = ForestDark,

    // Cards y superficies: blanco nítido sobre fondo verde medio
    surface                = Color(0xFFF7FAF7),
    onSurface              = ForestDark,

    // surfaceVariant: fondo distinguible del background (chips, inputs, etc.)
    surfaceVariant         = Color(0xFFCCDFCE),
    onSurfaceVariant       = Color(0xFF3D5440),

    // TopBars y contenedores destacados
    surfaceContainerHigh   = Color(0xFFCDD9CF),
    surfaceContainerLow    = Color(0xFFE8F0E9),
    surfaceContainerHighest= Color(0xFFBDCFC0),

    // Bordes más visibles para chips y campos
    outline                = Color(0xFF7A9E80),
    outlineVariant         = Color(0xFFA8C4AB),

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
