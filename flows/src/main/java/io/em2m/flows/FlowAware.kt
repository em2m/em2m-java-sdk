package io.em2m.flows


interface FlowAware {
    var flow: Flow<*>?
}