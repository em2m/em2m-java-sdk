package io.em2m.search.core.model

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.Assert
import org.junit.Test

class QueryMarshallerTest : Assert() {

    val mapper = jacksonObjectMapper()

    @Test
    fun testWriteString() {
        val q = AndQuery(listOf(TermQuery("key", "value")))
        val json = mapper.writeValueAsString(q)
        assertNotNull(json)
    }

    @Test
    fun test() {

        val request = SearchRequest(
                offset = 0,
                limit = 10,
                fields = listOf(Field("id"), Field("title")),
                fieldSet = "test",
                sorts = listOf(DocSort("timestamp", Direction.Descending)),
                headers = mapOf("echo" to true),
                query = (AndQuery(
                        listOf(
                                NativeQuery("""{"properties.vin": "3C4PDCAB4GT233400}"""),
                                NativeQuery("select * from derp group by date"),
                                NativeQuery(mapper.readTree("""{"properties.vin": "3C4PDCAB4GT233400"}""")),
                                RangeQuery(field = "voltage", lt = 10),
                                NotQuery(listOf(
                                        TermQuery("make", "Ford"),
                                        RegexQuery("make", "^Chev.*"),
                                        PrefixQuery("make", "Chev"),
                                        PhraseQuery("dealer", listOf("Jim", "Bob"))
                                )),
                                MatchAllQuery(),
                                ExistsQuery("make")
                        ))),
                aggs = listOf(TermsAgg("year"), TermsAgg("make"), TermsAgg("model")))


        val json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request)
        println(json)
        assertNotNull(json)

        val deser = mapper.readValue(json, SearchRequest::class.java)
        assertNotNull(deser)
        println(mapper.writeValueAsString(deser))
    }
}

