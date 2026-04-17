package com.example.divvyup.integration.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.example.divvyup.integration.ui.theme.DivvyUpTokens
import com.example.divvyup.integration.ui.theme.appOutlinedTextFieldColors
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun AppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        modifier = modifier.heightIn(min = DivvyUpTokens.ControlHeight),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = { onClear?.invoke() ?: onValueChange("") }) {
                    Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                }
            }
        },
        placeholder = {
            Text(
                text = placeholder,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        shape = RoundedCornerShape(DivvyUpTokens.RadiusControl),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = appOutlinedTextFieldColors()
    )
}

