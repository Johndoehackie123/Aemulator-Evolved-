package app.aemu.hub

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Firmware Hub: a small catalog of firmware known from community reports, one-tap downloads,
 * host-published checksum verification and a sequential import queue.
 *
 * AEmulator does not host firmware. Links go to the manufacturer or a public archive, and
 * entries without a published fingerprint are labelled UNVERIFIED in the UI.
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

    val downloads by FirmwareDownloads.entries.collectAsState()
    val downloadQueue by FirmwareDownloads.queue.collectAsState()
    val importItems by FirmwareImportQueue.items.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Firmware Hub") },
                subtitle = { Text("Catalog · checksums · import queue", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FirmwareCatalog.brands.forEach { b ->
                        FilterChip(selected = brand == b, onClick = { brand = b }, label = { Text(b) })
                    }
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Downloads are verified against fingerprints published by the file host. " +
                                "Files with no published checksum are marked UNVERIFIED and need your confirmation before import.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            items(FirmwareCatalog.entries.filter { brand == "All" || it.brand == brand }, key = { it.id }) { e ->
                FirmwareCard(
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
                        if (FirmwareDownloads.verdict(ctx, e) == FirmwareDownloads.Verdict.UNVERIFIED) confirmUnverified = e
                        else FirmwareImportQueue.enqueue(ctx, e, file, onImported)
                    },
                    onDiscard = { FirmwareDownloads.discard(ctx, e) },
                    onRemoveImport = { FirmwareImportQueue.remove(e.id) },
                )
            }

            item {
                Text("Import queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (importItems.isEmpty()) {
                item {
                    Text("Nothing queued. Download a firmware and tap “Queue import”.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(importItems, key = { it.id + it.status.name }) { item -> ImportQueueCard(item) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { FirmwareImportQueue.clearFinished() }) { Text("Clear finished") }
                        TextButton(onClick = { FirmwareImportQueue.cancel() }) { Text("Cancel all") }
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "AEmulator is GPL-3.0 and based on uxazu/AEmulator, which is based on the original " +
                        "SENSES emulator by Istratii-tech. Firmware belongs to its respective owners; " +
                        "this app ships no firmware. Compatibility notes are community reports, not guarantees.",
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
            title = { Text("Import without a checksum?") },
            text = {
                Text("${e.verificationNote ?: "No published checksum."}\n\n" +
                    "The file's size was checked, but its content could not be authenticated. " +
                    "Import it only if you obtained it from a source you trust.")
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmUnverified = null
                    FirmwareDownloads.finishedFile(ctx, e)?.let { FirmwareImportQueue.enqueue(ctx, e, it, onImported) }
                }) { Text("Import anyway") }
            },
            dismissButton = { TextButton(onClick = { confirmUnverified = null }) { Text("Cancel") } },
        )
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
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp)) {
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
                    Text(entry.format, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text("Reported: ${entry.compatReport}", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            ChecksumBadge(entry, verdict)

            val active = state.status == FirmwareDownloads.Status.DOWNLOADING ||
                state.status == FirmwareDownloads.Status.VERIFYING || inQueue
            AnimatedVisibility(visible = active) {
                Column(Modifier.padding(top = 12.dp)) {
                    if (inQueue && state.status == FirmwareDownloads.Status.QUEUED) {
                        Text("Queued for download", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        if (state.progress >= 0f)
                            LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        val remaining = (state.total - state.received).coerceAtLeast(0)
                        val eta = FirmwareDownloads.fmtEta(remaining, state.bytesPerSec)
                        Text(
                            buildString {
                                append(FirmwareDownloads.fmtBytes(state.received))
                                if (state.total > 0) append(" / ").append(FirmwareDownloads.fmtBytes(state.total))
                                val sp = FirmwareDownloads.fmtSpeed(state.bytesPerSec)
                                if (sp.isNotEmpty()) append(" · ").append(sp)
                                if (eta.isNotEmpty()) append(" · ").append(eta)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = onCancel) { Text("Cancel download") }
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
                            Text(if (verdict == FirmwareDownloads.Verdict.UNVERIFIED) "Queue (unverified)" else "Queue import")
                        }
                        OutlinedButton(onClick = onVerify) { Text("Verify") }
                        IconButton(onClick = onDiscard) { Icon(Icons.Rounded.Delete, "Delete file") }
                    }
                    active -> Unit
                    else -> {
                        FilledTonalButton(onClick = onDownload) {
                            Icon(Icons.Rounded.CloudDownload, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (partial > 0) "Resume (${FirmwareDownloads.fmtBytes(partial)})" else "Download")
                        }
                    }
                }
            }
            importItem?.let {
                Text(
                    "Import queue: ${it.status.name.lowercase()} — ${it.step}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (it.status == FirmwareImportQueue.Status.QUEUED) TextButton(onClick = onRemoveImport) { Text("Remove from queue") }
            }
        }
    }
}

@Composable
private fun ChecksumBadge(entry: FirmwareCatalog.Entry, verdict: FirmwareDownloads.Verdict) {
    val spec = entry.checksum
    val (text, color) = when {
        spec == null -> "UNVERIFIED · no published checksum" to MaterialTheme.colorScheme.error
        verdict == FirmwareDownloads.Verdict.MISMATCH -> "CHECKSUM MISMATCH · file discarded" to MaterialTheme.colorScheme.error
        verdict == FirmwareDownloads.Verdict.VERIFIED -> "${spec.algorithm} verified: ${spec.hex.take(12)}…" to Color(0xFF2E9E6B)
        else -> "${spec.algorithm} expected: ${spec.hex.take(12)}…" to MaterialTheme.colorScheme.primary
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (spec != null && verdict == FirmwareDownloads.Verdict.VERIFIED) Icons.Rounded.Verified else Icons.Rounded.Info,
            null, Modifier.size(14.dp), tint = color,
        )
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

@Composable
private fun ImportQueueCard(item: FirmwareImportQueue.Item) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(14.dp)) {
            Text(item.file, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.progress in 0f..1f && item.status == FirmwareImportQueue.Status.RUNNING)
                LinearProgressIndicator(progress = { item.progress }, modifier = Modifier.fillMaxWidth())
            Text(
                "${item.status.name.lowercase()}${if (item.step.isNotBlank()) " · ${item.step}" else ""}" +
                    (item.message?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (item.status == FirmwareImportQueue.Status.FAILED) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
