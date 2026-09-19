package top.trumeet.mipushframework.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nihility.service.XMPushServiceListener.ConnectionStatus

/**
 * The connection status reported by the push service, shared between the dashboard and whoever
 * else needs it. A null status means "not known (yet)".
 */
object ConnectionStatusHolder {
    var status: ConnectionStatus? by mutableStateOf(null)
        private set
    var host: String? by mutableStateOf(null)
        private set

    fun update(status: ConnectionStatus?, host: String?) {
        this.status = status
        this.host = host
    }
}
