package com.xeasy.noticefixrev.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeasy.noticefixrev.R
import com.xeasy.noticefixrev.ui.components.NoticeLoading
import com.xeasy.noticefixrev.ui.components.SectionLabel
import com.xeasy.noticefixrev.ui.components.SettingsCard
import com.xeasy.noticefixrev.ui.components.SettingsSwitchRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    state: UiState,
    onRefresh: () -> Unit,
    onToggleMaster: (Boolean) -> Unit,
    onToggleApp: (String, Boolean) -> Unit,
    onSaveIcon: (String, Uri) -> Unit,
    onResetIcon: (String) -> Unit,
    loadPreview: suspend (String) -> ImageBitmap?,
    contentPadding: PaddingValues,
) {
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<AppEntry?>(null) }
    var pendingPackage by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val pkg = pendingPackage
        if (uri != null && pkg != null) onSaveIcon(pkg, uri)
        pendingPackage = null
    }

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

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        if (!state.serviceAvailable) {
            ServiceBanner(Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionLabel(stringResource(R.string.section_fix))
            SettingsCard {
                SettingsSwitchRow(
                    icon = Icons.Outlined.NotificationsActive,
                    title = stringResource(R.string.master_enabled),
                    checked = state.masterEnabled,
                    supporting = stringResource(R.string.master_enabled_desc),
                    onToggle = { onToggleMaster(!state.masterEnabled) },
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        SearchField(
            value = query,
            onValueChange = { query = it },
            onRefresh = onRefresh,
        )

        Spacer(Modifier.height(4.dp))

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                NoticeLoading()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filtered, key = { it.packageName }) { app ->
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
                loadPreview = loadPreview,
                onToggle = { onToggleApp(sheet.packageName, it) },
                onPick = {
                    pendingPackage = sheet.packageName
                    picker.launch("image/*")
                },
                onReset = { onResetIcon(sheet.packageName) },
            )
        }
    }
}

@Composable
private fun ServiceBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
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
private fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onRefresh: () -> Unit,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(56.dp),
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 18.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { }),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                stringResource(R.string.search_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Outlined.Refresh, contentDescription = null)
            }
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
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
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

@Composable
private fun AppDetailSheet(
    app: AppEntry,
    enabled: Boolean,
    iconRevision: Int,
    loadPreview: suspend (String) -> ImageBitmap?,
    onToggle: (Boolean) -> Unit,
    onPick: () -> Unit,
    onReset: () -> Unit,
) {
    val preview by produceState<ImageBitmap?>(initialValue = null, app.packageName, iconRevision) {
        value = withContext(Dispatchers.IO) { loadPreview(app.packageName) }
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
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            val previewBitmap = preview
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
            } else {
                Image(
                    bitmap = app.icon,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                )
            }
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
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.icon_section),
                checked = enabled,
                supporting = if (preview == null) {
                    stringResource(R.string.icon_none)
                } else {
                    stringResource(R.string.icon_custom)
                },
                onToggle = { onToggle(!enabled) },
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onPick, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.AddPhotoAlternate, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_choose_icon))
            }
            OutlinedButton(
                onClick = onReset,
                enabled = preview != null,
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.action_reset_icon))
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.choose_icon_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
