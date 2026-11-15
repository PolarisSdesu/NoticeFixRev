package com.xeasy.noticefixrev.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeasy.noticefixrev.R
import com.xeasy.noticefixrev.ui.components.SectionLabel
import com.xeasy.noticefixrev.ui.components.SettingsActionRow
import com.xeasy.noticefixrev.ui.components.SettingsCard
import com.xeasy.noticefixrev.ui.components.SettingsInfoRow
import com.xeasy.noticefixrev.ui.components.navigationBarBottomPadding

@Composable
fun SettingsScreen(
    state: UiState,
    onRequestScope: () -> Unit,
    contentPadding: PaddingValues,
) {
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

        item { SectionLabel(stringResource(R.string.section_about)) }
        item {
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.about_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item {
            SettingsCard {
                SettingsActionRow(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.about_license),
                    supporting = "AGPL-3.0 · NoticeFix by xudazhu1",
                    onClick = { },
                )
            }
        }
    }
}
