package com.example.divvyup.integration.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Paleta "Soft Jungle" — dirección soft / rounded friendly.
// Verde como ancla de marca, acentos coral y lavanda, fondos mint suaves.
// ─────────────────────────────────────────────────────────────────────────────

// ── Tokens verdes (marca) ─────────────────────────────────────────────────────
val JungleGreen        = Color(0xFF188653)   // primary — verde fresco (contraste AA con blanco)
val JungleGreenDark    = Color(0xFF125C39)   // onPrimaryContainer / degradados
val JungleGreenMid     = Color(0xFF3FAE79)   // verde medio
val JungleGreenLight   = Color(0xFF7FD9A6)   // primary en modo oscuro
val JungleGreen100     = Color(0xFFD6F3E4)   // primaryContainer
val JungleGreen50      = Color(0xFFEDF9F2)   // fondos muy suaves

// ── Tokens coral (secundario / acento cálido) ─────────────────────────────────
val Coral              = Color(0xFFFF8A65)
val CoralDark          = Color(0xFFAD3809)   // onSecondaryContainer (contraste AA)
val CoralLight         = Color(0xFFFFB59B)
val CoralContainer     = Color(0xFFFFE0D6)
val Coral50            = Color(0xFFFFF1EB)

// ── Tokens lavanda (terciario / acento frío) ──────────────────────────────────
val Lavender           = Color(0xFF7C6BFF)
val LavenderDark       = Color(0xFF3B2FA8)
val LavenderLight      = Color(0xFFB3A8FF)
val LavenderContainer  = Color(0xFFE7E3FF)
val Lavender50         = Color(0xFFF1EFFF)

// ── Tokens marrones / tierra (paleta de avatares y detalles cálidos) ──────────
val BarkBrown          = Color(0xFFA9704C)
val BarkBrownDark      = Color(0xFF6B4226)
val BarkBrownLight     = Color(0xFFD4A57A)
val BarkBrown100       = Color(0xFFF5E6D3)
val Soil               = Color(0xFF7C5230)
val SandBeige          = Color(0xFFF5EFE6)

// ── Tokens dorados / luz solar ────────────────────────────────────────────────
val MossGold           = Color(0xFFE8A33D)
val MossGold100        = Color(0xFFFAF0CC)
val Amber              = Color(0xFFF0B94B)

// ── Rojo unificado ────────────────────────────────────────────────────────────
val AppRed             = Color(0xFFE5484D)   // rojo principal (errores, deudas)
val AppRedLight        = Color(0xFFF27A7E)   // rojo claro para modo oscuro
val AppRedContainer    = Color(0xFFFCE0E1)   // contenedor rojo pálido (claro)
val AppRedContainerDark= Color(0xFF7A1F22)   // contenedor rojo oscuro

// ── Texto / neutros ───────────────────────────────────────────────────────────
val ForestDark         = Color(0xFF17251C)
val MossGrey           = Color(0xFF6E7F72)
val ParchmentWhite     = Color(0xFFFAFCFA)
val White              = Color(0xFFFFFFFF)

// ── Tokens modo oscuro ────────────────────────────────────────────────────────
val DarkJungleBg       = Color(0xFF0F1A13)
val DarkJungleSurface  = Color(0xFF1B2A20)

// ── Tokens específicos para chips / pills en tema claro ────────────────────────
val FilterChipUnselectedLight = Color(0xFFCFE0D4)
val FilterChipUnselectedTextLight = ForestDark

// ── Beige de texto oscuro: de blanco puro a verde-gris muy claro ──────────────
val DarkTextBeige100   = Color(0xFFE8F0EA)   // textos secundarios
val DarkTextBeige200   = Color(0xFFBFCEC4)   // textos terciarios / hints
val DarkSurfaceNeutral = Color(0xFF2A342D)   // fondo de burbujas/iconos
val DarkBorderNeutral  = Color(0xFF46554A)   // borde neutro

// ── Semánticos: éxito / error / warning — light + dark ────────────────────────
val SuccessGreen         = Color(0xFF22A05E)
val SuccessGreenDark     = Color(0xFF4CC97F)
val SuccessContainer     = Color(0xFFB9EFCB)
val SuccessContainerDark = Color(0xFF14532D)

val ErrorRed             = Color(0xFFE5484D)
val ErrorRedDark         = Color(0xFFFF9DA1)
val ErrorContainerLight       = Color(0xFFFFE4E5)
val ErrorContainerLightDark   = Color(0xFF7A1F22)
val ErrorOnContainerDark      = Color(0xFFB3202A)

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

// Tendencias (semánticas — verde-arriba / rojo-abajo)
val TrendUp              = Color(0xFF8FE0B4)
val TrendDown            = Color(0xFFFFB0B3)
val TrendUpDark          = Color(0xFF4CC97F)
val TrendDownDark        = Color(0xFFFF7B80)

internal val LightColors = lightColorScheme(
    primary                = JungleGreen,
    onPrimary              = White,
    primaryContainer       = JungleGreen100,
    onPrimaryContainer     = JungleGreenDark,

    secondary              = Coral,
    onSecondary            = White,
    secondaryContainer     = CoralContainer,
    onSecondaryContainer   = CoralDark,

    tertiary               = Lavender,
    onTertiary             = White,
    tertiaryContainer      = LavenderContainer,
    onTertiaryContainer    = LavenderDark,

    error                  = AppRed,
    onError                = White,
    errorContainer         = AppRedContainer,
    onErrorContainer       = Color(0xFF5C0A0E),

    // Fondo mint suave para que las cards blancas resalten
    background             = Color(0xFFF3F7F2),
    onBackground           = ForestDark,

    // Cards y superficies: blanco nítido sobre fondo mint
    surface                = Color(0xFFFFFFFF),
    onSurface              = ForestDark,

    // surfaceVariant: chips, inputs, etc.
    surfaceVariant         = Color(0xFFE6EEE7),
    onSurfaceVariant       = Color(0xFF43564A),

    // TopBars y contenedores destacados
    surfaceContainerHigh   = Color(0xFFDCE8DE),
    surfaceContainerLow    = Color(0xFFF0F6F1),
    surfaceContainerHighest= Color(0xFFCFDFD3),

    // Bordes más visibles para chips y campos
    outline                = Color(0xFFA9C1AF),
    outlineVariant         = Color(0xFFD2E2D6),

    inverseSurface         = JungleGreenDark,
    inverseOnSurface       = JungleGreen50,
    inversePrimary         = JungleGreenLight,
    scrim                  = Color(0x52000000),
)

internal val DarkColors = darkColorScheme(
    primary                = JungleGreenLight,
    onPrimary              = Color(0xFF06301C),
    primaryContainer       = Color(0xFF175436),
    onPrimaryContainer     = JungleGreen100,

    secondary              = CoralLight,
    onSecondary            = Color(0xFF4A1A0A),
    secondaryContainer     = Color(0xFF7A3A22),
    onSecondaryContainer   = Coral50,

    tertiary               = LavenderLight,
    onTertiary             = Color(0xFF241E5C),
    tertiaryContainer      = LavenderDark,
    onTertiaryContainer    = Lavender50,

    error                  = ErrorRedDark,
    onError                = Color(0xFF4C0000),
    errorContainer         = AppRedContainerDark,
    onErrorContainer       = AppRedContainer,

    background             = DarkJungleBg,
    onBackground           = White,

    surface                = DarkJungleSurface,
    onSurface              = White,

    // burbujas/iconos: verde-gris neutro
    surfaceVariant         = DarkSurfaceNeutral,
    // texto secundario: verde-gris claro
    onSurfaceVariant       = DarkTextBeige100,
    // Contenedores de superficie
    surfaceContainerHigh   = Color(0xFF25312A),
    surfaceContainerLow    = Color(0xFF14201A),
    surfaceContainerHighest= Color(0xFF2E3A32),

    outline                = DarkBorderNeutral,
    outlineVariant         = Color(0xFF3A4A3E),
    inverseSurface         = Color(0xFFF0F6F1),
    inverseOnSurface       = ForestDark,
    inversePrimary         = JungleGreen,
    scrim                  = Color(0x52000000),
)
