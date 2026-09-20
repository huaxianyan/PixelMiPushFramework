package com.nihility.service

/**
 * Wall-clock anchor behind the service uptime the dashboard shows.
 *
 * The push service and the UI share one process — the manifest declares `XMPushService` without
 * `android:process` — so a plain field is enough and no IPC is involved.
 *
 * It is written from the service side, never from the UI: the activity only observes status while
 * it is alive, so a timestamp taken there would restart counting every time the dashboard is
 * opened. It resets when the process is killed, which is exactly what the duration means.
 */
object PushServiceTimeline {

    /** When the push service was created, or null while this process has not had one. */
    @Volatile
    var serviceStartedAt: Long? = null
        private set

    @JvmStatic
    fun onServiceCreated() {
        serviceStartedAt = System.currentTimeMillis()
    }
}
