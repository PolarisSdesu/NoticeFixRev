package com.xeasy.noticefixrev.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import android.util.Log
import org.json.JSONArray

/**
 * One adapted notification icon from the community icon pack.
 *
 * The pack ships 46–50 px white silhouettes plus the brand colour the glyph should be
 * tinted with, which is exactly what `Notification.smallIcon` expects.
 */
data class PackIcon(
    val packageName: String,
    val appName: String,
    /** ARGB tint from the pack, or 0 when the entry has none. */
    val color: Int,
    val png: ByteArray,
) {
    fun bitmap(): Bitmap? = runCatching { BitmapFactory.decodeByteArray(png, 0, png.size) }.getOrNull()
}

/**
 * Bundled icon pack, sourced from `fankes/AndroidNotifyIconAdapt` (AGPL-3.0, see
 * `assets/iconpack/LICENSE-AndroidNotifyIconAdapt.txt`).
 *
 * Three files are merged: the all-app table plus the ColorOS and MIUI vendor tables.
 * Every entry is used as-is — the pack's own `isEnabled` / `isEnabledAll` flags are not
 * consumed by the reference implementation either, and the pack only lists apps whose
 * icons actually need fixing.
 */
class IconPack private constructor(private val entries: Map<String, PackIcon>) {

    val size: Int get() = entries.size

    fun get(pkg: String): PackIcon? = entries[pkg]

    fun contains(pkg: String): Boolean = entries.containsKey(pkg)

    companion object {
        private const val TAG = "NoticeFixRev"

        private val ASSETS = listOf(
            "iconpack/app.json",
            "iconpack/coloros.json",
            "iconpack/miui.json",
        )

        @Volatile
        private var cached: IconPack? = null

        fun load(context: Context): IconPack = cached ?: synchronized(this) {
            cached ?: parse(context.applicationContext).also { cached = it }
        }

        private fun parse(context: Context): IconPack {
            val entries = LinkedHashMap<String, PackIcon>()

            for (asset in ASSETS) {
                runCatching {
                    val text = context.assets.open(asset).use { it.readBytes().decodeToString() }
                    val array = JSONArray(text)
                    for (index in 0 until array.length()) {
                        val obj = array.optJSONObject(index) ?: continue
                        val pkg = obj.optString("packageName")
                        if (pkg.isEmpty()) continue

                        val encoded = obj.optString("iconBitmap")
                        if (encoded.isEmpty()) continue

                        val png = runCatching { Base64.decode(encoded, Base64.DEFAULT) }.getOrNull()
                            ?: continue

                        entries[pkg] = PackIcon(
                            packageName = pkg,
                            appName = obj.optString("appName", pkg),
                            color = parseColor(obj.optString("iconColor")),
                            png = png,
                        )
                    }
                    Log.i(TAG, "icon pack $asset: ${entries.size} entries")
                }.onFailure {
                    Log.w(TAG, "cannot read icon pack $asset", it)
                }
            }

            return IconPack(entries)
        }

        private fun parseColor(raw: String): Int =
            if (raw.isEmpty()) 0 else runCatching { Color.parseColor(raw) }.getOrDefault(0)
    }
}
