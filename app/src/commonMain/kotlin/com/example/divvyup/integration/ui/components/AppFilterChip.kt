package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.divvyup.integration.ui.theme.DivvyUpTokens

@Composable
fun AppFilterChip(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    modifier: Modifier = Modifier,
    unselectedColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxWidth: Dp = 180.dp,
    height: Dp = DivvyUpTokens.ChipSmallHeight,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(DivvyUpTokens.RadiusPill),
        color = if (selected) selectedColor else unselectedColor,
        modifier = modifier.height(height)
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .widthIn(max = maxWidth),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) Color.White else unselectedTextColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

