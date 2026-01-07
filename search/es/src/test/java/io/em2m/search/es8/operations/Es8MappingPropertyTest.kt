package io.em2m.search.es8.operations

import io.em2m.search.es8.models.index.Es8MappingProperty
import kotlin.test.Test

class Es8MappingPropertyTest {

    @Test
    fun `add property`() {
        val props = Es8MappingProperty()
        props["hello"] = Es8MappingProperty(type = "keyword")
        assert("hello" in props)
    }

    @Test
    fun `add path property`() {
        val props = Es8MappingProperty()
        props["hello.world"] = Es8MappingProperty(type = "keyword")
        assert("hello" in props)
        assert("hello.world" in props)
    }

    @Test
    fun `add nested property`() {
        val props = Es8MappingProperty()
        props["extra.hello.world"] = Es8MappingProperty(type = "keyword")
        assert("extra" in props)
        assert("extra.hello" in props)
        assert("extra.hello.world" in props)
    }

    @Test
    fun `get property`() {
        val props = Es8MappingProperty()
        props["hello"] = Es8MappingProperty(type = "keyword")
        val property = props["hello"]
        assert(property?.type == "keyword")
    }

    @Test
    fun `get path property`() {
        val props = Es8MappingProperty()
        props["hello.world"] = Es8MappingProperty(type = "keyword")
        val property = props["hello.world"]
        assert(property?.type == "keyword")
    }

    @Test
    fun `remove property`() {
        val props = Es8MappingProperty()
        props["hello"] = Es8MappingProperty(type = "keyword")
        assert("hello" in props)
        props.removePath("hello")
        assert("hello" !in props)
    }

    @Test
    fun `remove nested property`() {
        val props = Es8MappingProperty()
        props["hello.world"] = Es8MappingProperty(type = "keyword")
        assert("hello.world" in props)
        props.removePath("hello.world")
        assert("hello.world" !in props)
    }

}
