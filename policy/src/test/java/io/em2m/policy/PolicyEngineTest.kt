package io.em2m.policy

import io.em2m.policy.basic.BasicPolicyEngine
import io.em2m.policy.basic.LocalPolicySource
import io.em2m.policy.model.Claims
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.*
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.*

class PolicyEngineTest : Assert() {

    val simplex: Simplex = Simplex()
            .keys(BasicKeyResolver(mapOf(
                    Key("ident", "orgPath") to OrgPathKey(),
                    Key("ident", "organization") to OrgPathKey(),
                    Key("claims", "org") to EnvOrganizationKey(),
                    Key("report", "ReportType") to ReportTypeKey(),
                    Key("ident", "role") to ConstKeyHandler("sales"))
            ))

    val policySource = LocalPolicySource(File("src/test/data"), simplex)
    val policyEngine = BasicPolicyEngine(policySource, simplex)

    @Test
    fun testFindAllowedActions() {
        val claims = Claims(mapOf("sub" to "userid", "roles" to listOf("admin"), "exp" to Date(),
                "features" to listOf("maintenance")))
        val environment = Environment(emptyMap())
        val resource = "em2m:ident:account:1234"
        val allowed = policyEngine.findAllowedActions(PolicyContext(claims, environment, resource))
        println(allowed)
    }

    @Test
    @Ignore
    fun testAllowIfFeature() {
        val claimsWithout = Claims(mapOf("sub" to "userid", "roles" to listOf("admin"), "exp" to Date()))
        val claimsWithMaintenance = Claims(mapOf("sub" to "userid", "roles" to listOf("admin"), "exp" to Date(),
                "features" to listOf("maintenance")))
        val environment = Environment(emptyMap())
        val resource = "em2m:reports:report:1234"
        assertTrue(policyEngine.isActionAllowed("report:ListReports", PolicyContext(claimsWithMaintenance, environment, resource)))
        assertFalse(policyEngine.isActionAllowed("report:ListReports", PolicyContext(claimsWithout, environment, resource)))
    }

    @Test
    fun testUnknownAction() {
        val claims = Claims(mapOf("sub" to "userid", "roles" to listOf("sales"), "exp" to Date()))
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
        val claims = Claims(mapOf("sub" to "1234", "roles" to listOf("sales"), "exp" to Date()))
        val environment = Environment(emptyMap())
        val resource = "em2m:ident:account:1234"
        val context = PolicyContext(claims, environment, resource)
        val allowed = policyEngine.isActionAllowed("ident:UpdateMyAccount", context)
        assertTrue(allowed)
    }

    @Test
    fun testRoleStatements() {
        val claims = Claims(mapOf("sub" to "1234", "roles" to listOf("sales"), "exp" to Date()))
        val environment = Environment(emptyMap())
        val resource = "em2m:ident:account:1234"
        val context = PolicyContext(claims, environment, resource)
        val allowed = policyEngine.isActionAllowed("feature:SalesRoleFeature", context)
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