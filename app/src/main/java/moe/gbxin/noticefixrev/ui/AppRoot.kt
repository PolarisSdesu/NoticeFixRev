package moe.gbxin.noticefixrev.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
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
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) { viewModel.refresh() }

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
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, null) },
                        // Labels only while the destination is selected, per M3 guidance.
                        label = { Text(stringResource(entry.labelRes)) },
                        alwaysShowLabel = false,
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.Apps -> AppListScreen(
                state = state,
                onRefresh = viewModel::refresh,
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
                onToggleApp = viewModel::setAppEnabled,
                loadBitmap = viewModel::packBitmap,
                contentPadding = padding,
            )

            Tab.Settings -> SettingsScreen(
                state = state,
                onRequestScope = viewModel::requestScope,
                contentPadding = padding,
            )
        }
    }
}
