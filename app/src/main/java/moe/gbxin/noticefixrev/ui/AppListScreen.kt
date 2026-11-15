package moe.gbxin.noticefixrev.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.gbxin.noticefixrev.R
import moe.gbxin.noticefixrev.ui.components.NoticeLoading
import moe.gbxin.noticefixrev.ui.components.NoticeSearchField
import moe.gbxin.noticefixrev.ui.components.SectionLabel
import moe.gbxin.noticefixrev.ui.components.SettingsCard
import moe.gbxin.noticefixrev.ui.components.SettingsSwitchRow
import moe.gbxin.noticefixrev.ui.components.navigationBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Home: the whole page is one scrolling list, so the switch, the apply button, the
 * search field and the app rows share a single scroll surface instead of stacking a
 * header column above a half-height list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onApplyAll: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onUsePackIcon: (String) -> Unit,
    onResetIcon: (String) -> Unit,
    loadAppliedIcon: suspend (String) -> ImageBitmap?,
    contentPadding: PaddingValues,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppEntry?>(null) }

    val filtered = remember(state.apps, query) {
        if (query.isBlank()) {
            state.apps
        } else {
            state.apps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 16.dp + navigationBarBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!state.serviceAvailable) {
            item { ServiceBanner() }
        }

        item { SectionLabel(stringResource(R.string.section_fix)) }
        item {
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.master_enabled),
                    checked = state.masterEnabled,
                    supporting = stringResource(R.string.pack_summary, state.apps.size),
                    onToggle = { onToggleMaster(!state.masterEnabled) },
                )
            }
        }

        item {
            Button(
                onClick = onApplyAll,
                enabled = state.serviceAvailable && state.apps.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Check, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.action_apply_all, state.apps.size),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        item {
            NoticeSearchField(
                value = query,
                onValueChange = { query = it },
                hint = stringResource(R.string.search_hint),
                onRefresh = onRefresh,
            )
        }

        when {
            state.loading -> item {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    NoticeLoading()
                }
            }

            filtered.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(
                            if (state.apps.isEmpty()) R.string.pack_empty else R.string.search_no_result,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            else -> items(filtered, key = { it.packageName }) { app ->
                AppCard(
                    app = app,
                    enabled = app.packageName in state.enabledApps,
                    onToggle = { onToggleApp(app.packageName, it) },
                    onClick = { selected = app },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    val sheet = selected
    if (sheet != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            AppDetailSheet(
                app = sheet,
                enabled = sheet.packageName in state.enabledApps,
                iconRevision = state.iconRevision,
                loadAppliedIcon = loadAppliedIcon,
                onToggle = { onToggleApp(sheet.packageName, it) },
                onUsePack = { onUsePackIcon(sheet.packageName) },
                onReset = { onResetIcon(sheet.packageName) },
            )
        }
    }
}

@Composable
private fun ServiceBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.service_unavailable),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.service_unavailable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun AppCard(
    app: AppEntry,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PackGlyph(app, 40)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

/**
 * Pack glyphs are white silhouettes, so they are drawn on a tinted circle and tinted
 * with `onPrimaryContainer`: the icon then follows the theme (light/dark/dynamic).
 */
@Composable
private fun PackGlyph(app: AppEntry, size: Int) {
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            bitmap = app.packIcon,
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
            modifier = Modifier.size((size * 0.5f).dp),
        )
    }
}

@Composable
private fun AppDetailSheet(
    app: AppEntry,
    enabled: Boolean,
    iconRevision: Int,
    loadAppliedIcon: suspend (String) -> ImageBitmap?,
    onToggle: (Boolean) -> Unit,
    onUsePack: () -> Unit,
    onReset: () -> Unit,
) {
    val applied by produceState<ImageBitmap?>(initialValue = null, app.packageName, iconRevision) {
        value = withContext(Dispatchers.IO) { loadAppliedIcon(app.packageName) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                bitmap = applied ?: app.packIcon,
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer),
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(app.label, style = MaterialTheme.typography.titleLarge, maxLines = 1)
        Text(
            app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        SettingsCard {
            SettingsSwitchRow(
                icon = Icons.Outlined.Palette,
                title = stringResource(R.string.icon_section),
                checked = enabled,
                supporting = stringResource(R.string.icon_supporting, app.label),
                onToggle = { onToggle(!enabled) },
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onUsePack, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.Palette, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_use_pack))
            }
            if (enabled) {
                TextButton(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Undo, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.action_reset_icon))
                }
            }
        }
    }
}
