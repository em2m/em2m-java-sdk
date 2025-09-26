package io.em2m.actions

import io.em2m.actions.lambda.LambdaRequest
import io.em2m.actions.lambda.LambdaResponse
import io.em2m.actions.model.ActionContext
import io.em2m.actions.xforms.JacksonRequestTransformer
import io.em2m.policy.model.Claims
import io.em2m.simplex.evalPath
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class JacksonRequestTransformerTest {

    val testTransform = JacksonRequestTransformer()

    @Test
    fun sanitizeRequest() {
        val testRequest = LambdaRequest(
            method = "GET",
            httpMethod = "GET",
            headers = mapOf(
                "Content-Type" to "application/json",
                "Authorization" to "Bearer test-token"
            ),
            body = """{"maliciousScript01": "<script>doMaliciousThings()</script>","maliciousScript02": "<scr<script>ipt>doMaliciousThings()</scr</script>ipt>"}""",
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

        val charset = Charsets.UTF_8

        val testContext = ActionContext(
            actionName = "TestService:TestAction",
            claims = Claims(),
            inputStream = testRequest.body?.toByteArray(charset)?.inputStream() ?: byteArrayOf().inputStream(),
            response = LambdaResponse(),
            environment = mutableMapOf(
                "ContentType" to "application/json"
            )
        )

        testTransform.doOnNext(testContext)
        val targetScriptVal = "doMaliciousThings()"
        assertEquals(targetScriptVal, testContext.request.evalPath("maliciousScript01"))
        assertEquals(targetScriptVal, testContext.request.evalPath("maliciousScript02"))
    }
}
