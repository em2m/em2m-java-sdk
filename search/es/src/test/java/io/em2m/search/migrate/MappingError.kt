package io.em2m.search.migrate

import io.em2m.search.es2.models.Es2Mapping
import io.em2m.search.es8.models.index.Es8MappingProperty

open class MappingError(open val index: String,
                        open val oldMapping: Es2Mapping,
                        open val newMapping: Es8MappingProperty,
                        open val exception: Exception)
