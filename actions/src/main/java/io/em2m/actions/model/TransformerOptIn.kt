package io.em2m.actions.model

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
@Repeatable
annotation class TransformerOptIn(vararg val transformerOptIns: KClass<out ActionTransformer>)
