package app.aemu.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R

/** Material 3 bottom sheet for the Hub-only preferences and layout choice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubSettingsSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val settings by HubSettings.state.collectAsState()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.hub_settings_title), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))

            ToggleRow(
                stringResource(R.string.hub_auto_import),
                stringResource(R.string.hub_auto_import_sub),
                settings.autoImport,
            ) { HubSettings.setAutoImport(ctx, it) }
            ToggleRow(
                stringResource(R.string.hub_wifi_only),
                stringResource(R.string.hub_wifi_only_sub),
                settings.wifiOnly,
            ) { HubSettings.setWifiOnly(ctx, it) }
            ToggleRow(
                stringResource(R.string.hub_delete_after),
                stringResource(R.string.hub_delete_after_sub),
                settings.deleteAfterImport,
            ) { HubSettings.setDeleteAfterImport(ctx, it) }
            ToggleRow(
                stringResource(R.string.hub_show_unverified),
                stringResource(R.string.hub_show_unverified_sub),
                settings.showUnverified,
            ) { HubSettings.setShowUnverified(ctx, it) }
            ToggleRow(
                stringResource(R.string.hub_reverify),
                stringResource(R.string.hub_reverify_sub),
                settings.reverifyBeforeImport,
            ) { HubSettings.setReverify(ctx, it) }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.hub_layout), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HubSettings.Layout.values().forEach { layout ->
                    FilterChip(
                        selected = settings.layout == layout,
                        onClick = { HubSettings.setLayout(ctx, layout) },
                        label = {
                            Text(
                                when (layout) {
                                    HubSettings.Layout.CARDS -> stringResource(R.string.hub_layout_cards)
                                    HubSettings.Layout.COMPACT -> stringResource(R.string.hub_layout_compact)
                                    HubSettings.Layout.GRID -> stringResource(R.string.hub_layout_grid)
                                },
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.hub_close))
            }
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
