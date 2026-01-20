package io.em2m.search

const val MAPPING_FILE_NAME     = "mapping"
const val MAPPINGS_KEY          = "mappings"
const val SETTINGS_FILE_NAME    = "settings"
const val ALIASES_FILE_NAME     = "aliases"
const val STATS_FILE_NAME       = "stats"

val ES_SCHEMA_FILE_NAMES        = listOf(MAPPING_FILE_NAME, SETTINGS_FILE_NAME, ALIASES_FILE_NAME, STATS_FILE_NAME)

val ES_ACCEPTED_TYPES           = listOf(
                                    "text",
                                    "keyword",
                                    "long",
                                    "unsigned_long",
                                    "integer",
                                    "short",
                                    "byte",
                                    "double",
                                    "float",
                                    "half_float",
                                    "scaled_float",
                                    "date",
                                    "date_nanos",
                                    "boolean",
                                    "binary",
                                    "object",
                                    "nested",
                                    "flattened",
                                    "geo_point",
                                    "geo_shape",
                                    "ip",
                                    "completion",
                                    "token_count",
                                    "percolator",
                                    "join",
                                    "rank_feature",
                                    "rank_features",
                                    "dense_vector",
                                    "sparse_vector",
                                    "search_as_you_type",
                                    "alias",
                                    "constant_keyword",
                                    "integer_range",
                                    "float_range",
                                    "long_range",
                                    "double_range",
                                    "date_range",
                                    "ip_range"
)
