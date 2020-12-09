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


class LocalPolicySource(roleDir: File, policyDir: File, simplex: Simplex) : PolicySource {

    constructor(dir: File, simplex: Simplex) : this(File(dir, "roles"), File(dir, "policies"), simplex)

    private val jsonMapper = jacksonObjectMapper().registerModule(SimplexModule(simplex))
    private val ymlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule().registerModule(SimplexModule(simplex))

    override val policies: List<Policy> = loadPolicyDir(policyDir)
    override val roles: List<Role> = loadRoleDir(roleDir)

    private fun readTree(file: File): ObjectNode {
        return if (file.isYml()) {
            ymlMapper.readTree(file) as ObjectNode
        } else jsonMapper.readTree(file) as ObjectNode
    }

    private fun loadPolicyDir(dir: File): List<Policy> {
        return dir.walk().maxDepth(1).toList().filter { it.isJson() || it.isYml() }.map {
            val id = it.nameWithoutExtension
            val tree = readTree(it)
            tree.put("id", id)
            jsonMapper.convertValue(tree, Policy::class.java).copy(id = id)
        }
    }

    private fun loadRoleDir(dir: File): List<Role> {
        return dir.walk().maxDepth(1).toList().filter { it.isJson() || it.isYml() }.map {
            val id = it.nameWithoutExtension
            val tree = readTree(it)
            tree.put("id", id)
            jsonMapper.convertValue(tree, Role::class.java).copy(id = id)
        }
    }

}

class CompositePolicySource(private val sources: List<PolicySource>) : PolicySource {

    override val policies: List<Policy>
        get() {
            return sources.flatMap { policies}
        }

    override val roles: List<Role>
        get() {
            return sources.flatMap { roles }
        }

}
