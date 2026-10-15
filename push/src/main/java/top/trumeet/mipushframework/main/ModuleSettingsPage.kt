package top.trumeet.mipushframework.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import com.xiaomi.xmsf.R
import kotlinx.coroutines.launch
import top.trumeet.mipushframework.component.NavigationRow
import top.trumeet.mipushframework.component.PageColumn
import top.trumeet.mipushframework.component.SettingsGroup
import top.trumeet.mipushframework.component.SettingsItem
import top.trumeet.mipushframework.component.SettingsRow
import top.trumeet.ui.theme.Layout

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleSettings(onBack: () -> Unit) {
    val controller = LocalModuleController.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.module_save_error)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.module_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        PageColumn(contentPadding = padding) {
            ModuleStatusGroup()
            SettingsItem(title = stringResource(R.string.module_refresh)) {
                scope.launch { controller.read(refresh = true) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(Layout.LineGap)) {
                SettingsGroup(title = stringResource(R.string.module_qq_settings)) {
                    listOf(
                        Triple(Api.QQ_BADGE, R.string.module_qq_badge, R.string.module_qq_badge_summary),
                        Triple(Api.QQ_GEOMETRY, R.string.module_qq_geometry, R.string.module_qq_geometry_summary),
                        Triple(Api.QQ_SUMMARY, R.string.module_qq_summary, R.string.module_qq_summary_summary)
                    ).forEach { (key, title, summary) ->
                        val checked = controller.snapshot?.getBoolean(key, true) ?: true
                        SettingsItem(
                            title = stringResource(title),
                            summary = stringResource(summary),
                            checked = checked,
                            enabled = controller.snapshot != null && !controller.saving
                        ) {
                            scope.launch {
                                if (!controller.set(key, !checked)) snackbar.showSnackbar(saveError)
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(R.string.module_qq_scope),
                    modifier = Modifier.padding(horizontal = Layout.CardPadding),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModuleStatusGroup(showSettingsLink: Boolean = false) {
    val controller = LocalModuleController.current
    val state = controller.snapshot
    val navigate = LocalPageNavigation.current
    SettingsGroup(title = stringResource(R.string.module_status)) {
        if (state == null) {
            SettingsRow(
                title = stringResource(if (controller.error == null) R.string.module_checking else R.string.module_not_ready),
                summary = stringResource(controller.error ?: R.string.module_checking_summary)
            )
        } else {
            SettingsRow(
                title = stringResource(R.string.module_status_xmsf),
                summary = stringResource(when {
                    !state.getBoolean(Api.XMSF_LOADED) -> R.string.module_xmsf_missing
                    !state.getBoolean(Api.XMSF_CONFIG_READY) -> R.string.module_config_pending
                    else -> R.string.module_loaded
                })
            )
            SettingsRow(
                title = stringResource(R.string.module_status_system),
                summary = stringResource(if (state.getBoolean(Api.SYSTEM_READY)) R.string.module_ready else R.string.module_system_missing)
            )
            SettingsRow(
                title = stringResource(R.string.module_status_style),
                summary = stringResource(when {
                    !state.getBoolean(Api.BADGE_READY) || !state.getBoolean(Api.GEOMETRY_READY) -> R.string.module_style_missing
                    !state.getBoolean(Api.SYSTEM_UI_CONFIG_READY) -> R.string.module_config_pending
                    else -> R.string.module_ready
                })
            )
        }
        if (showSettingsLink) {
            NavigationRow(title = stringResource(R.string.module_settings)) { navigate(Pages.Module) }
        }
    }
}
