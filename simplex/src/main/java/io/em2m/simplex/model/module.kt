package io.em2m.simplex.model

import io.em2m.simplex.Simplex

interface Module {
    val keyResolver: KeyResolver
    val execResolver: ExecResolver
    val pipeResolver: PipeTransformResolver
    val conditionResolver: ConditionResolver

    fun configure(simplex: Simplex)
}

abstract class BasicSimplexModule : Module {

    override val keyResolver = BasicKeyResolver()
    override val pipeResolver = BasicPipeTransformResolver()
    override val conditionResolver = BasicConditionResolver()
    override val execResolver = BasicExecResolver()

    fun key(key: Key, handler: KeyHandler) {
        keyResolver.key(key, handler)
    }

    fun key(key: Key, factory: (Key) -> KeyHandler) {
        keyResolver.key(key, factory)
    }

    fun keys(delegate: KeyResolver) {
        keyResolver.delegate(delegate)
    }

    fun transform(key: String, transform: PipeTransform) {
        pipeResolver.transform(key, transform)
    }

    fun transform(key: String, factory: (String) -> PipeTransform) {
        pipeResolver.transform(key, factory)
    }

    fun transforms(delegate: PipeTransformResolver) {
        pipeResolver.delegate(delegate)
    }

    fun condition(key: String, handler: ConditionHandler) {
        conditionResolver.condition(key, handler)
    }

    fun conditions(delegate: ConditionResolver) {
        conditionResolver.delegate(delegate)
    }

    fun exec(key: String, factory: (String) -> ExecHandler) {
        execResolver.handler(key, factory)
    }

    fun execs(delegate: ExecResolver) {
        execResolver.delegate(delegate)
    }

}
