package app.aemu.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.DrawerState
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.aemu.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Navigation style for the main app shell. CLASSIC preserves the original top-bar + FAB UI. */
enum class AppNavStyle { CLASSIC, BOTTOM, RAIL, DRAWER, TABS, SIDE }

/** Destinations reachable from every non-classic navigation style. */
enum class AppDestination { MACHINES, HUB, SETTINGS, UPDATES }

object AppNavSettings {
    private const val PREFS = "app_navigation"
    private const val KEY_STYLE = "style"

    private val _style = MutableStateFlow(AppNavStyle.CLASSIC)
    /** Observable so a change made in Settings updates an already-composed main screen. */
    val style: StateFlow<AppNavStyle> = _style.asStateFlow()

    fun load(ctx: Context): AppNavStyle {
        val saved = runCatching {
            AppNavStyle.valueOf(
                ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(KEY_STYLE, AppNavStyle.CLASSIC.name) ?: AppNavStyle.CLASSIC.name,
            )
        }.getOrDefault(AppNavStyle.CLASSIC)
        _style.value = saved
        return saved
    }

    fun set(ctx: Context, value: AppNavStyle) {
        ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_STYLE, value.name).apply()
        _style.value = value
    }
}

@Composable
fun navStyleLabel(style: AppNavStyle): String = when (style) {
    AppNavStyle.CLASSIC -> stringResource(R.string.as_nav_classic)
    AppNavStyle.BOTTOM -> stringResource(R.string.as_nav_bottom)
    AppNavStyle.RAIL -> stringResource(R.string.as_nav_rail)
    AppNavStyle.DRAWER -> stringResource(R.string.as_nav_drawer)
    AppNavStyle.TABS -> stringResource(R.string.as_nav_tabs)
    AppNavStyle.SIDE -> stringResource(R.string.as_nav_side)
}

@Composable
fun destinationLabel(destination: AppDestination): String = when (destination) {
    AppDestination.MACHINES -> stringResource(R.string.as_dest_machines)
    AppDestination.HUB -> stringResource(R.string.as_dest_hub)
    AppDestination.SETTINGS -> stringResource(R.string.as_dest_settings)
    AppDestination.UPDATES -> stringResource(R.string.as_dest_updates)
}

@Composable
fun destinationIcon(destination: AppDestination): ImageVector = when (destination) {
    AppDestination.MACHINES -> Icons.Rounded.Android
    AppDestination.HUB -> Icons.Rounded.CloudDownload
    AppDestination.SETTINGS -> Icons.Rounded.Settings
    AppDestination.UPDATES -> Icons.Rounded.SystemUpdate
}

@Composable
fun AppBottomBar(current: AppDestination, onSelect: (AppDestination) -> Unit) {
    NavigationBar {
        AppDestination.values().forEach { destination ->
            NavigationBarItem(
                selected = current == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destinationIcon(destination), null) },
                label = { Text(destinationLabel(destination)) },
            )
        }
    }
}

@Composable
fun AppSideNav(
    current: AppDestination,
    onSelect: (AppDestination) -> Unit,
    compact: Boolean = false,
) {
    NavigationRail(modifier = if (compact) Modifier.width(64.dp) else Modifier) {
        AppDestination.values().forEach { destination ->
            NavigationRailItem(
                selected = current == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destinationIcon(destination), null) },
                label = { Text(destinationLabel(destination)) },
            )
        }
    }
}

@Composable
fun AppDrawerContent(current: AppDestination, onSelect: (AppDestination) -> Unit) {
    ModalDrawerSheet(Modifier.width(300.dp)) {
        Text(
            stringResource(R.string.app_name),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp),
        )
        AppDestination.values().forEach { destination ->
            NavigationDrawerItem(
                label = { Text(destinationLabel(destination)) },
                selected = current == destination,
                onClick = { onSelect(destination) },
                icon = { Icon(destinationIcon(destination), null) },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
    }
}

@Composable
fun AppTopTabs(current: AppDestination, onSelect: (AppDestination) -> Unit) {
    val destinations = AppDestination.values()
    PrimaryScrollableTabRow(selectedTabIndex = destinations.indexOf(current)) {
        destinations.forEach { destination ->
            Tab(
                selected = current == destination,
                onClick = { onSelect(destination) },
                text = { Text(destinationLabel(destination)) },
                icon = { Icon(destinationIcon(destination), null) },
            )
        }
    }
}

/**
 * Shell used by every non-classic style. CLASSIC is handled by the original Scaffold in
 * MainActivity so the default screen is byte-for-byte the familiar top-bar + FAB layout.
 */
@Composable
fun AppNavigationShell(
    style: AppNavStyle,
    destination: AppDestination,
    onSelect: (AppDestination) -> Unit,
    drawerState: DrawerState,
    topBar: @Composable () -> Unit,
    fab: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    when (style) {
        AppNavStyle.BOTTOM -> Scaffold(
            topBar = topBar,
            bottomBar = { AppBottomBar(destination, onSelect) },
            floatingActionButton = fab,
        ) { pad -> content(pad) }

        AppNavStyle.TABS -> Scaffold(
            topBar = {
                Column {
                    topBar()
                    AppTopTabs(destination, onSelect)
                }
            },
            floatingActionButton = fab,
        ) { pad -> content(pad) }

        AppNavStyle.RAIL -> Row {
            AppSideNav(destination, onSelect)
            Scaffold(modifier = Modifier.weight(1f), topBar = topBar, floatingActionButton = fab) { pad -> content(pad) }
        }

        AppNavStyle.SIDE -> Row {
            AppSideNav(destination, onSelect, compact = true)
            Scaffold(modifier = Modifier.weight(1f), topBar = topBar, floatingActionButton = fab) { pad -> content(pad) }
        }

        AppNavStyle.DRAWER -> ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = { AppDrawerContent(destination, onSelect) },
        ) {
            Scaffold(topBar = topBar, floatingActionButton = fab) { pad -> content(pad) }
        }

        AppNavStyle.CLASSIC -> Scaffold(topBar = topBar, floatingActionButton = fab) { pad -> content(pad) }
    }
}
