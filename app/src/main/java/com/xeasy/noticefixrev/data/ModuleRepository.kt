package com.xeasy.noticefixrev.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.xeasy.noticefixrev.NoticeFixRevApp
import io.github.libxposed.service.XposedService
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * App side of the module: writes preferences and icons into the framework storage,
 * which the hooked SystemUI process then reads through libxposed remote preferences.
 */
class ModuleRepository(private val context: Context) {

    private val service: XposedService?
        get() = NoticeFixRevApp.instance

    val isServiceAvailable: Boolean get() = service != null

    val frameworkName: String? get() = runCatching { service?.frameworkName }.getOrNull()
    val frameworkVersion: String? get() = runCatching { service?.frameworkVersion }.getOrNull()
    val apiVersion: Int? get() = runCatching { service?.apiVersion }.getOrNull()

    fun scope(): List<String> = runCatching { service?.scope }.getOrNull().orEmpty()

    private fun prefs(): SharedPreferences? =
        runCatching { service?.getRemotePreferences(PrefKeys.GROUP) }.getOrNull()

    fun masterEnabled(): Boolean =
        prefs()?.getBoolean(PrefKeys.MASTER_ENABLED, false) ?: false

    fun enabledApps(): Set<String> =
        prefs()?.getStringSet(PrefKeys.ENABLED_APPS, emptySet())?.toSet().orEmpty()

    fun setMasterEnabled(enabled: Boolean) {
        prefs()?.edit()?.putBoolean(PrefKeys.MASTER_ENABLED, enabled)?.commit()
    }

    fun setAppEnabled(pkg: String, enabled: Boolean) {
        val current = enabledApps().toMutableSet()
        if (enabled) current.add(pkg) else current.remove(pkg)
        prefs()?.edit()?.putStringSet(PrefKeys.ENABLED_APPS, current)?.commit()
        if (!enabled) deleteIcon(pkg)
    }

    fun requestScope(onResult: (Boolean, String?) -> Unit) {
        val current = service
        if (current == null) {
            onResult(false, "LSPosed 服务未连接")
            return
        }
        runCatching {
            current.requestScope(
                listOf("com.android.systemui"),
                object : XposedService.OnScopeEventListener {
                    override fun onScopeRequestApproved(approved: List<String>) {
                        onResult(true, null)
                    }

                    override fun onScopeRequestFailed(message: String) {
                        onResult(false, message)
                    }
                },
            )
        }.onFailure { onResult(false, it.message) }
    }

    /** Decodes, normalizes and stores a user-picked image; returns true on success. */
    fun saveIcon(pkg: String, uri: Uri): Boolean = runCatching {
        val source = decode(uri) ?: return@runCatching false
        if (!writeIcon(pkg, normalize(source))) return@runCatching false
        // A hand-picked image keeps its own colours, so drop the pack tint.
        commitAll(mapOf(pkg to 0))
        true
    }.getOrDefault(false)

    /** Applies one icon-pack entry (white silhouette + pack tint). */
    fun applyPackIcon(pkg: String, icon: PackIcon): Boolean =
        applyPackIcons(mapOf(pkg to icon)) > 0

    /**
     * Applies many pack entries with a single preferences write. First-launch seeding
     * uses this so 50+ apps cost one binder round-trip instead of one per app.
     */
    fun applyPackIcons(icons: Map<String, PackIcon>): Int = runCatching {
        val applied = icons.filter { (pkg, icon) -> writeIcon(pkg, icon.png) }
        if (applied.isNotEmpty()) {
            commitAll(applied.values.associate { it.packageName to it.color })
        }
        applied.size
    }.getOrDefault(0)

    fun deleteIcon(pkg: String) {
        runCatching { service?.deleteRemoteFile(PrefKeys.iconFile(pkg)) }
        runCatching { File(mirrorDir(), PrefKeys.iconFile(pkg)).delete() }

        val colors = iconColors().toMutableMap()
        if (colors.remove(pkg) != null) {
            prefs()?.edit()?.putString(PrefKeys.ICON_COLORS, encodeColors(colors))?.commit()
        }
        bumpRevision()
    }

    fun loadLocalIcon(pkg: String): Bitmap? = runCatching {
        val file = File(mirrorDir(), PrefKeys.iconFile(pkg))
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }.getOrNull()

    fun iconRevision(): Int = prefs()?.getInt(PrefKeys.ICON_REVISION, 0) ?: 0

    fun iconColors(): Map<String, Int> = decodeColors(
        prefs()?.getString(PrefKeys.ICON_COLORS, null),
    )

    fun packSeeded(): Boolean = prefs()?.getBoolean(PrefKeys.PACK_SEEDED, false) ?: false

    fun markPackSeeded() {
        prefs()?.edit()?.putBoolean(PrefKeys.PACK_SEEDED, true)?.commit()
    }

    /** Writes the PNG into the framework storage and the local preview mirror. */
    private fun writeIcon(pkg: String, bytes: ByteArray): Boolean = runCatching {
        val target = service?.openRemoteFile(PrefKeys.iconFile(pkg)) ?: return false
        target.use { descriptor ->
            FileOutputStream(descriptor.fileDescriptor).use { output ->
                output.write(bytes)
                output.flush()
            }
        }
        File(mirrorDir(), PrefKeys.iconFile(pkg)).outputStream().use { it.write(bytes) }
        true
    }.getOrDefault(false)

    /** Enables the given apps and stores their tints + revision in one commit. */
    private fun commitAll(colors: Map<String, Int>) {
        val enabled = enabledApps().toMutableSet().apply { addAll(colors.keys) }
        val merged = iconColors().toMutableMap()
        colors.forEach { (pkg, color) -> if (color != 0) merged[pkg] = color else merged.remove(pkg) }

        prefs()?.edit()
            ?.putStringSet(PrefKeys.ENABLED_APPS, enabled)
            ?.putString(PrefKeys.ICON_COLORS, encodeColors(merged))
            ?.putInt(PrefKeys.ICON_REVISION, iconRevision() + 1)
            ?.commit()
    }

    private fun encodeColors(colors: Map<String, Int>): String =
        colors.entries
            .sortedBy { it.key }
            .joinToString("\n") { "${it.key}|${it.value.toUInt().toString(16)}" }

    private fun decodeColors(raw: String?): Map<String, Int> =
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

    private fun bumpRevision() {
        prefs()?.edit()?.putInt(PrefKeys.ICON_REVISION, iconRevision() + 1)?.commit()
    }

    private fun mirrorDir(): File = File(context.filesDir, "icons").apply { mkdirs() }

    private fun decode(uri: Uri): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, _, _ ->
                decoder.isMutableRequired = true
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
        }
    }.getOrNull()

    /** Square, 192px, transparent PNG — cheap for SystemUI to decode. */
    private fun normalize(source: Bitmap): ByteArray {
        val scaled = Bitmap.createScaledBitmap(source, ICON_SIZE, ICON_SIZE, true)
        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
            if (scaled !== source) scaled.recycle()
            source.recycle()
            out.toByteArray()
        }
    }

    private companion object {
        const val ICON_SIZE = 192
    }
}
