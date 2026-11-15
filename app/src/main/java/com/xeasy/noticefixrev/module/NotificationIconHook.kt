package com.xeasy.noticefixrev.module

import android.app.Notification
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Rewrites the small icon of incoming notifications before SystemUI ranks them.
 *
 * `NotificationListener.onNotificationPosted` is the single choke point: it fires for
 * both new posts and updates, and it runs before the notification row is inflated, so
 * replacing `notification.smallIcon` here is enough for the status bar, the shade and
 * the keyguard.
 */
internal class NotificationIconHook(private val module: XposedModule) {

    /**
     * Since API 37 the public `Notification.smallIcon` field is gone: it lives in the
     * private `mSmallIcon`, and the only writer (`setSmallIcon(Icon)`) is `@hide`.
     * Both are `@UnsupportedAppUsage` greylist members, so reflection still reaches
     * them — the original NoticeFix patches the field the same way.
     */
    private val smallIconField: Field? = runCatching {
        Notification::class.java.getDeclaredField("mSmallIcon").apply { isAccessible = true }
    }.getOrNull()

    private val smallIconSetter: Method? = runCatching {
        Notification::class.java.getDeclaredMethod("setSmallIcon", Icon::class.java)
    }.getOrNull()

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

        module.log(
            Log.INFO,
            ModuleEntry.TAG,
            "hooked ${listenerClass.name}#onNotificationPosted " +
                "(mSmallIcon=${smallIconField != null}, setter=${smallIconSetter != null})",
        )
    }

    private fun patch(sbn: StatusBarNotification) {
        val config = ConfigSnapshot.get()
        val pkg = sbn.packageName ?: return
        if (!config.shouldPatch(pkg)) return

        val bitmap = config.iconFor(pkg) ?: return
        val notification = sbn.notification ?: return
        val icon = Icon.createWithBitmap(bitmap)
        runCatching {
            smallIconField?.set(notification, icon)
                ?: smallIconSetter?.invoke(notification, icon)
                ?: error("Notification#mSmallIcon is not reachable")
            // Pack glyphs are white silhouettes; hand SystemUI the brand tint exactly
            // like the original NoticeFix does via `notification.color`.
            config.colorFor(pkg)?.let { notification.color = it }
        }.onFailure {
            module.log(Log.WARN, ModuleEntry.TAG, "cannot patch icon of $pkg", it)
        }
    }
}
