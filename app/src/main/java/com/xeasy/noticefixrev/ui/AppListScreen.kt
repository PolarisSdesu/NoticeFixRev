package com.xeasy.noticefixrev.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.xeasy.noticefixrev.R
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
        if (query.isBlank()) state.apps
        else state.apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (!state.serviceAvailable) {
            ServiceBanner(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }

        MasterCard(
            enabled = state.masterEnabled,
            onToggle = onToggleMaster,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
            placeholder = { Text(stringResource(R.string.search_hint)) },
            trailingIcon = {
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, null)
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                top = 4.dp,
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            items(filtered, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    enabled = app.packageName in state.enabledApps,
                    onToggle = { onToggleApp(app.packageName, it) },
                    onClick = { selected = app },
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
                onReset = {
                    onResetIcon(sheet.packageName)
                },
            )
        }
    }
}

@Composable
private fun MasterCard(enabled: Boolean, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.master_enabled),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    stringResource(R.string.master_enabled_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun ServiceBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.service_unavailable),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Text(
                stringResource(R.string.service_unavailable_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
        }
    }
}

@Composable
private fun AppRow(
    app: AppEntry,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        leadingContent = {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
        },
        headlineContent = {
            Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                app.packageName,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Switch(checked = enabled, onCheckedChange = onToggle)
        },
    )
    HorizontalDivider(Modifier.padding(start = 72.dp), thickness = 0.5.dp)
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
        val bitmap = preview
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier
                    .size(96.dp)
                    .padding(8.dp),
            )
        } else {
            Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier.size(96.dp),
            )
        }

        Spacer(Modifier.height(12.dp))
        Text(app.label, style = MaterialTheme.typography.titleLarge)
        Text(
            app.packageName,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(20.dp))

        ListItem(
            headlineContent = { Text(stringResource(R.string.icon_section)) },
            supportingContent = {
                Text(
                    if (preview == null) stringResource(R.string.icon_none) else "已使用自定义图标",
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            trailingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onPick) {
                        Icon(Icons.Outlined.AddPhotoAlternate, stringResource(R.string.action_choose_icon))
                    }
                    IconButton(onClick = onReset, enabled = preview != null) {
                        Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.action_reset_icon))
                    }
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        FilledTonalButton(onClick = { onToggle(!enabled) }, modifier = Modifier.fillMaxWidth()) {
            Text(if (enabled) "对此应用停用修复" else "对此应用启用修复")
        }

        Spacer(Modifier.height(4.dp))
        Text(
            "选择图片后会自动启用该应用的修复。",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
