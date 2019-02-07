package io.em2m.messages.model

import io.em2m.flows.FlowSupport

abstract class MessageFlowSupport(transformers: List<MessageTransformer> = ArrayList()) : MessageFlow {

    override val transformers = transformers.toMutableList()

}