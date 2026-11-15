package moe.gbxin.noticefixrev.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import moe.gbxin.noticefixrev.R
import moe.gbxin.noticefixrev.ui.components.SectionLabel
import moe.gbxin.noticefixrev.ui.components.SettingsActionRow
import moe.gbxin.noticefixrev.ui.components.SettingsCard
import moe.gbxin.noticefixrev.ui.components.SettingsInfoRow
import moe.gbxin.noticefixrev.ui.components.navigationBarBottomPadding
import moe.gbxin.noticefixrev.util.LANGUAGE_EN
import moe.gbxin.noticefixrev.util.LANGUAGE_JA
import moe.gbxin.noticefixrev.util.LANGUAGE_SYSTEM
import moe.gbxin.noticefixrev.util.LANGUAGE_ZH_CN
import moe.gbxin.noticefixrev.util.LANGUAGE_ZH_TW
import moe.gbxin.noticefixrev.util.SUPPORTED_APP_LANGUAGES
import moe.gbxin.noticefixrev.util.currentAppLanguage
import moe.gbxin.noticefixrev.util.setAppLanguage

@Composable
fun SettingsScreen(
    state: UiState,
    onRequestScope: () -> Unit,
    onShowLicense: () -> Unit,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    var showLanguage by remember { mutableStateOf(false) }
    val language = currentAppLanguage(context)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 4.dp,
            bottom = 32.dp + navigationBarBottomPadding(),
        ),
    ) {
        item { SectionLabel(stringResource(R.string.section_framework)) }
        item {
            SettingsCard {
                SettingsInfoRow(
                    stringResource(R.string.info_framework),
                    state.frameworkName ?: stringResource(R.string.value_none),
                )
                SettingsInfoRow(
                    stringResource(R.string.info_version),
                    state.frameworkVersion ?: stringResource(R.string.value_none),
                )
                SettingsInfoRow(
                    stringResource(R.string.info_api),
                    state.apiVersion?.let { "libxposed $it" }
                        ?: stringResource(R.string.value_none),
                )
                SettingsInfoRow(
                    stringResource(R.string.info_scope),
                    "com.android.systemui",
                )
                SettingsInfoRow(
                    stringResource(R.string.info_state),
                    if (state.serviceAvailable) {
                        stringResource(R.string.value_connected)
                    } else {
                        stringResource(R.string.value_disconnected)
                    },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.section_scope)) }
        item {
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.Shield,
                    title = stringResource(R.string.request_scope),
                    enabled = state.serviceAvailable,
                    supporting = stringResource(R.string.reboot_hint),
                    onClick = onRequestScope,
                )
            }
        }

        item { SectionLabel(stringResource(R.string.section_icon_pack)) }
        item {
            SettingsCard {
                SettingsInfoRow(
                    stringResource(R.string.info_pack_entries),
                    state.packTotal.toString(),
                )
                SettingsInfoRow(
                    stringResource(R.string.info_pack_matched),
                    state.apps.size.toString(),
                )
                Column(Modifier.padding(horizontal = 16.dp).padding(top = 4.dp, bottom = 14.dp)) {
                    Text(
                        stringResource(R.string.pack_source),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item { SectionLabel(stringResource(R.string.section_language)) }
        item {
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.Translate,
                    title = stringResource(R.string.language_title),
                    supporting = languageLabel(language),
                    onClick = { showLanguage = true },
                )
            }
        }

        item { SectionLabel(stringResource(R.string.section_about)) }
        item {
            // Two cards in one item, spaced explicitly — otherwise LazyColumn stacks
            // them flush against each other.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.about_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                SettingsCard {
                    SettingsActionRow(
                        icon = Icons.Outlined.Info,
                        title = stringResource(R.string.license_title),
                        supporting = "AGPL-3.0 · NoticeFix by xudazhu1",
                        onClick = onShowLicense,
                    )
                }
            }
        }
    }

    if (showLanguage) {
        AlertDialog(
            onDismissRequest = { showLanguage = false },
            title = { Text(stringResource(R.string.language_dialog_title)) },
            text = {
                Column {
                    SUPPORTED_APP_LANGUAGES.forEach { tag ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    setAppLanguage(context, tag)
                                    showLanguage = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = tag == language,
                                onClick = {
                                    setAppLanguage(context, tag)
                                    showLanguage = false
                                },
                            )
                            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                            Text(languageLabel(tag), style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguage = false }) {
                    Text(stringResource(R.string.action_close))
                }
            },
        )
    }
}

@Composable
private fun languageLabel(tag: String): String = stringResource(
    when (tag) {
        LANGUAGE_SYSTEM -> R.string.language_system
        LANGUAGE_ZH_CN -> R.string.language_zh_cn
        LANGUAGE_ZH_TW -> R.string.language_zh_tw
        LANGUAGE_JA -> R.string.language_ja
        LANGUAGE_EN -> R.string.language_english
        else -> R.string.language_system
    },
)
