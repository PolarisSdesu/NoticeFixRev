package com.xeasy.noticefixrev.module

import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam

/**
 * The single entry point declared in [META-INF/xposed/java_init.list].
 *
 * libxposed instantiates this class once per process and then calls the lifecycle
 * callbacks. We only care about SystemUI, everything else is ignored.
 */
class ModuleEntry : XposedModule() {

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        log(
            Log.INFO,
            TAG,
            "loaded in ${param.processName} " +
                "(framework=$frameworkName/$frameworkVersion, api=$apiVersion)"
        )
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (param.packageName != SYSTEM_UI) return

        runCatching { ConfigSnapshot.attach(this) }
            .onFailure { log(Log.ERROR, TAG, "config init failed", it) }

        runCatching { NotificationIconHook(this).install(param.classLoader) }
            .onFailure { log(Log.ERROR, TAG, "NotificationListener hook failed", it) }
    }

    companion object {
        const val TAG = "NoticeFixRev"
        const val SYSTEM_UI = "com.android.systemui"
    }
}
