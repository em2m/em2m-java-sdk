package io.em2m.search.es8.models.index

import com.fasterxml.jackson.annotation.JsonIgnore
import io.em2m.search.es8.models.Es8Dynamic

data class Es8Mapping(@JsonIgnore val index: String,
                      val properties: Es8MappingProperty,
                      val dynamic: Es8Dynamic = Es8Dynamic.FALSE)
