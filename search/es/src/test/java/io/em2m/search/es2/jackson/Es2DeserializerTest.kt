package io.em2m.search.es2.jackson

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es2.models.Es2MappingProperty
import kotlin.test.Test

class Es2DeserializerTest {

    @Test
    fun deserialize() {
        val jsonString = """
            {
              "properties" : {
                "actor" : {
                  "properties" : {
                    "_id" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "device" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "displayName" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "fuelLevel" : {
                      "type" : "long"
                    },
                    "make" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "model" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "odometer" : {
                      "type" : "double"
                    },
                    "orgPath" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "organization" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "serialNumber" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "type" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "vin" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "voltage" : {
                      "type" : "double"
                    },
                    "year" : {
                      "type" : "long"
                    }
                  }
                },
                "displayName" : {
                  "type" : "string"
                },
                "location" : {
                  "properties" : {
                    "_id" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "category" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "displayName" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "other" : {
                      "properties" : {
                        "category" : {
                          "type" : "string",
                          "index" : "not_analyzed"
                        }
                      }
                    },
                    "type" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    }
                  }
                },
                "published" : {
                  "type" : "date",
                  "format" : "strict_date_optional_time||epoch_millis"
                },
                "summary" : {
                  "type" : "string",
                  "index" : "not_analyzed"
                },
                "target" : {
                  "properties" : {
                    "max" : {
                      "type" : "double"
                    },
                    "min" : {
                      "type" : "long"
                    },
                    "type" : {
                      "type" : "string",
                      "index" : "not_analyzed"
                    },
                    "value" : {
                      "type" : "double"
                    }
                  }
                },
                "title" : {
                  "type" : "string",
                  "index" : "not_analyzed"
                },
                "type" : {
                  "type" : "string",
                  "index" : "not_analyzed"
                }
              }
            }
        """.trimIndent()
        val objectMapper = jacksonObjectMapper()
        val property = objectMapper.readValue(jsonString, Es2MappingProperty::class.java)
        println(property)
    }

}
