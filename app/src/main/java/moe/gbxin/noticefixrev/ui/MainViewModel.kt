package moe.gbxin.noticefixrev.ui

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import moe.gbxin.noticefixrev.NoticeFixRevApp
import moe.gbxin.noticefixrev.R
import moe.gbxin.noticefixrev.data.IconPack
import moe.gbxin.noticefixrev.data.ModuleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Only apps the bundled pack actually covers are listed — an app without a pack glyph
 * has nothing to adapt, so it would just be noise.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    /** The app's own launcher icon, shown while a fix is disabled. */
    val icon: ImageBitmap,
    /** The pack glyph shown in the list and used by the hook. */
    val packIcon: ImageBitmap,
)

/**
 * One entry of the bundled pack, shown in the icon-pack browser. Installed apps keep
 * their real launcher label so the grid reads like the launcher's icon pack screen.
 */
data class PackItemUi(
    val packageName: String,
    val title: String,
    val installed: Boolean,
)

data class UiState(
    val loading: Boolean = true,
    val serviceAvailable: Boolean = false,
    val masterEnabled: Boolean = false,
    val enabledApps: Set<String> = emptySet(),
    val apps: List<AppEntry> = emptyList(),
    val packItems: List<PackItemUi> = emptyList(),
    /** Total entries in the bundled icon pack (all three tables merged). */
    val packTotal: Int = 0,
    val frameworkName: String? = null,
    val frameworkVersion: String? = null,
    val apiVersion: Int? = null,
    val iconRevision: Int = 0,
    val message: String? = null,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ModuleRepository(application)

    /** Pack glyphs are tiny (~46px); caching the decoded ones keeps grid scrolling cheap. */
    private val packBitmapCache = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val serviceListener: (io.github.libxposed.service.XposedService?) -> Unit = { refresh() }

    init {
        NoticeFixRevApp.addServiceListener(serviceListener)
    }

    fun refresh() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val pack = withContext(Dispatchers.IO) { IconPack.load(context) }
            val (apps, packItems) = withContext(Dispatchers.IO) { loadInstalled(context, pack) }

            _state.update {
                it.copy(
                    loading = false,
                    serviceAvailable = repo.isServiceAvailable,
                    masterEnabled = repo.masterEnabled(),
                    enabledApps = repo.enabledApps(),
                    apps = apps,
                    packItems = packItems,
                    packTotal = pack.size,
                    frameworkName = repo.frameworkName,
                    frameworkVersion = repo.frameworkVersion,
                    apiVersion = repo.apiVersion,
                    iconRevision = repo.iconRevision(),
                )
            }

            // First launch after installing the module: adapt everything at once, so the
            // module works without touching a single switch.
            seedPackIfNeeded(pack)
        }
    }

    fun setMasterEnabled(enabled: Boolean) {
        repo.setMasterEnabled(enabled)
        _state.update { it.copy(masterEnabled = enabled) }
        if (enabled) repo.requestScope { _, _ -> }
    }

    fun setAppEnabled(pkg: String, enabled: Boolean) {
        if (enabled) {
            usePackIcon(pkg)
            return
        }
        repo.setAppEnabled(pkg, false)
        _state.update { it.copy(enabledApps = it.enabledApps - pkg) }
    }

    /** One-tap apply: writes the pack glyph for every installed app that has an entry. */
    fun applyAll() {
        viewModelScope.launch {
            val applied = withContext(Dispatchers.IO) { applyPack() }
            _state.update {
                it.copy(
                    masterEnabled = repo.masterEnabled(),
                    enabledApps = repo.enabledApps(),
                    iconRevision = repo.iconRevision(),
                    message = getString(R.string.pack_applied, applied),
                )
            }
        }
    }

    /** (Re)applies the pack glyph for a single app. */
    fun usePackIcon(pkg: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                IconPack.load(getApplication()).get(pkg)?.let { repo.applyPackIcon(pkg, it) } ?: false
            }
            if (ok) {
                _state.update {
                    it.copy(
                        enabledApps = it.enabledApps + pkg,
                        iconRevision = repo.iconRevision(),
                    )
                }
            } else {
                _state.update { it.copy(message = getString(R.string.apply_failed)) }
            }
        }
    }

    /** Drops the page-provided icon and lets the app's own icon come back. */
    fun resetIcon(pkg: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.deleteIcon(pkg) }
            _state.update {
                it.copy(
                    enabledApps = it.enabledApps - pkg,
                    iconRevision = repo.iconRevision(),
                )
            }
        }
    }

    /** Lazily decoded pack glyph for the browser grid. */
    suspend fun packBitmap(pkg: String): ImageBitmap? {
        packBitmapCache[pkg]?.let { return it }
        return withContext(Dispatchers.IO) {
            IconPack.load(getApplication<Application>()).get(pkg)
                ?.bitmap()
                ?.asImageBitmap()
                ?.also { packBitmapCache[pkg] = it }
        }
    }

    /** Currently applied glyph (pack or otherwise), for the detail sheet preview. */
    suspend fun appliedIcon(pkg: String): ImageBitmap? = withContext(Dispatchers.IO) {
        repo.loadLocalIcon(pkg)?.asImageBitmap()
    }

    fun requestScope() {
        repo.requestScope { ok, reason ->
            val message = if (ok) {
                getString(R.string.scope_ok)
            } else {
                getString(R.string.scope_failed, reason ?: getString(R.string.value_none))
            }
            _state.update { it.copy(message = message) }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    override fun onCleared() {
        NoticeFixRevApp.removeServiceListener(serviceListener)
        super.onCleared()
    }

    private suspend fun seedPackIfNeeded(pack: IconPack) {
        if (repo.packSeeded() || !repo.isServiceAvailable) return
        val applied = withContext(Dispatchers.IO) { applyPack() }
        _state.update {
            it.copy(
                masterEnabled = repo.masterEnabled(),
                enabledApps = repo.enabledApps(),
                iconRevision = repo.iconRevision(),
            )
        }
        if (applied > 0) repo.requestScope { _, _ -> }
    }

    /** Applies every installed pack entry; must run on a background thread. */
    private fun applyPack(): Int {
        val context = getApplication<Application>()
        val pack = IconPack.load(context)
        val targets = context.packageManager.getInstalledApplications(0)
            .asSequence()
            .map { it.packageName }
            .filter { it != context.packageName }
            .mapNotNull { pkg -> pack.get(pkg)?.let { pkg to it } }
            .toMap()

        repo.markPackSeeded()
        if (targets.isEmpty()) return 0

        repo.setMasterEnabled(true)
        return repo.applyPackIcons(targets)
    }

    private fun getString(res: Int, vararg args: Any): String =
        getApplication<Application>().getString(res, *args)

    private fun loadInstalled(
        context: Context,
        pack: IconPack,
    ): Pair<List<AppEntry>, List<PackItemUi>> {
        val pm = context.packageManager
        val apps = ArrayList<AppEntry>()
        val labels = HashMap<String, String>()

        pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach { info ->
            if (info.packageName == context.packageName) return@forEach
            val entry = pack.get(info.packageName) ?: return@forEach
            val glyph = entry.bitmap() ?: return@forEach
            val label = pm.getApplicationLabel(info).toString()
            labels[info.packageName] = label
            apps += AppEntry(
                packageName = info.packageName,
                label = label,
                icon = loadAppIcon(pm, info),
                packIcon = glyph.asImageBitmap(),
            )
        }

        val items = pack.all()
            .map { entry ->
                val installedLabel = labels[entry.packageName]
                PackItemUi(
                    packageName = entry.packageName,
                    title = installedLabel ?: entry.appName,
                    installed = installedLabel != null,
                )
            }
            .sortedWith(compareBy({ !it.installed }, { it.title.lowercase() }))

        return apps.sortedBy { it.label.lowercase() } to items
    }

    private fun loadAppIcon(pm: PackageManager, info: ApplicationInfo): ImageBitmap =
        runCatching {
            pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap()
        }.getOrElse {
            android.graphics.Bitmap
                .createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
                .asImageBitmap()
        }
}
