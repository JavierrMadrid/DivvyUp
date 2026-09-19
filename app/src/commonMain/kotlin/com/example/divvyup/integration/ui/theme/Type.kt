package com.example.divvyup.integration.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.divvyup.resources.Res
import com.example.divvyup.resources.PlusJakartaSans_Bold
import com.example.divvyup.resources.PlusJakartaSans_ExtraBold
import com.example.divvyup.resources.PlusJakartaSans_Medium
import com.example.divvyup.resources.PlusJakartaSans_Regular
import com.example.divvyup.resources.PlusJakartaSans_SemiBold
import org.jetbrains.compose.resources.Font

/**
 * Familia de marca de DivvyUp — Plus Jakarta Sans.
 *
 * Geométrica, contemporánea y con personalidad amable, encaja con la dirección
 * soft / rounded friendly. Se cargan 5 pesos (400–800) desde `composeResources/font`.
 */
@Composable
fun plusJakartaSans(): FontFamily = FontFamily(
    Font(Res.font.PlusJakartaSans_Regular, FontWeight.Normal),
    Font(Res.font.PlusJakartaSans_Medium, FontWeight.Medium),
    Font(Res.font.PlusJakartaSans_SemiBold, FontWeight.SemiBold),
    Font(Res.font.PlusJakartaSans_Bold, FontWeight.Bold),
    Font(Res.font.PlusJakartaSans_ExtraBold, FontWeight.ExtraBold),
)

/**
 * Escala tipográfica de DivvyUp sobre Plus Jakarta Sans.
 *
 * Máximo de 4 tamaños dominantes por pantalla, jerarquía por peso + tamaño.
 */
@Composable
fun divvyUpTypography(): Typography {
    val family = plusJakartaSans()
    return Typography(
        displayLarge   = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 40.sp, lineHeight = 48.sp, letterSpacing = (-1).sp),
        displayMedium  = TextStyle(fontFamily = family, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
        displaySmall   = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold,      fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.25).sp),
        headlineLarge  = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold,      fontSize = 28.sp, lineHeight = 36.sp, letterSpacing = (-0.2).sp),
        headlineMedium = TextStyle(fontFamily = family, fontWeight = FontWeight.Bold,      fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.1).sp),
        headlineSmall  = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold,  fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleLarge     = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold,  fontSize = 20.sp, lineHeight = 28.sp, letterSpacing = 0.sp),
        titleMedium    = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold,  fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.1.sp),
        titleSmall     = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodyLarge      = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal,    fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
        bodyMedium     = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal,    fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        bodySmall      = TextStyle(fontFamily = family, fontWeight = FontWeight.Normal,    fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp),
        labelLarge     = TextStyle(fontFamily = family, fontWeight = FontWeight.SemiBold,  fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        labelMedium    = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,    fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
        labelSmall     = TextStyle(fontFamily = family, fontWeight = FontWeight.Medium,    fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    )
}
