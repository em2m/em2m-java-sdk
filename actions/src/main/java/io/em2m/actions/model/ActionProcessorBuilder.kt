package io.em2m.actions.model

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import io.em2m.flows.*
import kotlin.reflect.KClass


class ActionProcessorBuilder : AbstractBuilder<ActionContext>() {

    private var prefix: String? = null
    private val classes = HashMap<String, KClass<out Flow<ActionContext>>>()
    private val instances = HashMap<String, Flow<ActionContext>>()

    override fun resolver(injector: Injector): FlowResolver<ActionContext> {
        return object : FlowResolver<ActionContext> {
            override fun findFlow(context: ActionContext): Flow<ActionContext>? {
                val key = context.actionName
                return instances[key] ?: classes[key]?.let {
                    injector.getInstance(it.java)
                }
            }
        }
    }

    override fun injector(injector: Injector): ActionProcessorBuilder {
        super.injector(injector)
        return this
    }

    override fun module(module: Module): ActionProcessorBuilder {
        super.module(module)
        return this
    }

    override fun transformer(transformer: Transformer<ActionContext>): ActionProcessorBuilder {
        super.transformer(transformer)
        return this
    }

    fun prefix(prefix: String): ActionProcessorBuilder {
        this.prefix = prefix
        return this
    }

    fun flow(key: String, flow: Flow<ActionContext>): ActionProcessorBuilder {
        instances[key] = flow
        return this
    }

    fun flow(key: String, flowClass: KClass<out Flow<ActionContext>>): ActionProcessorBuilder {
        classes[key] = flowClass
        return this
    }

    fun flow(flowClass: KClass<out Flow<ActionContext>>): ActionProcessorBuilder {
        val simpleName = requireNotNull(flowClass.simpleName)
        if (prefix != null) {
            classes["$prefix:$simpleName"] = flowClass
        } else {
            classes[simpleName] = flowClass
        }
        return this
    }

    override fun build(): ActionProcessor {
        val injector = injector?.createChildInjector(modules) ?: Guice.createInjector(modules)
        val resolver = resolver(injector)
        return BasicActionProcessor(resolver, xforms)
    }

    class BasicActionProcessor(flowResolver: FlowResolver<ActionContext>, standardXforms: List<Transformer<ActionContext>> = emptyList())
        : ActionProcessor, BasicProcessor<ActionContext>(flowResolver, standardXforms)

}