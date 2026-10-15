package top.trumeet.mipushframework.main

import android.net.Uri
import android.os.Bundle
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neko7ina.pixelmipush.api.ModuleContract as Api
import com.xiaomi.xmsf.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Requires a separately authorized test device with both candidates and all module scopes active. */
@RunWith(AndroidJUnit4::class)
class ModuleSettingsTest {
    @get:Rule val compose = createAndroidComposeRule<MainPage>()

    @Test
    fun userChangesEachQqOptionInModuleSettingsAndOtherChoicesArePreserved() {
        val context = compose.activity
        val uri = Uri.parse(Api.URI)
        fun read(): Bundle = requireNotNull(context.contentResolver.call(uri, Api.STATUS, null, null))
        // Discovery and host configuration loading finish asynchronously after the page starts.
        compose.waitUntil(10_000) {
            val status = read()
            status.getBoolean(Api.XMSF_CONFIG_READY) && status.getBoolean(Api.SYSTEM_UI_CONFIG_READY)
        }
        val original = read()
        assertTrue("Push service module scope must be active", original.getBoolean(Api.XMSF_LOADED))
        assertTrue("System notification enhancement must be ready", original.getBoolean(Api.SYSTEM_READY))
        assertTrue("System UI module scope must be active", original.getBoolean(Api.BADGE_READY))
        assertTrue("Conversation layout hook must be available", original.getBoolean(Api.GEOMETRY_READY))
        val expected = Api.OPTIONS.associateWith { original.getBoolean(it) }.toMutableMap()
        try {
            compose.onNodeWithText(context.getString(R.string.main_settings)).performClick()
            compose.onNodeWithText(context.getString(R.string.module_settings)).performScrollTo().performClick()
            listOf(
                Api.QQ_BADGE to R.string.module_qq_badge,
                Api.QQ_GEOMETRY to R.string.module_qq_geometry,
                Api.QQ_SUMMARY to R.string.module_qq_summary
            ).forEach { (key, title) ->
                expected[key] = !requireNotNull(expected[key])
                compose.onNodeWithText(context.getString(title)).performScrollTo().performClick()
                compose.waitUntil(10_000) {
                    val current = read()
                    current.getBoolean(key) == expected[key] &&
                        current.getBoolean(Api.XMSF_CONFIG_READY) && current.getBoolean(Api.SYSTEM_UI_CONFIG_READY)
                }
                val saved = read()
                expected.forEach { (option, value) -> assertEquals(option, value, saved.getBoolean(option)) }
            }
        } finally {
            // Restore actual device configuration, including when an assertion fails.
            Api.OPTIONS.forEach { key ->
                context.contentResolver.call(uri, Api.SET, key, Bundle().apply {
                    putBoolean(key, original.getBoolean(key))
                })
            }
        }
    }
}
