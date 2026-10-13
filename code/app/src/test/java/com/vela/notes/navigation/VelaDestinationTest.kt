package com.vela.notes.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class VelaDestinationTest {
    @Test
    fun routes_areUnique() {
        val routes = VelaDestination.entries.map(VelaDestination::route)
        assertEquals(routes.size, routes.distinct().size)
    }
}
