package moe.gbxin.noticefixrev.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import moe.gbxin.noticefixrev.R
import moe.gbxin.noticefixrev.ui.components.TooltipIconButton

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    Apps(R.string.tab_apps, Icons.Outlined.Apps),
    Pack(R.string.tab_pack, Icons.Outlined.Palette),
    Settings(R.string.tab_settings, Icons.Outlined.Settings),
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(Tab.Apps) }
    var searching by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) { viewModel.refresh() }

    if (showLicense) {
        LicenseScreen(onBack = { showLicense = false })
        return
    }

    if (searching) {
        AppSearchScreen(
            state = state,
            onToggleApp = viewModel::setAppEnabled,
            onBack = { searching = false },
        )
        return
    }

    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbar.showSnackbar(message)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        // The app bar consumes the status bar inset itself; list content adds the
        // gesture bar inset explicitly so it can scroll underneath.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(
                        when (tab) {
                            Tab.Apps -> stringResource(R.string.app_name)
                            Tab.Pack -> stringResource(R.string.tab_pack)
                            Tab.Settings -> stringResource(R.string.tab_settings)
                        },
                        maxLines = 1,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    if (tab == Tab.Apps) {
                        TooltipIconButton(
                            icon = Icons.Filled.Search,
                            label = stringResource(R.string.action_search),
                            onClick = { searching = true },
                            // Top bar: anchor the tooltip below so the status bar can't clip it.
                            anchorBelow = true,
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { entry ->
                    val label = stringResource(entry.labelRes)
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = tab == entry,
                        onClick = { tab = entry },
                        // Tooltip lives on the icon so NavigationBarItem stays the direct
                        // child of the bar (wrapping it breaks the bar's own layout).
                        icon = {
                            TooltipBox(
                                positionProvider = TooltipDefaults
                                    .rememberPlainTooltipPositionProvider(),
                                tooltip = { PlainTooltip { Text(label) } },
                                state = rememberTooltipState(),
                            ) {
                                Icon(entry.icon, contentDescription = null)
                            }
                        },
                        // Labels only while the destination is selected, per M3 guidance.
                        label = { Text(label) },
                        alwaysShowLabel = false,
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.Apps -> AppListScreen(
                state = state,
                onApplyAll = viewModel::applyAll,
                onToggleMaster = viewModel::setMasterEnabled,
                onToggleApp = viewModel::setAppEnabled,
                onUsePackIcon = viewModel::usePackIcon,
                onResetIcon = viewModel::resetIcon,
                loadAppliedIcon = viewModel::appliedIcon,
                contentPadding = padding,
            )

            Tab.Pack -> IconPackScreen(
                state = state,
                loadBitmap = viewModel::packBitmap,
                contentPadding = padding,
            )

            Tab.Settings -> SettingsScreen(
                state = state,
                onRequestScope = viewModel::requestScope,
                onShowLicense = { showLicense = true },
                contentPadding = padding,
            )
        }
    }
}
