package io.em2m.actions

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.em2m.actions.lambda.LambdaRequest
import io.em2m.actions.lambda.LambdaResponse
import io.em2m.actions.model.ActionContext
import io.em2m.actions.xforms.JwtTransformer
import io.em2m.policy.model.Claims
import org.junit.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class JwtTransformerTest {

    val testTransform = JwtTransformer("SECRET_FOR_TESTING")

    private fun generateTestJwt(issuedTime: Date, expiration: Date, accountId: String, roles: List<String>, org: String, brand: String): String {
        val token = JWT.create().withExpiresAt(expiration).withIssuedAt(issuedTime)
            .withClaim("sub", accountId)
            .withClaim("org", org)
            .withClaim("bid", brand)
            .withArrayClaim("roles", roles.toTypedArray())

        val algorithm = Algorithm.HMAC256(testTransform.secretKey)
        return token.sign(algorithm)
    }

    private fun generateRequest(): LambdaRequest {
        return LambdaRequest(
            method = "GET",
            httpMethod = "GET",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer test-token"
            ),
            body = """{"testKey": "testValue"}""",
            requestUri = "/api/v1/resource",
            path = "/resource",
            pathParameters = mapOf(
                "id" to "12345"
            ),
            queryStringParameters = mapOf(
                "filter" to "active",
                "limit" to "10"
            )
        )
    }

    @Test
    fun transformJwt() {
        val testRequest = generateRequest()

        val now = Date()
        val oneHour = 1000 * 60 * 60
        val tokenExpiration = Date(now.time + oneHour)
        val accountId = "test-account-id"
        val roles = listOf("superadmin", "installer")
        val org = "testOrg"
        val brand = "testBrand"
        val token = generateTestJwt(now, tokenExpiration, accountId, roles, org, brand)

        val testContext = ActionContext(
            actionName = "TestService:TestAction",
            claims = Claims(),
            inputStream = testRequest.body?.toByteArray()?.inputStream() ?: byteArrayOf().inputStream(),
            response = LambdaResponse(),
            environment = mutableMapOf(
                "ContentType" to "application/json",
                "Token" to token
            )
        )

        testTransform.doOnNext(testContext)
        val targetClaimsMap = mapOf(
            "sub" to accountId,
            "org" to org,
            "roles" to roles,
            "bid" to brand,
        )
        val targetClaims = Claims(targetClaimsMap)
        val parsedClaims = testContext.claims

        assertEquals(targetClaims.map["sub"], parsedClaims.map["sub"])
        assertEquals(targetClaims.map["org"], parsedClaims.map["org"])
        assertEquals(targetClaims.map["roles"], parsedClaims.map["roles"])
        assertEquals(targetClaims.map["bid"], parsedClaims.map["bid"])
    }
}
