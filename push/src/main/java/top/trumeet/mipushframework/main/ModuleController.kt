package top.trumeet.mipushframework.main

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import com.xiaomi.xmsf.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

val LocalModuleController = staticCompositionLocalOf<ModuleController> {
    error("Module controller must be provided by the main host")
}

/** UI state always comes from the module's acknowledged snapshot, not a second preference store. */
class ModuleController(private val context: Context) {
    var snapshot by mutableStateOf<Bundle?>(null)
        private set
    var error by mutableStateOf<Int?>(null)
        private set
    var saving by mutableStateOf(false)
        private set
    private val mutex = Mutex()

    suspend fun read(refresh: Boolean = false) = mutex.withLock {
        try {
            val result = request(if (refresh) Api.REFRESH else Api.STATUS)
            if (result?.getInt(Api.API_VERSION) != Api.VERSION) {
                snapshot = null
                error = R.string.module_update_required
            } else {
                snapshot = result
                error = null
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            snapshot = null
            error = R.string.module_unavailable
        }
    }

    suspend fun set(key: String, value: Boolean): Boolean = mutex.withLock {
        saving = true
        try {
            val result = request(Api.SET, key, Bundle().apply { putBoolean(key, value) })
            check(result?.getInt(Api.API_VERSION) == Api.VERSION)
            snapshot = result
            error = null
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        } finally {
            saving = false
        }
    }

    private suspend fun request(method: String, key: String? = null, extras: Bundle? = null) =
        withContext(Dispatchers.IO) {
            context.contentResolver.call(Uri.parse(Api.URI), method, key, extras)
        }
}

@Composable
fun rememberModuleController(): ModuleController {
    val context = LocalContext.current.applicationContext
    val controller = remember(context) { ModuleController(context) }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(controller, owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var first = true
            callbackFlow {
                val observer = object : ContentObserver(null) {
                    override fun onChange(selfChange: Boolean) { trySend(Unit) }
                }
                val registered = try {
                    context.contentResolver.registerContentObserver(Uri.parse(Api.URI), false, observer)
                    true
                } catch (_: Exception) {
                    // With no compatible module installed, the initial read supplies the UI error.
                    false
                }
                trySend(Unit)
                awaitClose {
                    if (registered) context.contentResolver.unregisterContentObserver(observer)
                }
            }.conflate().collect {
                controller.read(refresh = first)
                first = false
            }
        }
    }
    return controller
}
