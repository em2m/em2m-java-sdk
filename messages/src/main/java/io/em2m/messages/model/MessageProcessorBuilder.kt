package io.em2m.messages.model

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Module
import io.em2m.flows.*
import kotlin.reflect.KClass

class MessageProcessorBuilder : AbstractBuilder<MessageContext>() {

    private class Binding(val predicate: (MessageContext) -> Boolean, val flowClass: Class<out MessageFlow>?, val flow: MessageFlow?) {

        fun instance(injector: Injector): MessageFlow {
            return flow ?: injector.getInstance(flowClass)
        }

    }

    private val bindings = ArrayList<Binding>()

    override fun injector(injector: Injector): MessageProcessorBuilder {
        super.injector(injector)
        return this
    }

    override fun module(module: Module): MessageProcessorBuilder {
        super.module(module)
        return this
    }

    override fun transformer(transformer: Transformer<MessageContext>): MessageProcessorBuilder {
        super.transformer(transformer)
        return this
    }

    fun flow(predicate: (MessageContext) -> Boolean, flowClass: KClass<out MessageFlow>): MessageProcessorBuilder {
        bindings.add(Binding(predicate = predicate, flowClass = flowClass.java, flow = null))
        return this
    }

    fun flow(predicate: (MessageContext) -> Boolean, flow: MessageFlow): MessageProcessorBuilder {
        bindings.add(Binding(predicate = predicate, flowClass = null, flow = flow))
        return this
    }

    override fun resolver(injector: Injector): FlowResolver<MessageContext> {
        return object : FlowResolver<MessageContext> {

            override fun findFlow(context: MessageContext): Flow<MessageContext>? {
                return bindings.firstOrNull { it.predicate(context) }?.instance(injector)
            }

        }
    }

    override fun build(): MessageProcessor {
        val injector = injector?.createChildInjector(modules) ?: Guice.createInjector(modules)
        val resolver = resolver(injector)
        return BasicMessageProcessor(resolver, xforms)
    }

    class BasicMessageProcessor(flowResolver: FlowResolver<MessageContext>, standardXforms: List<Transformer<MessageContext>> = emptyList())
        : MessageProcessor, BasicProcessor<MessageContext>(flowResolver, standardXforms)

}