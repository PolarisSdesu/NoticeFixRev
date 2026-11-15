package com.xeasy.noticefixrev.data

/**
 * Keys shared by the module UI (writer) and the hooked SystemUI process (reader).
 * Both sides go through libxposed remote preferences, so there is no world-readable
 * file hack and no root involved.
 */
object PrefKeys {
    const val GROUP = "config"

    const val MASTER_ENABLED = "master_enabled"
    const val ENABLED_APPS = "enabled_apps"

    /** Bumped whenever an icon file is rewritten, so readers know to refresh. */
    const val ICON_REVISION = "icon_revision"

    fun iconFile(pkg: String): String = "icon_${pkg.replace('.', '_')}.png"
}
