package moe.gbxin.noticefixrev

import android.app.Application
import io.github.libxposed.service.XposedService
import io.github.libxposed.service.XposedServiceHelper
import java.util.concurrent.CopyOnWriteArraySet

class NoticeFixRevApp : Application(), XposedServiceHelper.OnServiceListener {

    override fun onCreate() {
        super.onCreate()
        XposedServiceHelper.registerListener(this)
    }

    override fun onServiceBind(service: XposedService) {
        instance = service
        listeners.forEach { it(service) }
    }

    override fun onServiceDied(service: XposedService) {
        if (instance === service) {
            instance = null
            listeners.forEach { it(null) }
        }
    }

    companion object {
        @Volatile
        var instance: XposedService? = null
            private set

        private val listeners = CopyOnWriteArraySet<(XposedService?) -> Unit>()

        fun addServiceListener(listener: (XposedService?) -> Unit) {
            listeners.add(listener)
            listener(instance)
        }

        fun removeServiceListener(listener: (XposedService?) -> Unit) {
            listeners.remove(listener)
        }
    }
}
