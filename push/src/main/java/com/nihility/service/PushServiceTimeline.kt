package com.nihility.service

import com.nihility.service.XMPushServiceListener.ConnectionStatus

/**
 * Wall-clock anchors behind the two durations the dashboard shows.
 *
 * The push service and the UI share one process — the manifest declares `XMPushService` without
 * `android:process` — so plain fields are enough and no IPC is involved.
 *
 * Both are written from the service side, never from the UI: the activity only observes status
 * while it is alive, so a timestamp taken there would restart counting every time the dashboard
 * is opened. They reset when the process is killed, which is exactly what these durations mean.
 */
object PushServiceTimeline {

    /** When the push service was created, or null while this process has not had one. */
    @Volatile
    var serviceStartedAt: Long? = null
        private set

    /** When the live connection was established, or null while there is none. */
    @Volatile
    var connectedSince: Long? = null
        private set

    @JvmStatic
    fun onServiceCreated() {
        serviceStartedAt = System.currentTimeMillis()
    }

    @JvmStatic
    fun onConnectionStatusChanged(status: ConnectionStatus) {
        if (status == ConnectionStatus.connected) {
            // The SDK reports the same status more than once; keep the first timestamp.
            if (connectedSince == null) {
                connectedSince = System.currentTimeMillis()
            }
        } else {
            connectedSince = null
        }
    }
}
