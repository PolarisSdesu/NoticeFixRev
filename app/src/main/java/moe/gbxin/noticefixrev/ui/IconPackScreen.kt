package moe.gbxin.noticefixrev.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import moe.gbxin.noticefixrev.R
import moe.gbxin.noticefixrev.ui.components.NoticeSearchField
import moe.gbxin.noticefixrev.ui.components.navigationBarBottomPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Browser for every icon the bundled pack ships — laid out like a launcher's icon pack
 * screen: a dense grid of themed glyphs. Installed entries are tappable and toggle the
 * per-app fix; the rest are browse-only so the pack's coverage stays visible.
 */
@Composable
fun IconPackScreen(
    state: UiState,
    onToggleApp: (String, Boolean) -> Unit,
    loadBitmap: suspend (String) -> ImageBitmap?,
    contentPadding: PaddingValues,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(state.packItems, query) {
        if (query.isBlank()) {
            state.packItems
        } else {
            state.packItems.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(contentPadding)) {
        NoticeSearchField(
            value = query,
            onValueChange = { query = it },
            hint = stringResource(R.string.pack_search_hint),
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            stringResource(R.string.pack_tap_hint),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    stringResource(R.string.pack_no_result),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Column
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 12.dp,
                end = 12.dp,
                top = 8.dp,
                bottom = 16.dp + navigationBarBottomPadding(),
            ),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(filtered, key = { it.packageName }) { item ->
                PackCell(
                    item = item,
                    enabled = item.packageName in state.enabledApps,
                    loadBitmap = loadBitmap,
                    onToggle = { onToggleApp(item.packageName, it) },
                )
            }
        }
    }
}

@Composable
private fun PackCell(
    item: PackItemUi,
    enabled: Boolean,
    loadBitmap: suspend (String) -> ImageBitmap?,
    onToggle: (Boolean) -> Unit,
) {
    val bitmap by produceState<ImageBitmap?>(initialValue = null, item.packageName) {
        value = withContext(Dispatchers.IO) { loadBitmap(item.packageName) }
    }

    val glyph = ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = item.installed) { onToggle(!enabled) }
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                )
                .then(
                    if (enabled) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            val image = bitmap
            if (image != null) {
                Image(
                    bitmap = image,
                    contentDescription = null,
                    colorFilter = glyph,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            item.title,
            style = MaterialTheme.typography.labelMedium,
            color = if (item.installed) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
