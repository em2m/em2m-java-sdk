package io.em2m.search.es8.operations

import io.em2m.search.EsPropertyConflictStrategy
import io.em2m.search.es8.inferEs8Types
import io.em2m.search.es8.models.Es8Dynamic
import io.em2m.search.es8.models.index.Es8MappingProperty

fun es8MergeProperties(vararg mappings: Es8MappingProperty,
          strategy: EsPropertyConflictStrategy = EsPropertyConflictStrategy.PERMISSIVE)
        : Es8MappingProperty {
    require(mappings.isNotEmpty()) { "mappings cannot be empty" }
    val propertyResolution = mutableMapOf<String, MutableSet<Es8MappingProperty>>()
    val allPropertyNames = mappings.flatMapTo(mutableSetOf()) {
        it.properties?.keys ?: emptyList()
    }
    allPropertyNames.forEach { propName -> propertyResolution[propName] = mutableSetOf() }
    mappings.forEach { mapping ->
        mapping.properties?.forEach { (propName, prop) ->
            propertyResolution[propName]?.add(prop)
        }
    }
    val allTypes = mappings.mapNotNullTo(mutableSetOf(), Es8MappingProperty::type)

    if (allPropertyNames.isEmpty() && allTypes.size > 1) {
        return inferEs8Types(allTypes)
    }
    if (allPropertyNames.isEmpty() && allTypes.size == 1) {
        return Es8MappingProperty(type=allTypes.first())
    }

    val flattenedProperties = mutableMapOf<String, Es8MappingProperty>()

    propertyResolution.forEach { (propName, props) ->
        flattenedProperties[propName] = if (props.size == 1) {
            props.first()
        } else {
            when (strategy) {
                EsPropertyConflictStrategy.EXCEPTION -> {
                    throw IllegalStateException("Properties don't match.")
                }
                EsPropertyConflictStrategy.LAST -> {
                    props.first()
                }
                EsPropertyConflictStrategy.FIRST -> {
                    props.last()
                }
                EsPropertyConflictStrategy.PERMISSIVE -> {
                    val types = props.mapNotNullTo(mutableSetOf(), Es8MappingProperty::type)
                    val numObjects = props.count { it.properties != null }
                    // TODO: Figure out format and dynamic
                    val format = props.firstNotNullOfOrNull(Es8MappingProperty::format)
                    val dynamic = Es8Dynamic.fromBoolean(props.any { it.dynamic == Es8Dynamic.TRUE })

                    if (types.size > 1) {
                        inferEs8Types(types).apply {
                            this.format = format
                            this.dynamic = dynamic
                        }
                    } else if (types.isEmpty() && numObjects > 0){

                        val subPropertyResolution = mutableMapOf<String, MutableSet<Es8MappingProperty>>()
                        val subPropertyNames = props.mapNotNullTo(mutableSetOf()) { prop ->
                            prop.properties?.keys
                        }.flatten()
                        subPropertyNames.forEach { subPropertyResolution[it] = mutableSetOf() }
                        props.forEach {
                            it.properties?.forEach { (subPropName, subProp) ->
                                subPropertyResolution[subPropName]?.add(subProp)
                            }
                        }

                        // recurse on sub-properties, idk if we need to pass strategy by reference
                        // could just say that it's always going to be PERMISSIVE in this block
                        val subPropertyMap = subPropertyResolution.mapValues { (_, subProps) ->
                            es8MergeProperties(mappings = subProps.toTypedArray(), strategy = strategy)
                        }.toMutableMap()

                        Es8MappingProperty(type=null, properties = subPropertyMap, format=format, dynamic = dynamic)
                    } else {
                        throw IllegalStateException("Unexpected branch")
                    }
                }
            }
        }
    }

    return Es8MappingProperty(type=null, properties = flattenedProperties)
}
