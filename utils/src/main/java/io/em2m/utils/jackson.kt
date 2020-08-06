import com.fasterxml.jackson.databind.JsonNode


fun JsonNode.path(path: String): JsonNode {
    var pathNode: JsonNode = this
    for (element in path.split(".")) {
        pathNode = pathNode.path(element)
    }
    return pathNode
}

