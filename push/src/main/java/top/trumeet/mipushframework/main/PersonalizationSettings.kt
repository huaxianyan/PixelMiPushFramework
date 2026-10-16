package top.trumeet.mipushframework.main

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import com.xiaomi.xmsf.R

internal data class PersonalizationStatus(val ready: Boolean, @StringRes val summary: Int)

internal data class PersonalizationEntry(
    @StringRes val title: Int,
    val route: String,
    val status: (Bundle) -> PersonalizationStatus
)

/** Each customization is registered once, for both its settings entrance and status count. */
internal val personalizations = listOf(
    PersonalizationEntry(R.string.module_qq_settings, Pages.QqNotifications) { state ->
        when {
            !state.getBoolean(Api.XMSF_LOADED) -> PersonalizationStatus(false, R.string.module_xmsf_missing)
            !state.getBoolean(Api.SYSTEM_READY) -> PersonalizationStatus(false, R.string.module_system_missing)
            !state.getBoolean(Api.BADGE_READY) || !state.getBoolean(Api.GEOMETRY_READY) ->
                PersonalizationStatus(false, R.string.module_style_missing)
            !state.getBoolean(Api.XMSF_CONFIG_READY) || !state.getBoolean(Api.SYSTEM_UI_CONFIG_READY) ->
                PersonalizationStatus(false, R.string.module_config_pending)
            else -> PersonalizationStatus(true, R.string.module_ready)
        }
    }
)

internal data class PersonalizationCounts(val ready: Int, val notReady: Int)

internal fun countPersonalizations(readiness: List<Boolean>): PersonalizationCounts {
    val ready = readiness.count { it }
    return PersonalizationCounts(ready, readiness.size - ready)
}

@Composable
internal fun personalizationSummary(state: Bundle?): String {
    if (state == null) {
        return pluralStringResource(
            R.plurals.module_personalization_unknown, personalizations.size, personalizations.size
        )
    }
    val counts = countPersonalizations(personalizations.map { it.status(state).ready })
    val ready = pluralStringResource(R.plurals.module_personalization_ready, counts.ready, counts.ready)
    val notReady = pluralStringResource(
        R.plurals.module_personalization_not_ready, counts.notReady, counts.notReady
    )
    return when {
        counts.notReady == 0 -> ready
        counts.ready == 0 -> notReady
        else -> stringResource(R.string.module_personalization_mixed, ready, notReady)
    }
}
