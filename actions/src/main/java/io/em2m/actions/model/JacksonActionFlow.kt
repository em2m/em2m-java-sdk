package io.em2m.actions.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.actions.xforms.JacksonRequestTransformer
import kotlin.reflect.KClass

open class JacksonActionFlow(type: KClass<out Any>, objectMapper: ObjectMapper = jacksonObjectMapper(), vararg transformers: ActionTransformer)
    : ActionFlowSupport(transformers.toList().plus(JacksonRequestTransformer(type.java, objectMapper)))