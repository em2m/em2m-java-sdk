package io.em2m.policy

import io.em2m.policy.basic.EnhancedPolicyEngine
import io.em2m.policy.basic.LocalPolicySource
import io.em2m.policy.model.Claims
import io.em2m.policy.model.Environment
import io.em2m.policy.model.PolicyContext
import io.em2m.simplex.Simplex
import io.em2m.simplex.model.BasicKeyResolver
import io.em2m.simplex.model.ConstKeyHandler
import io.em2m.simplex.model.Key
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.util.*

class EnhancedPolicyEngineTest : Assert() {

    val simplex: Simplex = Simplex()
            .keys(BasicKeyResolver(mapOf(
                    Key("ident", "orgPath") to ConstKeyHandler(listOf("root", "auto", "em2m")),
                    Key("ident", "organization") to ConstKeyHandler("em2m"),
                    Key("claims", "org") to ConstKeyHandler("root"),
                    Key("report", "ReportType") to ConstKeyHandler("maintenance"),
                    Key("ident", "role") to ConstKeyHandler("sales"))
            ))

    val policySource = LocalPolicySource(File("src/test/data"), simplex)
    val policyEngine = EnhancedPolicyEngine(policySource, simplex)

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
    fun testUnknownAction() {
        val claims = Claims(mapOf("sub" to "userid", "roles" to listOf("sales"), "exp" to Date()))
        val environment = Environment(emptyMap())
        val resource = "em2m:test:test:1234"
        val context = PolicyContext(claims, environment, resource)
        assertFalse(policyEngine.isActionAllowed("foo:bar", context))
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

}