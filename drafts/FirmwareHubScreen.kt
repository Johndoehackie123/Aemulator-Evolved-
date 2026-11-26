package app.aemu.hub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Firmware Hub: a catalog of firmware that is known to work in TimeDroid, with one-tap
 * download and import. Everything is hosted by the manufacturers or public archives —
 * the app only points at it and prepares the file for the normal import pipeline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmwareHubScreen(
    onBack: () -> Unit,
    onDownload: (FirmwareCatalog.Entry) -> Unit,
    onCancel: () -> Unit,
    onImport: (FirmwareCatalog.Entry) -> Unit,
    onDiscard: (FirmwareCatalog.Entry) -> Unit,
) {
    val ctx = LocalContext.current
    var brand by remember { mutableStateOf("All") }
    val state by FirmwareDownloads.state.collectAsState()
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var tick by remember { mutableStateOf(0) }
    LaunchedEffect(state) { tick++ }  // refresh finished/partial badges when the state changes

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("Firmware Hub") },
                subtitle = { Text("Tested firmware, one tap away", maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Back") } },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FirmwareCatalog.brands.forEach { b ->
                        FilterChip(selected = brand == b, onClick = { brand = b }, label = { Text(b) })
                    }
                }
            }
            items(FirmwareCatalog.entries.filter { brand == "All" || it.brand == brand }, key = { it.id }) { e ->
                val isThis = (state as? FirmwareDownloads.State.Running)?.id == e.id
                FirmwareCard(
                    entry = e,
                    running = state as? FirmwareDownloads.State.Running,
                    isThis = isThis,
                    finished = FirmwareDownloads.finishedFile(ctx, e) != null,
                    partial = FirmwareDownloads.partialBytes(ctx, e),
                    onDownload = { onDownload(e) },
                    onCancel = onCancel,
                    onImport = { onImport(e) },
                    onDiscard = { onDiscard(e) },
                )
            }
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Firmware files belong to their manufacturers; links point at their servers or public archives. " +
                        "TimeDroid ships no firmware. Import takes a few minutes and needs 1–3 GB per system.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FirmwareCard(
    entry: FirmwareCatalog.Entry,
    running: FirmwareDownloads.State.Running?,
    isThis: Boolean,
    finished: Boolean,
    partial: Long,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onImport: () -> Unit,
    onDiscard: () -> Unit,
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
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(FirmwareDownloads.fmtBytes(entry.sizeBytes), style = MaterialTheme.typography.labelLarge)
                    Text(entry.format, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(10.dp))
            StatusLine(entry.status)
            AnimatedVisibility(visible = isThis && running != null) {
                running?.let { r ->
                    Column(Modifier.padding(top = 12.dp)) {
                        if (r.progress >= 0f) LinearProgressIndicator(progress = { r.progress }, modifier = Modifier.fillMaxWidth())
                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                        Spacer(Modifier.height(6.dp))
                        val remaining = (r.total - r.received).coerceAtLeast(0)
                        val eta = FirmwareDownloads.fmtEta(remaining, r.bytesPerSec)
                        Text(
                            buildString {
                                append(FirmwareDownloads.fmtBytes(r.received))
                                if (r.total > 0) append(" / ").append(FirmwareDownloads.fmtBytes(r.total))
                                val sp = FirmwareDownloads.fmtSpeed(r.bytesPerSec)
                                if (sp.isNotEmpty()) append(" · ").append(sp)
                                if (eta.isNotEmpty()) append(" · ").append(eta)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = onCancel) { Text("Cancel") }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    isThis -> Unit
                    finished -> {
                        FilledTonalButton(onClick = onImport) {
                            Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Import")
                        }
                        IconButton(onClick = onDiscard) { Icon(Icons.Rounded.Delete, "Delete file") }
                    }
                    else -> {
                        FilledTonalButton(onClick = onDownload) {
                            Icon(Icons.Rounded.Download, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp))
                            Text(if (partial > 0) "Resume (${FirmwareDownloads.fmtBytes(partial)})" else "Download")
                        }
                        if (partial > 0) Text(partial.let { "${FirmwareDownloads.fmtBytes(it)} downloaded" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(status: String) {
    val (dot, color) = when {
        status.startsWith("Works") -> "●" to androidx.compose.ui.graphics.Color(0xFF2E9E6B)
        status.startsWith("Partly") -> "●" to androidx.compose.ui.graphics.Color(0xFFC68A12)
        status.startsWith("In progress") -> "●" to androidx.compose.ui.graphics.Color(0xFFE07A2E)
        else -> "●" to MaterialTheme.colorScheme.error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(dot, color = color, fontSize = 10.sp)
        Spacer(Modifier.width(6.dp))
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BrandBadge(brand: String) {
    val label = when (brand) {
        "Samsung" -> "S"
        "Google" -> "G"
        "Xiaomi" -> "MI"
        "HTC" -> "HTC"
        else -> brand.take(1)
    }
    val tint = when (brand) {
        "Samsung" -> androidx.compose.ui.graphics.Color(0xFF1428A0)
        "Google" -> androidx.compose.ui.graphics.Color(0xFF0B8043)
        "Xiaomi" -> androidx.compose.ui.graphics.Color(0xFFFF6900)
        "HTC" -> androidx.compose.ui.graphics.Color(0xFF00A650)
        else -> MaterialTheme.colorScheme.primary
    }
    Box(Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
    }
}
