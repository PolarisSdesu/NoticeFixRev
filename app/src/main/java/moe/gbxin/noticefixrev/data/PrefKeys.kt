package moe.gbxin.noticefixrev.data

/**
 * Keys shared by the module UI (writer) and the hooked SystemUI process (reader).
 * Both sides go through libxposed remote preferences, so there is no world-readable
 * file hack and no root involved.
 */
object PrefKeys {
    const val GROUP = "config"

    const val MASTER_ENABLED = "master_enabled"
    const val ENABLED_APPS = "enabled_apps"

    /**
     * Tint written next to each icon as `package|argbHex`, one per line.
     *
     * The icon pack ships white silhouettes plus a brand colour; the hook mirrors the
     * original NoticeFix and sets `Notification.color` so SystemUI tints the glyph.
     */
    const val ICON_COLORS = "icon_colors"

    /** Set once the bundled icon pack has been applied to every installed match. */
    const val PACK_SEEDED = "pack_seeded"

    /** Bumped whenever an icon file is rewritten, so readers know to refresh. */
    const val ICON_REVISION = "icon_revision"

    fun iconFile(pkg: String): String = "icon_${pkg.replace('.', '_')}.png"
}
