package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.Strings
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.resources.Res
import com.example.divvyup.resources.divvyup_logo
import org.jetbrains.compose.resources.painterResource

/**
 * Cabecera de marca para pantallas de autenticación.
 *
 * Muestra el logo real de DivvyUp dentro de un contenedor redondeado con sombra
 * suave, seguido del nombre de la app y un subtítulo. Fuente única para
 * Login y Register.
 */
@Composable
fun AppBrandHeader(
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DivvyUpTokens.GapSm)
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .shadow(
                    elevation = DivvyUpTokens.ElevationRaised,
                    shape = RoundedCornerShape(DivvyUpTokens.RadiusCard),
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.26f)
                )
                .clip(RoundedCornerShape(DivvyUpTokens.RadiusCard))
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.divvyup_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp)
            )
        }
        Text(
            Strings.Auth.APP_NAME,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = DivvyUpTokens.GapXs)
        )
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
