package top.trumeet.mipushframework.main.subpage

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import top.trumeet.ui.theme.Theme
import java.util.Date

/** Exercises the same history list and Paging source as the app without writing device history. */
@RunWith(AndroidJUnit4::class)
class EventHistoryPaginationTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun userOpensHistoryAndScrollsToEarlierRecords() {
        val latest = (30L downTo 11L).map(::record)
        val older = listOf(record(10), record(9))
        compose.setContent {
            Theme {
                EventList(
                    onClick = {},
                    getEvents = { beforeId, _ ->
                        when (beforeId) {
                            null -> latest
                            11L -> older
                            else -> emptyList()
                        }
                    },
                    query = "",
                    packageName = "com.xiaomi.xmsf"
                )
            }
        }
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Record 30").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Record 30").assertIsDisplayed()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Record 9"))
        compose.onNodeWithText("Record 9").assertIsDisplayed()
    }

    private fun record(id: Long) = EventInfoForDisplay(
        id, "com.xiaomi.xmsf", emptySet(), "", Date(0), "Record $id", "History fixture"
    )
}
