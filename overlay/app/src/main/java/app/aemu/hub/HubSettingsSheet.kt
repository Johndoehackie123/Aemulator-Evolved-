package app.aemu.hub

import androidx.compose.foundation.horizontalScroll
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
import app.aemu.ui.glassPanel

/** Material 3 bottom sheet for the Hub-only preferences, layout choice and field customisation. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubSettingsSheet(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val settings by HubSettings.state.collectAsState()
    val fields = HubSettings.fieldsFor(settings.layout)
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
            ) { HubSettings.setDeleteAfter(ctx, it) }
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
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HubSettings.Layout.values().forEach { layout ->
                    FilterChip(
                        selected = settings.layout == layout,
                        onClick = { HubSettings.setLayout(ctx, layout) },
                        modifier = Modifier.glassPanel(),
                        label = { Text(layoutLabel(layout)) },
                    )
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(
                stringResource(R.string.hub_fields_title, layoutLabel(settings.layout)),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(R.string.hub_fields_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            fields.forEachIndexed { index, field ->
                FieldRow(
                    label = fieldLabel(field),
                    checked = field in fields,
                    canMoveUp = index > 0,
                    canMoveDown = index < fields.lastIndex,
                    onCheckedChange = { HubSettings.toggleField(ctx, settings.layout, field) },
                    onUp = { HubSettings.moveField(ctx, settings.layout, field, -1) },
                    onDown = { HubSettings.moveField(ctx, settings.layout, field, 1) },
                )
            }
            TextButton(onClick = { HubSettings.resetFields(ctx, settings.layout) }) {
                Text(stringResource(R.string.hub_fields_reset))
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))
            Text(stringResource(R.string.hub_storage_quota), style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0, 2, 5, 10).forEach { gb ->
                    FilterChip(
                        selected = settings.storageQuotaGb == gb,
                        onClick = { HubSettings.setStorageQuota(ctx, gb) },
                        modifier = Modifier.glassPanel(),
                        label = {
                            Text(
                                when (gb) {
                                    0 -> stringResource(R.string.hub_storage_unlimited)
                                    2 -> stringResource(R.string.hub_storage_2gb)
                                    5 -> stringResource(R.string.hub_storage_5gb)
                                    else -> stringResource(R.string.hub_storage_10gb)
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
private fun layoutLabel(layout: HubSettings.Layout): String = when (layout) {
    HubSettings.Layout.CARDS -> stringResource(R.string.hub_layout_cards)
    HubSettings.Layout.COMPACT -> stringResource(R.string.hub_layout_compact)
    HubSettings.Layout.GRID -> stringResource(R.string.hub_layout_grid)
    HubSettings.Layout.MASONRY -> stringResource(R.string.hub_layout_masonry)
    HubSettings.Layout.LIST -> stringResource(R.string.hub_layout_list)
    HubSettings.Layout.TIMELINE -> stringResource(R.string.hub_layout_timeline)
    HubSettings.Layout.FEATURED -> stringResource(R.string.hub_layout_featured)
    HubSettings.Layout.TABLE -> stringResource(R.string.hub_layout_table)
    HubSettings.Layout.CAROUSEL -> stringResource(R.string.hub_layout_carousel)
    HubSettings.Layout.TEXT -> stringResource(R.string.hub_layout_text)
}

@Composable
private fun fieldLabel(field: HubField): String = when (field) {
    HubField.STATUS -> stringResource(R.string.hub_field_status)
    HubField.SIZE -> stringResource(R.string.hub_field_size)
    HubField.CHECKSUM -> stringResource(R.string.hub_field_checksum)
    HubField.SOURCE -> stringResource(R.string.hub_field_source)
    HubField.FORMAT -> stringResource(R.string.hub_field_format)
    HubField.REPORT -> stringResource(R.string.hub_field_report)
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

@Composable
private fun FieldRow(
    label: String,
    checked: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        TextButton(onClick = onUp, enabled = canMoveUp) {
            Text(stringResource(R.string.hub_field_move_up))
        }
        TextButton(onClick = onDown, enabled = canMoveDown) {
            Text(stringResource(R.string.hub_field_move_down))
        }
    }
}
