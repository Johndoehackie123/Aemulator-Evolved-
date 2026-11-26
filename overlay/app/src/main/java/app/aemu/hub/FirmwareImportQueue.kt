package app.aemu.hub

import android.content.Context
import android.net.Uri
import app.aemu.core.ImageStore
import app.aemu.importer.Importer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/**
 * Sequential import queue for files downloaded by the Firmware Hub.
 *
 * The upstream Importer is reused unchanged; this only serialises jobs and keeps a visible
 * queue so several one-tap downloads can be queued for import without overlapping imports
 * (two concurrent Importer instances would fight over cache and free storage).
 */
object FirmwareImportQueue {

    data class Item(
        val id: String,
        val file: String,
        val status: Status,
        val step: String = "",
        val progress: Float = -1f,
        val message: String? = null,
        val imageId: String? = null,
    )

    enum class Status { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items
    private var active: Importer? = null
    private var worker: Job? = null

    /** Set by the Hub screen so the library refreshes after an auto-imported firmware appears. */
    var globalOnImported: (() -> Unit)? = null

    @Synchronized
    fun enqueue(
        ctx: Context,
        entry: FirmwareCatalog.Entry,
        file: File,
        onImported: (() -> Unit)? = globalOnImported,
    ) {
        if (_items.value.any { it.id == entry.id && it.status.let { s -> s == Status.QUEUED || s == Status.RUNNING } }) return
        _items.update { it + Item(entry.id, entry.fileName, Status.QUEUED, "Waiting") }
        if (worker?.isActive != true) {
            worker = scope.launch { drain(ctx.applicationContext, onImported) }
        }
    }

    fun remove(id: String) {
        if (items.value.firstOrNull()?.id == id && items.value.firstOrNull()?.status == Status.RUNNING) cancel()
        _items.update { list -> list.filterNot { it.id == id && it.status != Status.RUNNING } }
    }

    @Synchronized
    fun cancel() {
        active?.cancelled = true
        worker?.cancel()
        worker = null
        _items.update { list -> list.map { if (it.status == Status.RUNNING) it.copy(status = Status.CANCELLED, step = "Cancelled") else it } }
        active = null
    }

    fun clearFinished() {
        _items.update { list -> list.filter { it.status == Status.QUEUED || it.status == Status.RUNNING } }
    }

    fun isQueued(id: String): Boolean = _items.value.any {
        it.id == id && (it.status == Status.QUEUED || it.status == Status.RUNNING)
    }

    private suspend fun drain(ctx: Context, onImported: (() -> Unit)?) {
        while (true) {
            val next = _items.value.firstOrNull { it.status == Status.QUEUED } ?: break
            val entry = FirmwareCatalog.find(next.id) ?: run {
                update(next.id) { copy(status = Status.FAILED, message = "Unknown catalog entry") }
                continue
            }
            val file = FirmwareDownloads.finishedFile(ctx, entry)
            if (file == null) {
                update(next.id) { copy(status = Status.FAILED, message = "File missing or not checksum-verified") }
                continue
            }
            try {
                update(next.id) { copy(status = Status.RUNNING, step = "Starting import") }
                val importer = Importer(ctx,
                    onProgress = { step, p -> update(next.id) { copy(step = step, progress = p) } },
                    log = { _ -> /* the upstream importer logs a lot; keep only steps in the UI */ })
                active = importer
                val img = importer.import(Uri.fromFile(file), entry.fileName)
                update(next.id) { copy(status = Status.DONE, step = "Imported", progress = 1f, imageId = img.id, message = null) }
                onImported?.invoke()
            } catch (e: CancellationException) {
                update(next.id) { copy(status = Status.CANCELLED, step = "Cancelled") }
                break
            } catch (e: Throwable) {
                update(next.id) { copy(status = Status.FAILED, message = e.message ?: e.toString()) }
            } finally {
                active = null
            }
        }
    }

    private fun update(id: String, f: Item.() -> Item) {
        _items.update { list -> list.map { if (it.id == id) f(it) else it } }
    }

    fun imageCount(ctx: Context): Int = runCatching { ImageStore.list(ctx).size }.getOrDefault(0)
}
