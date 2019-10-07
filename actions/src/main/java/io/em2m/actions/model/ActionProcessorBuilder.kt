package io.em2m.actions.model

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import kotlin.reflect.KClass


class ActionProcessorBuilder {

    private var prefix: String? = null
    private val classes = HashMap<String, KClass<out ActionFlow>>()
    private val instances = HashMap<String, ActionFlow>()
    private val modules = ArrayList<Module>()
    private var injector: Injector? = null
    private val xforms = ArrayList<ActionTransformer>()

    fun injector(injector: Injector): ActionProcessorBuilder {
        this.injector = injector
        return this
    }

    fun module(module: Module): ActionProcessorBuilder {
        modules.add(module)
        return this
    }

    fun transformer(transformer: ActionTransformer): ActionProcessorBuilder {
        xforms.add(transformer)
        return this
    }

    fun prefix(prefix: String): ActionProcessorBuilder {
        this.prefix = prefix
        return this
    }

    fun flow(key: String, flow: ActionFlow): ActionProcessorBuilder {
        instances[key] = flow
        return this
    }

    fun flow(key: String, flowClass: KClass<out ActionFlow>): ActionProcessorBuilder {
        classes[key] = flowClass
        return this
    }

    fun flow(flowClass: KClass<out ActionFlow>): ActionProcessorBuilder {
        val simpleName = requireNotNull(flowClass.simpleName)
        if (prefix != null) {
            classes["$prefix:$simpleName"] = flowClass
        } else {
            classes[simpleName] = flowClass
        }
        return this
    }

    private fun resolver(injector: Injector): ActionFlowResolver {
        return object : ActionFlowResolver {
            override fun findFlow(context: ActionContext): ActionFlow? {
                val key = context.actionName
                return instances[key] ?: classes[key]?.let {
                    injector.getInstance(it.java)
                }
            }
        }
    }

    fun build(): ActionProcessor {
        val injector = injector?.createChildInjector(modules) ?: Guice.createInjector(modules)
        val resolver = resolver(injector)
        return BasicActionProcessor(resolver, xforms)
    }


}