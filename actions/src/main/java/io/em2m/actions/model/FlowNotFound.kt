package io.em2m.actions.model

class FlowNotFound(val name: String) : Exception("Flow '$name' not found")