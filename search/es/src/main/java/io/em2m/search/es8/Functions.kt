package io.em2m.search.es8

import io.em2m.search.es8.models.index.Es8MappingProperty

fun getPrimitiveEs8Type(cls: Class<*>): String = when (cls) {
    Int::class.javaPrimitiveType -> "integer"
    Long::class.javaPrimitiveType -> "long"
    Boolean::class.javaPrimitiveType -> "boolean"
    Float::class.javaPrimitiveType -> "float"
    Double::class.javaPrimitiveType -> "double"
    String::class.javaPrimitiveType -> "keyword"
    else -> "object"
}

fun getBoxedEs8Type(cls: Class<*>): String = when (cls) {
   Integer::class.java -> "integer"
   java.lang.Long::class.java -> "long"
   java.lang.Boolean::class.java -> "boolean"
   java.lang.Float::class.java -> "float"
   java.lang.Double::class.java -> "double"
   java.lang.String::class.java -> "keyword"
   else -> "object"
}

private fun canEs8TypeBeDouble(es8Type: String): Boolean {
    return es8Type in listOf("integer", "short", "byte", "float", "double")
}

private fun canEs8TypeBeKeyword(es8Type: String): Boolean {
    return es8Type in listOf("keyword", "wildcard", "text")
}

fun inferEs8Types(types: Collection<String>): Es8MappingProperty {
    if (types.size == 1) {
        // I mean, types wouldn't need to be inferred in this case unless there was a mapping issue
        return Es8MappingProperty(types.first())
    }
    // allow integers and floats to cast up to doubles
    return if (types.all(::canEs8TypeBeDouble)) {
        Es8MappingProperty("double")
    } else if (types.all(::canEs8TypeBeKeyword)) {
        Es8MappingProperty("keyword")
    } else {
        TODO("Multiple types, unknown resolution: $types")
    }
}
