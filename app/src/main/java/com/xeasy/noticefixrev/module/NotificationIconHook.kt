package com.xeasy.noticefixrev.module

import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule

/**
 * Rewrites the small icon of incoming notifications before SystemUI ranks them.
 *
 * `NotificationListener.onNotificationPosted` is the single choke point: it fires for
 * both new posts and updates, and it runs before the notification row is inflated, so
 * replacing `notification.smallIcon` here is enough for the status bar, the shade and
 * the keyguard.
 */
internal class NotificationIconHook(private val module: XposedModule) {

    fun install(classLoader: ClassLoader) {
        val listenerClass = Class.forName(
            "com.android.systemui.statusbar.NotificationListener",
            false,
            classLoader,
        )

        val onPosted = listenerClass.getDeclaredMethod(
            "onNotificationPosted",
            StatusBarNotification::class.java,
            NotificationListenerService.RankingMap::class.java,
        )

        module.hook(onPosted)
            .setPriority(XposedInterface.PRIORITY_DEFAULT)
            .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
            .intercept { chain ->
                (chain.args.getOrNull(0) as? StatusBarNotification)?.let(::patch)
                chain.proceed()
            }

        module.log(Log.INFO, ModuleEntry.TAG, "hooked ${listenerClass.name}#onNotificationPosted")
    }

    private fun patch(sbn: StatusBarNotification) {
        val config = ConfigSnapshot.get()
        val pkg = sbn.packageName ?: return
        if (!config.shouldPatch(pkg)) return

        val bitmap = config.iconFor(pkg) ?: return
        runCatching {
            sbn.notification.smallIcon = Icon.createWithBitmap(bitmap)
        }.onFailure {
            module.log(Log.WARN, ModuleEntry.TAG, "cannot patch icon of $pkg", it)
        }
    }
}
