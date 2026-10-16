package top.trumeet.mipushframework.main

import org.junit.Assert.assertEquals
import org.junit.Test

class PersonalizationCountsTest {
    @Test
    fun customizationsAreCountedForReadyAndMixedResults() {
        listOf(
            listOf(true) to PersonalizationCounts(1, 0),
            listOf(false) to PersonalizationCounts(0, 1),
            listOf(true, true) to PersonalizationCounts(2, 0),
            listOf(true, false, true) to PersonalizationCounts(2, 1)
        ).forEach { (states, expected) ->
            assertEquals(expected, countPersonalizations(states))
        }
    }
}
