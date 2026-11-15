package com.xeasy.noticefixrev.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xeasy.noticefixrev.R
import com.xeasy.noticefixrev.data.ModuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class AppEntry(
    val packageName: String,
    val label: String,
    val icon: ImageBitmap,
)

data class UiState(
    val loading: Boolean = true,
    val serviceAvailable: Boolean = false,
    val masterEnabled: Boolean = false,
    val enabledApps: Set<String> = emptySet(),
    val apps: List<AppEntry> = emptyList(),
    val frameworkName: String? = null,
    val frameworkVersion: String? = null,
    val apiVersion: Int? = null,
    val iconRevision: Int = 0,
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ModuleRepository(application)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val serviceListener: (io.github.libxposed.service.XposedService?) -> Unit = { refresh() }

    init {
        com.xeasy.noticefixrev.NoticeFixRevApp.addServiceListener(serviceListener)
    }

    fun refresh() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { loadApps(getApplication()) }
            _state.update {
                it.copy(
                    loading = false,
                    serviceAvailable = repo.isServiceAvailable,
                    masterEnabled = repo.masterEnabled(),
                    enabledApps = repo.enabledApps(),
                    apps = apps,
                    frameworkName = repo.frameworkName,
                    frameworkVersion = repo.frameworkVersion,
                    apiVersion = repo.apiVersion,
                    iconRevision = repo.iconRevision(),
                )
            }
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        repo.setMasterEnabled(enabled)
        _state.update { it.copy(masterEnabled = enabled) }
        if (enabled) repo.requestScope { _, _ -> }
    }

    fun setAppEnabled(pkg: String, enabled: Boolean) {
        repo.setAppEnabled(pkg, enabled)
        _state.update {
            it.copy(enabledApps = if (enabled) it.enabledApps + pkg else it.enabledApps - pkg)
        }
    }

    fun saveIcon(pkg: String, uri: Uri) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { repo.saveIcon(pkg, uri) }
            if (ok) {
                _state.update {
                    it.copy(
                        enabledApps = it.enabledApps + pkg,
                        iconRevision = repo.iconRevision(),
                    )
                }
            } else {
                _state.update { it.copy(message = getApplication<Application>().getString(R.string.saved_failed)) }
            }
        }
    }

    fun resetIcon(pkg: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteIcon(pkg) }
            _state.update { it.copy(iconRevision = repo.iconRevision()) }
        }
    }

    suspend fun previewIcon(pkg: String): ImageBitmap? = withContext(Dispatchers.IO) {
        repo.loadLocalIcon(pkg)?.asImageBitmap()
    }

    fun requestScope() {
        repo.requestScope { ok, reason ->
            val message = if (ok) {
                getApplication<Application>().getString(R.string.scope_ok)
            } else {
                getApplication<Application>().getString(
                    R.string.scope_failed,
                    reason ?: getApplication<Application>().getString(R.string.value_none),
                )
            }
            _state.update { it.copy(message = message) }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    override fun onCleared() {
        com.xeasy.noticefixrev.NoticeFixRevApp.removeServiceListener(serviceListener)
        super.onCleared()
    }

    private fun loadApps(context: Context): List<AppEntry> {
        val pm = context.packageManager
        return pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { it.packageName != context.packageName }
            .map { info ->
                AppEntry(
                    packageName = info.packageName,
                    label = pm.getApplicationLabel(info).toString(),
                    icon = runCatching {
                        pm.getApplicationIcon(info)
                            .toBitmap(96, 96)
                            .asImageBitmap()
                    }.getOrElse {
                        android.graphics.Bitmap
                            .createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                            .asImageBitmap()
                    },
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
