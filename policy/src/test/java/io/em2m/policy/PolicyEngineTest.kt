package io.em2m.policy

import com.fasterxml.jackson.databind.node.ArrayNode
import io.em2m.policy.basic.BasicPolicyEngine
import io.em2m.policy.basic.ListPolicySource
import io.em2m.policy.model.Claims
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.basic.BasicKeyResolver
import io.em2m.simplex.conditions.StandardStringConditions
import io.em2m.simplex.model.BasicConditionResolver
import io.em2m.simplex.model.ExprContext
import io.em2m.simplex.model.Key
import io.em2m.simplex.model.KeyHandlerSupport
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.*

class PolicyEngineTest : Assert() {

    val policySource = ListPolicySource.load(File("src/test/data/policies"), File("src/test/data/roles"))
    val keyResolver = BasicKeyResolver(mapOf(
            Key("ident", "orgPath") to OrgPathKey(),
            Key("ident", "organization") to OrgPathKey(),
            Key("claims", "org") to EnvOrganizationKey(),
            Key("report", "ReportType") to ReportTypeKey())
    )
    val conditionResolver = BasicConditionResolver(StandardStringConditions)
    val policyEngine = BasicPolicyEngine(policySource, keyResolver, conditionResolver)

    @Test
    fun testFindAllowedActions() {
        val claims: Claims = Claims(mapOf("sub" to "userid", "roles" to listOf("admin"), "exp" to Date(),
                "features" to listOf("maintenance")))
        val environment = Environment(emptyMap())
        val resource = "em2m:auto:device:1234"
        val allowed = policyEngine.findAllowedActions(PolicyContext(claims, environment, resource))
        println(allowed)
    }

    @Test
    fun testRoleCustomData() {
        val roles = policySource.loadAllRoles()
        val admin = roles.filter { it.id == "admin" }.toList()[0]
        assertEquals(2, (admin.customData.get("allowedRoles") as ArrayNode).size())
    }

    @Test
    @Ignore
    fun testAllowIfFeature() {
        val claimsWithout: Claims = Claims(mapOf("sub" to "userid", "roles" to listOf("admin"), "exp" to Date()))
        val claimsWithMaintenance: Claims = Claims(mapOf("sub" to "userid", "roles" to listOf("admin"), "exp" to Date(),
                "features" to listOf("maintenance")))
        val environment = Environment(emptyMap())
        val resource = "em2m:reports:report:1234"
        assertTrue(policyEngine.isActionAllowed("report:ListReports", PolicyContext(claimsWithMaintenance, environment, resource)))
        assertFalse(policyEngine.isActionAllowed("report:ListReports", PolicyContext(claimsWithout, environment, resource)))
    }

    @Test
    fun testUnknownAction() {
        val claims: Claims = Claims(mapOf("sub" to "userid", "roles" to listOf("sales"), "exp" to Date()))
        val environment = Environment(emptyMap())
        val resource = "em2m:test:test:1234"
        val context = PolicyContext(claims, environment, resource)
        policyEngine.isActionAllowed("foo:bar", context)
    }

    @Test
    @Ignore
    fun testAllowAndDeny() {
        error("Not implemented")
    }

    @Test
    fun testAllow() {
        val claims: Claims = Claims(mapOf("sub" to "1234", "roles" to listOf("sales"), "exp" to Date()))
        val environment = Environment(emptyMap())
        val resource = "em2m:ident:account:1234"
        val context = PolicyContext(claims, environment, resource)
        val allowed = policyEngine.isActionAllowed("ident:UpdateMyAccount", context)
        assertTrue(allowed)
    }

    @Test
    @Ignore
    fun testDeny() {
        error("Not implemented")
    }

    class ReportTypeKey : KeyHandlerSupport() {

        override fun call(key: Key, context: ExprContext): Any {
            return "maintenance"
        }
    }

    class OrgPathKey : KeyHandlerSupport() {

        override fun call(key: Key, context: ExprContext): Any {
            return listOf("root", "auto", "em2m")
        }
    }

    class EnvOrganizationKey : KeyHandlerSupport() {

        override fun call(key: Key, context: ExprContext): Any {
            return "root"
        }
    }

}