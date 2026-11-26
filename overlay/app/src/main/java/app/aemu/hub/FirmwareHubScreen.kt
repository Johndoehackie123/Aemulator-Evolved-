package app.aemu.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.aemu.R
import app.aemu.ui.glassBorder
import app.aemu.ui.glassCardColors
import app.aemu.ui.glassTonalButtonColors
import app.aemu.ui.glassWell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Firmware Hub: a small catalog of firmware known from community reports, one-tap downloads,
 * host-published checksum verification and a sequential import queue.
 *
 * Ten layouts are available: Cards, Compact, Grid, Mosaic, List, Timeline, Featured, Table,
 * Carousel and Text only. Every layout uses the same download/import actions and the same
 * per-layout field order chosen in Hub settings, so switching views never changes behaviour.
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

    DisposableEffect(onImported) {
        val previous = FirmwareImportQueue.globalOnImported
        FirmwareImportQueue.globalOnImported = onImported
        onDispose { FirmwareImportQueue.globalOnImported = previous }
    }

    val visibleEntries = FirmwareCatalog.entries.filter {
        (settings.showUnverified || it.verified) && (brand == "All" || it.brand == brand)
    }
    val views = visibleEntries.map { buildView(ctx, it, downloads, downloadQueue, importItems) }
    val fields = HubSettings.fieldsFor(settings.layout)
    val fullSpan = StaggeredGridItemSpan.FullLine
    val fullWidthLayout = settings.layout !in setOf(HubSettings.Layout.GRID, HubSettings.Layout.MASONRY)

    fun actionsFor(entry: FirmwareCatalog.Entry) = buildActions(ctx, scope, entry, { confirmUnverified = it }, onImported)

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.hub_title)) },
                subtitle = { Text(stringResource(R.string.hub_subtitle), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.glassWell()) {
                        Icon(Icons.Rounded.ArrowBack, stringResource(R.string.hub_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showSettings = true }, modifier = Modifier.glassWell()) {
                        Icon(Icons.Rounded.Settings, stringResource(R.string.hub_settings))
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyVerticalStaggeredGrid(
            columns = when (settings.layout) {
                HubSettings.Layout.MASONRY -> StaggeredGridCells.Adaptive(180.dp)
                HubSettings.Layout.GRID -> StaggeredGridCells.Adaptive(260.dp)
                else -> StaggeredGridCells.Fixed(1)
            },
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalItemSpacing = 12.dp,
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
                    colors = glassCardColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        StatColumn(FirmwareCatalog.entries.size.toString(), stringResource(R.string.hub_stat_catalog))
                        StatColumn(
                            FirmwareCatalog.entries.count { FirmwareDownloads.finishedFile(ctx, it) != null }.toString(),
                            stringResource(R.string.hub_stat_downloaded),
                        )
                        StatColumn(
                            (downloadQueue.size + importItems.count {
                                it.status == FirmwareImportQueue.Status.QUEUED ||
                                    it.status == FirmwareImportQueue.Status.RUNNING
                            }).toString(),
                            stringResource(R.string.hub_stat_queued),
                        )
                    }
                }
            }

            item(span = fullSpan) {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = glassCardColors(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer),
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

            when (settings.layout) {
                HubSettings.Layout.TABLE -> {
                    item(key = "firmware-table", span = fullSpan) {
                        FirmwareTable(views, fields) { actionsFor(it.entry) }
                    }
                }

                HubSettings.Layout.CAROUSEL -> {
                    item(key = "firmware-carousel", span = fullSpan) {
                        FirmwareCarousel(views, fields) { actionsFor(it.entry) }
                    }
                }

                HubSettings.Layout.FEATURED -> {
                    val hero = views.firstOrNull { it.entry.verified } ?: views.firstOrNull()
                    hero?.let {
                        item(key = "featured-${it.entry.id}", span = fullSpan) {
                            FeaturedFirmwareCard(it, fields, actionsFor(it.entry))
                        }
                    }
                    views.filter { it.entry.id != hero?.entry?.id }.forEach { view ->
                        item(key = view.entry.id, span = fullSpan) {
                            ListFirmwareRow(view, fields, actionsFor(view.entry))
                        }
                    }
                }

                else -> {
                    views.forEach { view ->
                        if (fullWidthLayout) {
                            item(key = view.entry.id, span = fullSpan) {
                                LayoutItem(view, fields, actionsFor(view.entry), settings.layout)
                            }
                        } else {
                            item(key = view.entry.id) {
                                LayoutItem(view, fields, actionsFor(view.entry), settings.layout)
                            }
                        }
                    }
                }
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
                importItems.forEach { queueItem ->
                    item(key = "queue-${queueItem.id}-${queueItem.status.name}", span = fullSpan) {
                        ImportQueueCard(queueItem)
                    }
                }
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

// ---------------------------------------------------------------------------
// View/action wiring shared by every layout
// ---------------------------------------------------------------------------

private data class EntryView(
    val entry: FirmwareCatalog.Entry,
    val state: FirmwareDownloads.EntryState,
    val inQueue: Boolean,
    val finished: File?,
    val verdict: FirmwareDownloads.Verdict,
    val partial: Long,
    val importItem: FirmwareImportQueue.Item?,
) {
    val active: Boolean get() = state.status == FirmwareDownloads.Status.DOWNLOADING ||
        state.status == FirmwareDownloads.Status.VERIFYING || inQueue
}

private data class EntryActions(
    val download: () -> Unit,
    val cancel: () -> Unit,
    val verify: () -> Unit,
    val queueImport: (File) -> Unit,
    val discard: () -> Unit,
    val removeImport: () -> Unit,
)

private fun buildView(
    ctx: Context,
    entry: FirmwareCatalog.Entry,
    downloads: Map<String, FirmwareDownloads.EntryState>,
    downloadQueue: List<String>,
    importItems: List<FirmwareImportQueue.Item>,
) = EntryView(
    entry = entry,
    state = downloads[entry.id] ?: FirmwareDownloads.EntryState(entry.id),
    inQueue = entry.id in downloadQueue,
    finished = FirmwareDownloads.finishedFile(ctx, entry),
    verdict = FirmwareDownloads.verdict(ctx, entry),
    partial = FirmwareDownloads.partialBytes(ctx, entry),
    importItem = importItems.firstOrNull { it.id == entry.id },
)

private fun buildActions(
    ctx: Context,
    scope: CoroutineScope,
    entry: FirmwareCatalog.Entry,
    onConfirmUnverified: (FirmwareCatalog.Entry) -> Unit,
    onImported: () -> Unit,
) = EntryActions(
    download = { FirmwareDownloads.enqueue(ctx, entry) },
    cancel = { FirmwareDownloads.cancel(entry.id) },
    verify = { scope.launch(Dispatchers.IO) { FirmwareDownloads.verify(ctx, entry) } },
    queueImport = { file ->
        if (FirmwareDownloads.verdict(ctx, entry) == FirmwareDownloads.Verdict.UNVERIFIED) {
            onConfirmUnverified(entry)
        } else {
            FirmwareImportQueue.enqueue(ctx, entry, file, onImported)
        }
    },
    discard = { FirmwareDownloads.discard(ctx, entry) },
    removeImport = { FirmwareImportQueue.remove(entry.id) },
)

// ---------------------------------------------------------------------------
// Field rendering (the same order/visibility drives every layout)
// ---------------------------------------------------------------------------

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
private fun checksumBadgeParts(view: EntryView): Pair<String, Color> =
    view.entry.checksum.let { spec ->
        when {
            spec == null -> stringResource(R.string.hub_unverified_badge) to MaterialTheme.colorScheme.error
            view.verdict == FirmwareDownloads.Verdict.MISMATCH ->
                stringResource(R.string.hub_mismatch_badge) to MaterialTheme.colorScheme.error
            view.verdict == FirmwareDownloads.Verdict.VERIFIED ->
                stringResource(R.string.hub_verified_short, spec!!.algorithm, spec!!.hex.take(12)) to
                    MaterialTheme.colorScheme.tertiary
            else -> stringResource(R.string.hub_expected_short, spec!!.algorithm, spec!!.hex.take(12)) to
                MaterialTheme.colorScheme.primary
        }
    }

@Composable
private fun statusValue(view: EntryView): String = when {
    view.verdict == FirmwareDownloads.Verdict.MISMATCH -> stringResource(R.string.hub_state_mismatch)
    view.importItem != null -> stringResource(R.string.hub_state_importing) +
        " · " + importStatusLabel(view.importItem!!.status)
    view.finished != null && view.verdict == FirmwareDownloads.Verdict.VERIFIED ->
        stringResource(R.string.hub_state_verified)
    view.finished != null -> stringResource(R.string.hub_state_downloaded)
    view.inQueue || view.state.status == FirmwareDownloads.Status.QUEUED ->
        stringResource(R.string.hub_queued_download)
    view.state.status == FirmwareDownloads.Status.DOWNLOADING ||
        view.state.status == FirmwareDownloads.Status.VERIFYING -> stringResource(R.string.hub_status_running)
    view.verdict == FirmwareDownloads.Verdict.UNVERIFIED -> stringResource(R.string.hub_state_unverified)
    else -> stringResource(R.string.hub_state_ready)
}

@Composable
private fun fieldValue(view: EntryView, field: HubField): String = when (field) {
    HubField.STATUS -> statusValue(view)
    HubField.SIZE -> FirmwareDownloads.fmtBytes(view.entry.sizeBytes)
    HubField.CHECKSUM -> checksumBadgeParts(view).first
    HubField.SOURCE -> view.entry.source
    HubField.FORMAT -> view.entry.format
    HubField.REPORT -> view.entry.compatReport
}

@Composable
private fun FieldStrip(view: EntryView, fields: List<HubField>, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        fields.forEach { field ->
            Row {
                Text(
                    fieldLabel(field) + ": ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    fieldValue(view, field),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FieldCell(view: EntryView, field: HubField) {
    Column(Modifier.width(132.dp).padding(end = 8.dp)) {
        Text(
            fieldLabel(field),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Text(
            fieldValue(view, field),
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Actions and progress (shared by all ten layouts)
// ---------------------------------------------------------------------------

@Composable
private fun DownloadProgress(view: EntryView, actions: EntryActions, compact: Boolean) {
    if (view.active) {
        Spacer(Modifier.height(8.dp))
        if (view.inQueue && view.state.status == FirmwareDownloads.Status.QUEUED) {
            Text(stringResource(R.string.hub_queued_download), style = MaterialTheme.typography.labelSmall)
        } else {
            val progressText = buildString {
                append(FirmwareDownloads.fmtBytes(view.state.received))
                if (view.state.total > 0) append(" / ").append(FirmwareDownloads.fmtBytes(view.state.total))
                val speed = FirmwareDownloads.fmtSpeed(view.state.bytesPerSec)
                if (speed.isNotEmpty()) append(" · ").append(speed)
                val eta = FirmwareDownloads.fmtEta(
                    (view.state.total - view.state.received).coerceAtLeast(0),
                    view.state.bytesPerSec,
                )
                if (eta.isNotEmpty()) append(" · ").append(eta)
            }
            val label = stringResource(R.string.hub_progress_label) + " " + progressText
            val modifier = Modifier.fillMaxWidth().semantics { contentDescription = label }
            if (view.state.progress >= 0f) {
                LinearProgressIndicator(progress = { view.state.progress }, modifier = modifier)
            } else {
                LinearProgressIndicator(modifier)
            }
            if (!compact) {
                Spacer(Modifier.height(6.dp))
                Text(
                    progressText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = actions.cancel) { Text(stringResource(R.string.hub_cancel_download)) }
        }
    }
}

@Composable
private fun EntryActionsRow(view: EntryView, actions: EntryActions, compact: Boolean = false) {
    val finished = view.finished
    when {
        finished != null -> {
            FilledTonalButton(
                enabled = view.importItem == null,
                onClick = { actions.queueImport(finished) },
                colors = glassTonalButtonColors(),
            ) {
                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (view.verdict == FirmwareDownloads.Verdict.UNVERIFIED)
                        stringResource(R.string.hub_queue_unverified)
                    else stringResource(R.string.hub_queue_import),
                )
            }
            OutlinedButton(onClick = actions.verify) { Text(stringResource(R.string.hub_verify)) }
            IconButton(onClick = actions.discard, modifier = Modifier.glassWell()) {
                Icon(Icons.Rounded.Delete, stringResource(R.string.hub_delete_file))
            }
        }
        view.active -> Unit
        else -> {
            FilledTonalButton(onClick = actions.download, colors = glassTonalButtonColors()) {
                Icon(Icons.Rounded.CloudDownload, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    if (view.partial > 0) stringResource(R.string.hub_resume, FirmwareDownloads.fmtBytes(view.partial))
                    else stringResource(R.string.hub_download),
                )
            }
        }
    }
    if (compact) {
        DownloadProgress(view, actions, compact = true)
    } else {
        DownloadProgress(view, actions, compact = false)
    }
    view.state.message?.takeIf { it.isNotBlank() }?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    view.importItem?.let { item ->
        Text(
            stringResource(R.string.hub_import_queue_label, importStatusLabel(item.status), item.step),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.status == FirmwareImportQueue.Status.QUEUED) {
            TextButton(onClick = actions.removeImport) { Text(stringResource(R.string.hub_remove_queue)) }
        }
    }
}

@Composable
private fun SourceLink(entry: FirmwareCatalog.Entry, ctx: Context) {
    TextButton(onClick = {
        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(entry.sourcePage))) }
    }) {
        Icon(Icons.Rounded.OpenInNew, null, Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(stringResource(R.string.hub_open_source))
    }
}

// ---------------------------------------------------------------------------
// Layouts
// ---------------------------------------------------------------------------

@Composable
private fun LayoutItem(
    view: EntryView,
    fields: List<HubField>,
    actions: EntryActions,
    layout: HubSettings.Layout,
) {
    when (layout) {
        HubSettings.Layout.COMPACT -> CompactFirmwareRow(view, fields, actions)
        HubSettings.Layout.LIST -> ListFirmwareRow(view, fields, actions)
        HubSettings.Layout.TIMELINE -> TimelineFirmwareRow(view, fields, actions)
        HubSettings.Layout.TEXT -> TextFirmwareRow(view, fields, actions)
        else -> FirmwareCard(
            modifier = if (layout == HubSettings.Layout.MASONRY) {
                val accent = when {
                    view.verdict == FirmwareDownloads.Verdict.VERIFIED -> MaterialTheme.colorScheme.tertiary
                    view.verdict == FirmwareDownloads.Verdict.UNVERIFIED -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                }
                Modifier.border(2.dp, accent, MaterialTheme.shapes.extraLarge)
            } else Modifier,
            view = view,
            fields = fields,
            actions = actions,
        )
    }
}

@Composable
private fun FirmwareCard(
    modifier: Modifier = Modifier,
    view: EntryView,
    fields: List<HubField>,
    actions: EntryActions,
) {
    val ctx = LocalContext.current
    Card(
        modifier = modifier.glassBorder(MaterialTheme.shapes.extraLarge),
        shape = MaterialTheme.shapes.extraLarge,
        colors = glassCardColors(),
    ) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(view.entry.brand)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(view.entry.device, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${view.entry.codename} · Android ${view.entry.android} · ${view.entry.skin}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            FieldStrip(view, fields)
            Spacer(Modifier.height(10.dp))
            EntryActionsRow(view, actions)
            if (HubField.SOURCE in fields) SourceLink(view.entry, ctx)
        }
    }
}

@Composable
private fun CompactFirmwareRow(view: EntryView, fields: List<HubField>, actions: EntryActions) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = glassCardColors(),
    ) {
        Column(Modifier.padding(12.dp).animateContentSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandBadge(view.entry.brand)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        view.entry.device,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "Android ${view.entry.android}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FieldStrip(view, fields)
                }
            }
            EntryActionsRow(view, actions, compact = true)
        }
    }
}

@Composable
private fun ListFirmwareRow(view: EntryView, fields: List<HubField>, actions: EntryActions) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = glassCardColors(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            BrandBadge(view.entry.brand)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(view.entry.device, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "${view.entry.codename} · Android ${view.entry.android} · ${view.entry.skin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                FieldStrip(view, fields)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                EntryActionsRow(view, actions, compact = true)
            }
        }
    }
}

@Composable
private fun TimelineFirmwareRow(view: EntryView, fields: List<HubField>, actions: EntryActions) {
    val dotColor = when {
        view.verdict == FirmwareDownloads.Verdict.VERIFIED -> MaterialTheme.colorScheme.tertiary
        view.verdict == FirmwareDownloads.Verdict.UNVERIFIED -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Row {
        Box(Modifier.size(width = 28.dp, height = 96.dp), contentAlignment = Alignment.TopCenter) {
            Box(
                Modifier
                    .width(2.dp)
                    .height(96.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Box(Modifier.size(12.dp).clip(CircleShape).background(dotColor))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(bottom = 12.dp)) {
            Text(view.entry.device, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "${view.entry.codename} · Android ${view.entry.android}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            FieldStrip(view, fields)
            EntryActionsRow(view, actions, compact = true)
        }
    }
}

@Composable
private fun FeaturedFirmwareCard(view: EntryView, fields: List<HubField>, actions: EntryActions) {
    val ctx = LocalContext.current
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = glassCardColors(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer),
    ) {
        Column(Modifier.padding(20.dp).animateContentSize()) {
            Text(
                stringResource(R.string.hub_layout_featured).uppercase(),
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(view.entry.device, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "${view.entry.codename} · Android ${view.entry.android} · ${view.entry.skin}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            FieldStrip(view, fields)
            Spacer(Modifier.height(12.dp))
            EntryActionsRow(view, actions)
            if (HubField.SOURCE in fields) SourceLink(view.entry, ctx)
        }
    }
}

@Composable
private fun FirmwareTable(
    views: List<EntryView>,
    fields: List<HubField>,
    actionsFor: (EntryView) -> EntryActions,
) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = glassCardColors(),
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState()).padding(12.dp)) {
            Row {
                Text(
                    stringResource(R.string.hub_catalog),
                    Modifier.width(160.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                fields.forEach { field ->
                    Text(
                        fieldLabel(field),
                        Modifier.width(132.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            HorizontalRule()
            views.forEach { view ->
                Row(Modifier.padding(vertical = 8.dp)) {
                    Column(Modifier.width(160.dp)) {
                        Text(view.entry.device, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                        Text(
                            view.entry.codename,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    fields.forEach { field -> FieldCell(view, field) }
                }
                Row { EntryActionsRow(view, actionsFor(view), compact = true) }
                HorizontalRule()
            }
        }
    }
}

@Composable
private fun HorizontalRule() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
}

@Composable
private fun FirmwareCarousel(
    views: List<EntryView>,
    fields: List<HubField>,
    actionsFor: (EntryView) -> EntryActions,
) {
    Column {
        Text(stringResource(R.string.hub_layout_carousel), style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            views.forEach { view ->
                Card(
                    Modifier.width(240.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = glassCardColors(),
                ) {
                    Column(Modifier.padding(14.dp).animateContentSize()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BrandBadge(view.entry.brand)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                view.entry.device,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        FieldStrip(view, fields)
                        Spacer(Modifier.height(8.dp))
                        EntryActionsRow(view, actionsFor(view), compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun TextFirmwareRow(view: EntryView, fields: List<HubField>, actions: EntryActions) {
    Column(Modifier.padding(horizontal = 4.dp, vertical = 8.dp).animateContentSize()) {
        Text(view.entry.device, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        val parts = mutableListOf<String>()
        fields.forEach { field -> parts.add(fieldLabel(field) + ": " + fieldValue(view, field)) }
        Text(
            parts.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EntryActionsRow(view, actions, compact = true)
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ImportQueueCard(item: FirmwareImportQueue.Item) {
    Card(
        shape = MaterialTheme.shapes.extraLarge,
        colors = glassCardColors(MaterialTheme.colorScheme.surfaceContainerLow),
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
        Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)).glassWell(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = tint)
    }
}
