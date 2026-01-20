package io.em2m.search.es8.operations

import kotlin.test.Test

// Pass in an object mapping and generate a schema from it
class Es8GenerateSchemaTest {

    private data class TestSchema(
        val name: String,
        val age: Double,
        val hairs: Long,
        val pets: Int,
        val extra: Map<String, Any?>,
        val favoriteBooks: List<String>
    )

    @Test
    fun `generate es8 schema from a data class`() {
        // this is supposed to be deprecated, just testing optional class support over object
        val mapping = es8GenerateMappingProperties(TestSchema::class.java)
        println(mapping)
    }

    @Test
    fun `generate es8 schema from a data class object`() {
        val mapping = es8GenerateMappingProperties(
            TestSchema(
                name = "Jake",
                age = 1000.0,
                hairs = 1L,
                pets = 2,
                extra = mutableMapOf("hello" to "world", "goodbye" to "stranger"),
                favoriteBooks = mutableListOf("2001 A Space Odyssey")
            ),
            ignorePaths = setOf("extra.goodbye")
        )
        assert("extra" in mapping)
        assert("extra.hello" in mapping)
        println(mapping)
    }

}
