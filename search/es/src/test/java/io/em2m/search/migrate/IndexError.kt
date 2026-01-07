package io.em2m.search.migrate

import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es8.models.index.Es8MappingProperty

open class IndexError(override val index: String,
                              override val oldMapping: Es2Mapping,
                              override val newMapping: Es8MappingProperty,
                              override val exception: Exception,
                              val entity: Any): MappingError(index, oldMapping, newMapping, exception)
