package com.anvar.photolibraryorganizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun PanelActions(
    visible: Boolean,
    actionText: String?,
    secondaryActionText: String?,
    actionEnabled: Boolean = true,
    onActionClick: (() -> Unit)?,
    onSecondaryActionClick: (() -> Unit)?,
) {
    if (!visible || actionText == null || onActionClick == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onActionClick,
            modifier = Modifier.weight(1f),
            enabled = actionEnabled,
        ) {
            Text(actionText)
        }
        if (secondaryActionText != null && onSecondaryActionClick != null) {
            OutlinedButton(
                onClick = onSecondaryActionClick,
                modifier = Modifier.weight(1f),
            ) {
                Text(secondaryActionText)
            }
        }
    }
}
