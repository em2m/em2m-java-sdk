package io.em2m.search.migrate

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es2.operations.es2ConvertMappings
import io.em2m.search.es8.models.index.Es8Mapping
import io.em2m.search.es8.models.index.properties.Es8GeoPointProperty
import kotlin.test.Test
import kotlin.test.assertNotNull

class Es2ToEs8SchemaParityTest {

    private val objectMapper = jacksonObjectMapper()

    // <editor-fold desc="strings">
    @Test
    fun `string analyzed`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                    "name": {
                        "type": "string"
                    }
                }
            }
        """.trimIndent()
        val es2Mapping: Es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping, objectMapper)
        val nameProp = es8Mapping.properties["name"]
        assert(nameProp?.type == "text")
    }

    @Test
    fun `string not_analyzed`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                        "name": {
                            "type": "string",
                            "index": "not_analyzed"
                        }
                    }
            }
        """.trimIndent()
        val es2Mapping: Es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping, objectMapper)
        val nameProp = es8Mapping.properties["name"]
        assert(nameProp?.type == "keyword")
    }
    // </editor-fold>

    // <editor-fold desc="integer">
    @Test
    fun `integer standard`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                    "size": {
                        "type": "integer"
                    }
                }
            }
        """.trimIndent()
        val es2Mapping: Es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping, objectMapper)
        val sizeProp = es8Mapping.properties["size"]
        assert(sizeProp?.type == "integer")
    }
    // </editor-fold>

    // <editor-fold desc="double">
    @Test
    fun `double standard`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                    "miles": {
                        "type": "double"
                    }
                }
            }
        """.trimIndent()
        val es2Mapping: Es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping, objectMapper)
        val milesProp = es8Mapping.properties["miles"]
        assert(milesProp?.type == "double")
    }
    // </editor-fold>

    // <editor-fold desc="long">
    @Test
    fun `long standard`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                    "hairs": {
                        "type": "long"
                    }
                }
            }
        """.trimIndent()
        val es2Mapping: Es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping, objectMapper)
        val hairsProp = es8Mapping.properties["hairs"]
        assert(hairsProp?.type == "long")
    }
    // </editor-fold>

    // <editor-fold desc="date">
    @Test
    fun `date standard`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                    "birthday": {
                        "type": "date",
                        "format": "yyyy/MM/dd"
                    }
                }
            }
        """.trimIndent()
        val es2Mapping: Es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping, objectMapper)
        val birthday = es8Mapping.properties["birthday"]
        assert(birthday?.type == "date")
        assert(birthday?.format == "yyyy/MM/dd")
    }
    // </editor-fold>

    // <editor-fold desc="geo_point">
    @Test
    fun geo_point() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                   "center": {
                        "type": "geo_point",
                        "lat_lon": true,
                        "geohash": true,
                        "geohash_prefix": true,
                        "geohash_precision": 10
                  }
                }
            }
        """.trimIndent()
        val es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping)
        val centerProp = es8Mapping.properties["center"]
        assert(centerProp?.type == "geo_point")
    }

    @Test
    fun `geo_point property mapping`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                   "center": {
                        "type": "geo_point",
                        "lat_lon": false,
                        "geohash": false,
                        "geohash_prefix": false,
                        "geohash_precision": 100
                  }
                }
            }
        """.trimIndent()
        val es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping)
        val centerProp = es8Mapping.properties["center"] as? Es8GeoPointProperty
        assert(centerProp?.type == "geo_point")
    }

    @Test
    fun `geo_point nested`() {
        val input = """
            {
                "index" : "Es2ToEs8SchemaParityTest.index",
                "type"  : "Es2ToEs8SchemaParityTest.type",
                "properties": {
                   "address" : {
                        "type": "object",
                        "properties": {
                            "center": {
                                "type": "geo_point",
                                "lat_lon": false,
                                "geohash": false,
                                "geohash_prefix": false,
                                "geohash_precision": 100
                            }
                        }
                   }
                }
            }
        """.trimIndent()
        val es2Mapping = assertNotNull(es2ConvertMappings(input),
            "Null result in es2ConvertMappings")
        val es8Mapping: Es8Mapping = migrateEs2ToEs8(es2Mapping)
        val centerProp = es8Mapping.properties["address.center"] as? Es8GeoPointProperty
        assert(centerProp?.type == "geo_point")
    }
    // </editor-fold>
}
