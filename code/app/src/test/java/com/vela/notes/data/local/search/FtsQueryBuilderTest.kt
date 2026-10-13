package com.vela.notes.data.local.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FtsQueryBuilderTest {
    @Test
    fun emptyQuery_returnsNull() {
        assertNull(FtsQueryBuilder.build("   "))
    }

    @Test
    fun terms_areQuotedPrefixedAndCombined() {
        assertEquals(
            "ghi* chu*",
            FtsQueryBuilder.build(" ghi   chu "),
        )
    }

    @Test
    fun punctuation_cannotBecomeFtsSyntax() {
        assertEquals(
            "say* hello*",
            FtsQueryBuilder.build("say \"hello\""),
        )
    }
}
