package io.em2m.flows

class FlowNotFound(val name: String) : Exception("Flow '$name' not found")