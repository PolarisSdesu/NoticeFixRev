package com.xeasy.noticefixrev.module

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.xeasy.noticefixrev.data.PrefKeys
import io.github.libxposed.api.XposedModule
import java.io.FileNotFoundException

/**
 * Immutable view of the module configuration, living inside the SystemUI process.
 *
 * Reading remote preferences is cheap but not free, so the snapshot is cached and
 * rebuilt whenever the module app writes something.
 */
class ConfigSnapshot private constructor(
    val masterEnabled: Boolean,
    val enabledApps: Set<String>,
    private val icons: Map<String, Bitmap>,
    private val colors: Map<String, Int>,
) {
    fun shouldPatch(pkg: String): Boolean = masterEnabled && pkg in enabledApps

    fun iconFor(pkg: String): Bitmap? = icons[pkg]

    /** Brand tint for the pack icon, or null to leave `Notification.color` alone. */
    fun colorFor(pkg: String): Int? = colors[pkg]

    companion object {
        private const val TAG = "NoticeFixRev"

        /** Preference writes land one key at a time; wait for the burst to finish. */
        private const val RELOAD_DEBOUNCE_MS = 250L

        @Volatile
        private var current = ConfigSnapshot(false, emptySet(), emptyMap(), emptyMap())

        /** Fingerprint of the values the current snapshot was built from. */
        @Volatile
        private var lastFingerprint: String? = null

        private val handler by lazy { Handler(Looper.getMainLooper()) }

        fun get(): ConfigSnapshot = current

        fun attach(module: XposedModule) {
            reload(module)
            runCatching {
                module.getRemotePreferences(PrefKeys.GROUP)
                    .registerOnSharedPreferenceChangeListener { _, _ ->
                        scheduleReload(module)
                    }
            }.onFailure {
                module.log(Log.WARN, TAG, "cannot observe remote preferences", it)
            }
        }

        /**
         * One commit touches several keys and each key fires the listener, so a naive
         * reload re-reads every icon file a dozen times per change. Coalesce the burst:
         * only the last event within the debounce window actually rebuilds the snapshot.
         */
        private fun scheduleReload(module: XposedModule) {
            runCatching {
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({ reload(module) }, RELOAD_DEBOUNCE_MS)
            }.onFailure {
                // No main looper (should not happen inside SystemUI) — reload inline.
                reload(module)
            }
        }

        private fun reload(module: XposedModule) {
            try {
                val prefs = module.getRemotePreferences(PrefKeys.GROUP)
                val master = prefs.getBoolean(PrefKeys.MASTER_ENABLED, false)
                val apps = prefs.getStringSet(PrefKeys.ENABLED_APPS, emptySet())
                    ?.toSet()
                    .orEmpty()
                val revision = prefs.getInt(PrefKeys.ICON_REVISION, -1)
                val colorsRaw = prefs.getString(PrefKeys.ICON_COLORS, null)

                // Debouncing collapses the burst; this also swallows late duplicates.
                val fingerprint = buildString {
                    append(master).append('|').append(revision).append('|')
                    append(apps.sorted().joinToString(",")).append('|')
                    append(colorsRaw.orEmpty())
                }
                if (fingerprint == lastFingerprint) return
                lastFingerprint = fingerprint

                val icons = buildMap {
                    if (master) {
                        for (pkg in apps) {
                            loadIcon(module, pkg)?.let { put(pkg, it) }
                        }
                    }
                }

                val colors = parseColors(colorsRaw)

                current = ConfigSnapshot(master, apps, icons, colors)
                module.log(Log.INFO, TAG, "config loaded: ${apps.size} apps, ${icons.size} icons")
            } catch (t: Throwable) {
                module.log(Log.ERROR, TAG, "failed to read config", t)
            }
        }

        /** Mirrors `ModuleRepository.encodeColors`: `package|argbHex` lines. */
        private fun parseColors(raw: String?): Map<String, Int> =
            raw.orEmpty()
                .lineSequence()
                .mapNotNull { line ->
                    val split = line.lastIndexOf('|')
                    if (split <= 0) return@mapNotNull null
                    val color = line.substring(split + 1).toUIntOrNull(16)?.toInt()
                        ?: return@mapNotNull null
                    line.substring(0, split) to color
                }
                .toMap()

        private fun loadIcon(module: XposedModule, pkg: String): Bitmap? = try {
            module.openRemoteFile(PrefKeys.iconFile(pkg)).use { descriptor ->
                BitmapFactory.decodeFileDescriptor(descriptor.fileDescriptor)
            }
        } catch (_: FileNotFoundException) {
            null
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "cannot decode icon for $pkg", t)
            null
        }
    }
}
