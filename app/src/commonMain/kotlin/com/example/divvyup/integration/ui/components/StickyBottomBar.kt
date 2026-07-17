package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

/**
 * Barra inferior fija para CTAs primarios y secundarios.
 *
 * Reemplaza el patrón `Surface(8.dp) + Row { secondaryButton; primaryButton }`
 * que se repetía en CreateGroup, AddParticipants, AddSpend, SettleUp, etc.
 *
 * - `shadowElevation = 8.dp` para destacar sobre el contenido.
 * - Respeta el inset inferior del sistema (`navigationBarsPadding`).
 * - Padding interior generoso para CTAs cómodos al tacto.
 */
@Composable
fun StickyBottomBar(
    modifier: Modifier = Modifier,
    secondary: @Composable (() -> Unit)? = null,
    primary: @Composable RowScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = DivvyUpTokens.ScreenPaddingH, vertical = DivvyUpTokens.GapMd)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(DivvyUpTokens.PrimaryButtonHeight),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(DivvyUpTokens.GapMd)
            ) {
                if (secondary != null) {
                    secondary()
                }
                primary()
            }
        }
    }
}