package io.em2m.conveyor.flows

class FlowNotFound(val name: String) : Exception("Flow '$name' not found")