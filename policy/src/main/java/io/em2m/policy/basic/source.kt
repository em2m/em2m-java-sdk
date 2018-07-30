package io.em2m.policy.basic

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.em2m.policy.model.Policy
import io.em2m.policy.model.PolicySource
import io.em2m.policy.model.Role
import io.em2m.simplex.Simplex
import io.em2m.simplex.parser.SimplexModule
import java.io.File


private fun File.isYml(): Boolean = extension == "yml" || extension == "yaml"
private fun File.isJson(): Boolean = extension == "json"


class LocalPolicySource(private val dir: File, private val simplex: Simplex) : PolicySource {

    val jsonMapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))
    val ymlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule().registerModule(SimplexModule(simplex))

    override val policies: List<Policy> = loadPolicyDir(File(dir, "policies"))
    override val roles: List<Role> = loadRoleDir(File(dir, "roles"))

    fun readTree(file: File): ObjectNode {
        return if (file.isYml()) {
            ymlMapper.readTree(file) as ObjectNode
        } else jsonMapper.readTree(file) as ObjectNode
    }

    fun loadPolicyDir(dir: File): List<Policy> {
        return dir.walk().maxDepth(1).toList().filter { it.isJson() || it.isYml() }.map {
            val id = it.nameWithoutExtension
            val tree = readTree(it)
            tree.put("id", id)
            jsonMapper.convertValue(tree, Policy::class.java).copy(id = id)
        }
    }

    fun loadRoleDir(dir: File): List<Role> {
        return dir.walk().maxDepth(1).toList().filter { it.isJson() || it.isYml() }.map {
            val id = it.nameWithoutExtension
            val tree = readTree(it)
            tree.put("id", id)
            jsonMapper.convertValue(tree, Role::class.java).copy(id = id)
        }
    }


}
