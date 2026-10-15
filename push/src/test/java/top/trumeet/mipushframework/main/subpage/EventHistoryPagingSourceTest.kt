package top.trumeet.mipushframework.main.subpage

import androidx.paging.PagingSource
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Date

class EventHistoryPagingSourceTest {
    @Test
    fun latestAndOlderPagesStayInOrderAndStopAtTheEnd() = runBlocking {
        val source = EventHistoryPagingSource { beforeId, _ ->
            when (beforeId) {
                null -> listOf(record(93), record(92), record(91))
                91L -> listOf(record(90), record(89))
                else -> error("Unexpected fixture cursor")
            }
        }
        val first = source.load(PagingSource.LoadParams.Refresh(null, 3, false))
            as PagingSource.LoadResult.Page<Long, EventInfoForDisplay>
        assertEquals(listOf(93L, 92L, 91L), first.data.map { it.id })
        assertEquals(91L, first.nextKey)
        val older = source.load(PagingSource.LoadParams.Append(requireNotNull(first.nextKey), 3, false))
            as PagingSource.LoadResult.Page<Long, EventInfoForDisplay>
        assertEquals(listOf(90L, 89L), older.data.map { it.id })
        assertNull(older.nextKey)
    }

    @Test
    fun pagingDiagnosticsShowOnlyTheRecordIdentity() {
        assertEquals(
            "EventInfoForDisplay(id=93, packageName=com.example.history)",
            record(93).toString()
        )
    }

    private fun record(id: Long) = EventInfoForDisplay(
        id, "com.example.history", emptySet(), "", Date(0), "Record $id", "History fixture"
    )
}
