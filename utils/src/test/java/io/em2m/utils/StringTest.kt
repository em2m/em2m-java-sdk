package io.em2m.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StringTest {

    @Test
    fun toNonEmptyOrNull() {
        assertNull("".toNonEmptyOrNull())
        assertEquals(" ", " ".toNonEmptyOrNull())
    }

    @Test
    fun toNonBlankOrNull() {
        assertNull(" ".toNonBlankOrNull())
        assertEquals(null, " ".toNonBlankOrNull())
    }

}
