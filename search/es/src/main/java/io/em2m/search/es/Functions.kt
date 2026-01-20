package io.em2m.search.es

fun isEsTypeNumeric(esType: String): Boolean {
    return esType in listOf("long", "integer", "short", "byte", "double", "float", "half_float", "scaled_float", "unsigned_long")
}

fun isEsTypeDate(esType: String): Boolean {
    return esType in listOf("date")
}

fun isEsLeafType(esType: String): Boolean {
    return isEsTypeNumeric(esType) || isEsTypeDate(esType)
}
