import io.em2m.utils.coerce
import org.junit.Test
import java.util.*
import java.util.Calendar.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class CoerceTest {

    @Test
    fun testNumbers() {
        assertEquals("45", "45".coerce()!!)
        assertEquals(45, "45".coerce()!!)
        assertEquals("45", 45.coerce()!!)
        assertEquals(45.0F, "45".coerce()!!)
        assertEquals(45.0, "45".coerce()!!)
        assertEquals(0x2D, "45".coerce()!!)
    }

    @Test
    fun testDate() {
        val date: Calendar = "2010-01-05".coerce()!!
        assertEquals(2010, date.get(YEAR))
        assertEquals(1 - 1, date.get(MONTH))
        assertEquals(5, date.get(DAY_OF_MONTH))
    }

    @Test
    fun testNullable() {
        val value = null
        assertEquals("45", value.coerce("45"))
        assertEquals(45, value.coerce(45))
        assertEquals("45", value.coerce("45"))
        assertEquals(45.0F, value.coerce(45.0F))
        assertEquals(45.0, value.coerce(45.0))
        assertEquals(0x2D, value.coerce(0x2D))
    }
}