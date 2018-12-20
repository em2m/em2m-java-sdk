package io.em2m.messages.model

import io.em2m.flows.Processor

interface MessageProcessor : Processor<MessageContext> {
}