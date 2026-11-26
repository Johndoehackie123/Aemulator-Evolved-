package app.aemu.hub

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firmware Hub: a small catalog of firmware known from community reports, one-tap downloads,
 * host-published checksum verification and a sequential import queue.
 *
 * Every visual element is Material 3: Scaffold + LargeFlexibleTopAppBar, FilterChips,
 * tonal Cards, FilledTonal/Outlined/Text buttons, LinearProgressIndicator and AlertDialog.
 * All user-facing copy comes from string resources so the rest of AEmulator's locales can
 * translate it without touching this file.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareHubScreen(
    onBack: () -> Unit,
    onImported: () -> Unit = {},
) {
    BackHandler(onBack = onBack)
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var brand by remember { mutableStateOf("All") }
    var confirmUnverified by remember { mutableStateOf<FirmwareCatalog.Entry?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { HubSettings.load(ctx) }
    val settings by HubSettings.state.collectAsState()
    val downloads by FirmwareDownloads.entries.collectAsState()
    val downloadQueue by FirmwareDownloads.queue.collectAsState()
    val importItems by FirmwareImportQueue.items.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Auto-import can finish while the Hub is in the background; keep the library fresh.
    DisposableEffect(onImported) {
        val previous = FirmwareImportQueue.globalOnImported
        FirmwareImportQueue.globalOnImported = onImported
        onDispose { FirmwareImportQueue.globalOnImported = previous }
    }

    val catalogCount = FirmwareCatalog.entries.size
    val downloadedCount = FirmwareCatalog.entries.count { FirmwareDownloads.finishedFile(ctx, it) != null }
    val queuedCount = downloadQueue.size + importItems.count {
        it.status == FirmwareImportQueue.Status.QUEUED || it.status == FirmwareImportQueue.Status.RUNNING
    }
    val visibleEntries = FirmwareCatalog.entries.filter {
        (settings.showUnverified || it.verified) && (brand == "All" || it.brand == brand)
    }
    val fullSpan: androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope.() ->
        androidx.compose.foundation.lazy.grid.GridItemSpan =
        { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.hub_title)) },
                subtitle = { Text(stringResource(R.string.hub_subtitle), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.hub_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.hub_settings))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyVerticalGrid(
            columns = if (settings.layout == HubSettings.Layout.GRID) GridCells.Adaptive(260.dp) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = fullSpan) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FirmwareCatalog.brands.forEach { b ->
                        FilterChip(
                            selected = brand == b,
                            onClick = { brand = b },
                            label = { Text(if (b == "All") stringResource(R.string.hub_filter_all) else b) },
                        )
                    }
                }
            }

            item(span = fullSpan) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatColumn(catalogCount.toString(), stringResource(R.string.hub_stat_catalog))
                        StatColumn(downloadedCount.toString(), stringResource(R.string.hub_stat_downloaded))
                        StatColumn(queuedCount.toString(), stringResource(R.string.hub_stat_queued))
                    }
                }
            }

            item(span = fullSpan) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(stringResource(R.string.hub_info_card), style = MaterialTheme.typography.bodySmall)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.hub_auto_import_note), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            item(span = fullSpan) {
                Text(
                    stringResource(R.string.hub_catalog),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            items(visibleEntries, key = { it.id }) { e ->
                if (settings.layout == HubSettings.Layout.COMPACT) CompactFirmwareRow(
                    entry = e,
                    state = downloads[e.id] ?: FirmwareDownloads.EntryState(e.id),
                    inQueue = e.id in downloadQueue,
                    finished = FirmwareDownloads.finishedFile(ctx, e),
                    verdict = FirmwareDownloads.verdict(ctx, e),
                    partial = FirmwareDownloads.partialBytes(ctx, e),
                    onDownload = { FirmwareDownloads.enqueue(ctx, e) },
                    onCancel = { FirmwareDownloads.cancel(e.id) },
                    onVerify = { scope.launch(Dispatchers.IO) { FirmwareDownloads.verify(ctx, e) } },
                    onQueueImport = { file ->
                        if (FirmwareDownloads.verdict(ctx, e) == FirmwareDownloads.Verdict.UNVERIFIED) {
                            confirmUnverified = e
                        } else {
                            FirmwareImportQueue.enqueue(ctx, e, file, onImported)
                        }
                    },
                    onDiscard = { FirmwareDownloads.discard(ctx, e) },
                ) else FirmwareCard(
                    entry = e,
                    state = downloads[e.id] ?: FirmwareDownloads.EntryState(e.id),
                    inQueue = e.id in downloadQueue,
                    finished = FirmwareDownloads.finishedFile(ctx, e),
                    verdict = FirmwareDownloads.verdict(ctx, e),
                    partial = FirmwareDownloads.partialBytes(ctx, e),
                    importItem = importItems.firstOrNull { it.id == e.id },
                    onDownload = { FirmwareDownloads.enqueue(ctx, e) },
                    onCancel = { FirmwareDownloads.cancel(e.id) },
                    onVerify = { scope.launch(Dispatchers.IO) { FirmwareDownloads.verify(ctx, e) } },
                    onQueueImport = { file ->
                        if (FirmwareDownloads.verdict(ctx, e) == FirmwareDownloads.Verdict.UNVERIFIED) {
                            confirmUnverified = e
                        } else {
                            FirmwareImportQueue.enqueue(ctx, e, file, onImported)
                        }
                    },
                    onDiscard = { FirmwareDownloads.discard(ctx, e) },
                    onRemoveImport = { FirmwareImportQueue.remove(e.id) },
                )
            }

            item(span = fullSpan) {
                Text(
                    stringResource(R.string.hub_import_queue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (importItems.isEmpty()) {
                item(span = fullSpan) {
                    Text(
                        stringResource(R.string.hub_queue_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                items(importItems, key = { it.id + it.status.name }) { item -> ImportQueueCard(item) }
                item(span = fullSpan) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { FirmwareImportQueue.clearFinished() }) {
                            Text(stringResource(R.string.hub_clear_finished))
                        }
                        TextButton(onClick = { FirmwareImportQueue.cancel() }) {
                            Text(stringResource(R.string.hub_cancel_all))
                        }
                    }
                }
            }

            item(span = fullSpan) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.hub_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    confirmUnverified?.let { e ->
        AlertDialog(
            onDismissRequest = { confirmUnverified = null },
            icon = { Icon(Icons.Rounded.Info, null) },
            title = { Text(stringResource(R.string.hub_dialog_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.hub_dialog_text,
                        e.verificationNote ?: stringResource(R.string.hub_unverified_badge),
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnverified = null
                    FirmwareDownloads.finishedFile(ctx, e)?.let { FirmwareImportQueue.enqueue(ctx, e, it, onImported) }
                }) { Text(stringResource(R.string.hub_import_anyway)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmUnverified = null }) { Text(stringResource(R.string.hub_cancel)) }
            },
        )
    }
    if (showSettings) HubSettingsSheet(onDismiss = { showSettings = false })
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FirmwareCard(
    entry: FirmwareCatalog.Entry,
    state: FirmwareDownloads.EntryState,
    inQueue: Boolean,
    finished: java.io.File?,
    verdict: FirmwareDownloads.Verdict,
    partial: Long,
    importItem: FirmwareImportQueue.Item?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onVerify: () -> Unit,
    onQueueImport: (java.io.File) -> Unit,
    onDiscard: () -> Unit,
    onRemoveImport: () -> Unit,
) {
    val ctx = LocalContext.current
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(entry.brand)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.device, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${entry.codename} · Android ${entry.android} · ${entry.skin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(FirmwareDownloads.fmtBytes(entry.sizeBytes), style = MaterialTheme.typography.labelLarge)
                    Text(
                        entry.format,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.hub_reported, entry.compatReport),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            ChecksumBadge(entry, verdict)

            val active = state.status == FirmwareDownloads.Status.DOWNLOADING ||
                state.status == FirmwareDownloads.Status.VERIFYING || inQueue
            AnimatedVisibility(visible = active) {
                Column(Modifier.padding(top = 12.dp)) {
                    if (inQueue && state.status == FirmwareDownloads.Status.QUEUED) {
                        Text(
                            stringResource(R.string.hub_queued_download),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        val progressText = buildString {
                            append(FirmwareDownloads.fmtBytes(state.received))
                            if (state.total > 0) append(" / ").append(FirmwareDownloads.fmtBytes(state.total))
                            val sp = FirmwareDownloads.fmtSpeed(state.bytesPerSec)
                            if (sp.isNotEmpty()) append(" · ").append(sp)
                            val eta = FirmwareDownloads.fmtEta((state.total - state.received).coerceAtLeast(0), state.bytesPerSec)
                            if (eta.isNotEmpty()) append(" · ").append(eta)
                        }
                        val progressLabel = stringResource(R.string.hub_progress_label) + " " + progressText
                        val indicator = Modifier
                            .fillMaxWidth()
                            .semantics { contentDescription = progressLabel }
                        if (state.progress >= 0f)
                            LinearProgressIndicator(progress = { state.progress }, modifier = indicator)
                        else LinearProgressIndicator(indicator)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            progressText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onCancel) { Text(stringResource(R.string.hub_cancel_download)) }
                    }
                }
            }

            state.message?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    finished != null -> {
                        FilledTonalButton(
                            enabled = importItem == null,
                            onClick = { onQueueImport(finished) },
                        ) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (verdict == FirmwareDownloads.Verdict.UNVERIFIED)
                                    stringResource(R.string.hub_queue_unverified)
                                else stringResource(R.string.hub_queue_import),
                            )
                        }
                        OutlinedButton(onClick = onVerify) { Text(stringResource(R.string.hub_verify)) }
                        IconButton(onClick = onDiscard) {
                            Icon(Icons.Rounded.Delete, stringResource(R.string.hub_delete_file))
                        }
                    }
                    active -> Unit
                    else -> {
                        FilledTonalButton(onClick = onDownload) {
                            Icon(Icons.Rounded.CloudDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (partial > 0) stringResource(R.string.hub_resume, FirmwareDownloads.fmtBytes(partial))
                                else stringResource(R.string.hub_download),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            TextButton(onClick = {
                runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.sourcePage))) }
            }) {
                Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.hub_open_source))
            }

            importItem?.let {
                Text(
                    stringResource(R.string.hub_import_queue_label, importStatusLabel(it.status), it.step),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (it.status == FirmwareImportQueue.Status.QUEUED) {
                    TextButton(onClick = onRemoveImport) { Text(stringResource(R.string.hub_remove_queue)) }
                }
            }
        }
    }
}

@Composable
private fun ChecksumBadge(entry: FirmwareCatalog.Entry, verdict: FirmwareDownloads.Verdict) {
    val spec = entry.checksum
    val (text, color) = when {
        spec == null -> stringResource(R.string.hub_unverified_badge) to MaterialTheme.colorScheme.error
        verdict == FirmwareDownloads.Verdict.MISMATCH ->
            stringResource(R.string.hub_mismatch_badge) to MaterialTheme.colorScheme.error
        verdict == FirmwareDownloads.Verdict.VERIFIED ->
            stringResource(R.string.hub_verified_short, spec.algorithm, spec.hex.take(12)) to
                MaterialTheme.colorScheme.tertiary
        else ->
            stringResource(R.string.hub_expected_short, spec.algorithm, spec.hex.take(12)) to
                MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (spec != null && verdict == FirmwareDownloads.Verdict.VERIFIED) Icons.Rounded.Verified
            else Icons.Rounded.Info,
            null,
            Modifier.size(14.dp),
            tint = color,
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ImportQueueCard(item: FirmwareImportQueue.Item) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(14.dp).animateContentSize()) {
            Text(item.file, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.progress in 0f..1f && item.status == FirmwareImportQueue.Status.RUNNING) {
                val queueProgressLabel = stringResource(R.string.hub_progress_label) + " " + item.step
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = queueProgressLabel },
                )
            }
            Text(
                "${importStatusLabel(item.status)}${if (item.step.isNotBlank()) " · ${item.step}" else ""}" +
                    (item.message?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.status == FirmwareImportQueue.Status.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun importStatusLabel(status: FirmwareImportQueue.Status): String = when (status) {
    FirmwareImportQueue.Status.QUEUED -> stringResource(R.string.hub_status_queued)
    FirmwareImportQueue.Status.RUNNING -> stringResource(R.string.hub_status_running)
    FirmwareImportQueue.Status.DONE -> stringResource(R.string.hub_status_done)
    FirmwareImportQueue.Status.FAILED -> stringResource(R.string.hub_status_failed)
    FirmwareImportQueue.Status.CANCELLED -> stringResource(R.string.hub_status_cancelled)
}

@Composable
private fun CompactFirmwareRow(
    entry: FirmwareCatalog.Entry,
    state: FirmwareDownloads.EntryState,
    inQueue: Boolean,
    finished: java.io.File?,
    verdict: FirmwareDownloads.Verdict,
    partial: Long,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onVerify: () -> Unit,
    onQueueImport: (java.io.File) -> Unit,
    onDiscard: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(12.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(entry.brand)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(entry.device, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Android ${entry.android} · ${FirmwareDownloads.fmtBytes(entry.sizeBytes)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ChecksumBadge(entry, verdict)
                }
                if (finished != null) {
                    IconButton(onClick = { onQueueImport(finished) }) {
                        Icon(Icons.Rounded.PlayArrow, stringResource(R.string.hub_queue_import))
                    }
                    IconButton(onClick = onDiscard) { Icon(Icons.Rounded.Delete, stringResource(R.string.hub_delete_file)) }
                } else if (!inQueue) {
                    IconButton(onClick = onDownload) { Icon(Icons.Rounded.CloudDownload, stringResource(R.string.hub_download)) }
                }
            }
            val active = state.status == FirmwareDownloads.Status.DOWNLOADING ||
                state.status == FirmwareDownloads.Status.VERIFYING || inQueue
            if (active) {
                Spacer(Modifier.height(8.dp))
                if (inQueue && state.status == FirmwareDownloads.Status.QUEUED) {
                    Text(stringResource(R.string.hub_queued_download), style = MaterialTheme.typography.labelSmall)
                } else {
                    LinearProgressIndicator(
                        progress = { if (state.progress >= 0f) state.progress else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TextButton(onClick = onCancel) { Text(stringResource(R.string.hub_cancel_download)) }
                }
            } else if (partial > 0) {
                Text(
                    stringResource(R.string.hub_resume, FirmwareDownloads.fmtBytes(partial)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (finished != null) TextButton(onClick = onVerify) { Text(stringResource(R.string.hub_verify)) }
        }
    }
}

@Composable
private fun BrandBadge(brand: String) {
    val label = when (brand) {
        "Samsung" -> "S"
        "Google" -> "G"
        "Xiaomi" -> "MI"
        else -> brand.take(1)
    }
    // Brand identity colours are intentionally kept; all app chrome uses theme tokens.
    val tint = when (brand) {
        "Samsung" -> Color(0xFF1428A0)
        "Google" -> Color(0xFF0B8043)
        "Xiaomi" -> Color(0xFFFF6900)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
    }
}
