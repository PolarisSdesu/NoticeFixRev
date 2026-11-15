package com.xeasy.noticefixrev.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.xeasy.noticefixrev.R

@Composable
fun SettingsScreen(
    state: UiState,
    onRequestScope: () -> Unit,
    contentPadding: PaddingValues,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
    ) {
        InfoCard(title = stringResource(R.string.framework_info)) {
            InfoRow("框架", state.frameworkName ?: "未连接")
            InfoRow("框架版本", state.frameworkVersion ?: "—")
            InfoRow("API", state.apiVersion?.let { "libxposed $it" } ?: "—")
            InfoRow("作用域", "com.android.systemui")
            InfoRow("状态", if (state.serviceAvailable) "已连接" else "未连接")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onRequestScope,
            enabled = state.serviceAvailable,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.request_scope))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.reboot_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        InfoCard(title = stringResource(R.string.about)) {
            Text(
                "Notice Fix Rev 1.0.0\n" +
                    "次世代通知图标修复模块，基于 libxposed API 101、Kotlin 与 Jetpack Compose 重建。\n" +
                    "原作 NoticeFix 由 xudazhu1 编写（AGPL-3.0）。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
