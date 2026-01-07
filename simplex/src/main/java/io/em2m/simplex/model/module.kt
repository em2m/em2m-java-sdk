package io.em2m.simplex.model

import io.em2m.simplex.Simplex

interface Module {
    val keyResolver: KeyResolver
    val execResolver: ExecResolver
    val pipeResolver: PipeTransformResolver
    val conditionResolver: ConditionResolver

    fun configure(simplex: Simplex)

    fun install(other: Module)

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

    fun key(pair: Pair<Key, KeyHandler>) {
        this.key(pair.first, pair.second)
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

    fun transform(pair: Pair<String, PipeTransform>) {
        this.transform(pair.first, pair.second)
    }

    fun transforms(delegate: PipeTransformResolver) {
        pipeResolver.delegate(delegate)
    }

    fun condition(key: String, handler: ConditionHandler) {
        conditionResolver.condition(key, handler)
    }

    fun condition(pair: Pair<String, ConditionHandler>) {
        this.condition(pair.first, pair.second)
    }

    fun conditions(delegate: ConditionResolver) {
        conditionResolver.delegate(delegate)
    }

    fun exec(key: String, factory: (String) -> ExecHandler) {
        execResolver.handler(key, factory)
    }

    fun exec(pair: Pair<String, (String) -> ExecHandler>) {
        this.exec(pair.first, pair.second)
    }

    fun execs(delegate: ExecResolver) {
        execResolver.delegate(delegate)
    }

    override fun install(other: Module) {
        this.keys(other.keyResolver)
        this.transforms(other.pipeResolver)
        this.conditions(other.conditionResolver)
        this.execs(other.execResolver)
    }

}
