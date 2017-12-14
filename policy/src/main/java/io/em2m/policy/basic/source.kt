package io.em2m.policy.basic

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.em2m.policy.model.Policy
import io.em2m.policy.model.Role
import java.io.File

interface PolicySource {

    fun loadAllPolicies(): List<Policy>
    fun loadAllRoles(): List<Role>
    fun policiesForRole(role: String): List<Policy>

}

private fun File.isYml(): Boolean = extension == "yml" || extension == "yaml"
private fun File.isJson(): Boolean = extension == "json"


class ListPolicySource(val policies: List<Policy>, val roles: List<Role>) : PolicySource {


    override fun policiesForRole(role: String): List<Policy> {
        val role = roles.find { it.id == role }
        return role?.policies?.map { policyId -> policies.find { it.id == policyId } }?.filterNotNull() ?: emptyList()
    }

    override fun loadAllRoles(): List<Role> {
        return roles
    }

    override fun loadAllPolicies(): List<Policy> {
        return policies
    }

    companion object {

        val jsonMapper = jacksonObjectMapper()
        val ymlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

        fun readTree(file: File): ObjectNode {
            return if (file.isYml()) {
                ymlMapper.readTree(file) as ObjectNode
            } else jsonMapper.readTree(file) as ObjectNode
        }

        fun load(policyDir: File, roleDir: File): PolicySource {
            val policies = loadPolicyDir(policyDir)
            val roles = loadRoleDir(roleDir)
            return ListPolicySource(policies, roles)
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


}

