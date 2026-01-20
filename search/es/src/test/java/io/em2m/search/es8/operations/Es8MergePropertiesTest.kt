package io.em2m.search.es8.operations

import kotlin.test.Test

class Es8MergePropertiesTest {

    data class Cat(val name: String, val legs: Int)

    data class Table(val legs: Int, val drawers: Boolean)

    @Test
    fun `combine cat and table into a new creature`() {
        // This is an extreme example
        val cat = Cat("whiskers", 3)
        val table = Table(4, false)

        val catMapping = es8GenerateMappingProperties(cat)
        val tableMapping = es8GenerateMappingProperties(table)

        val mergedMapping = es8MergeProperties(catMapping, tableMapping)

        println(mergedMapping)
    }


}
