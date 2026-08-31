package com.xiaoquexing.app.viewmodel

import com.xiaoquexing.app.data.entity.Record
import org.junit.Assert.assertEquals
import org.junit.Test

class FootprintAggregatorTest {
    @Test
    fun groupsByPlaceAndSkipsBlank() {
        val records = listOf(
            Record(id = 1, text = "a", locationName = "公园", gpEarned = 5, createdAt = 20),
            Record(id = 2, text = "b", locationName = "公园", gpEarned = 3, createdAt = 10),
            Record(id = 3, text = "c", locationName = "家里", gpEarned = 2, createdAt = 30),
            Record(id = 4, text = "d", locationName = null),
        )
        val places = groupFootprints(records)
        assertEquals(2, places.size)
        assertEquals("公园", places[0].name)
        assertEquals(2, places[0].count)
        assertEquals(8, places[0].gp)
        assertEquals("家里", places[1].name)
    }
}
