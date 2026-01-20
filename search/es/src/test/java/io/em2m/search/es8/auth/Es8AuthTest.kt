package io.em2m.search.es8.auth

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.em2m.search.es8.Es8Api
import io.em2m.search.es8.models.auth.Es8CreateUserRequest
import io.em2m.search.es8.models.auth.Es8Role
import feign.Feign
import feign.auth.BasicAuthRequestInterceptor
import feign.jackson.JacksonDecoder
import feign.jackson.JacksonEncoder
import feign.slf4j.Slf4jLogger
import io.em2m.geo.geojson.GeoJsonModule
import io.em2m.search.es.TextPlainEncoder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.TestMethodOrder
import kotlin.test.Test

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Es8AuthTest {

    val es8Url: String = System.getenv()["es8Url"] ?: "http://localhost:9200"

    val es8User = requireNotNull(System.getenv()["es8User"]) { "es8User was not set in env variables." }
    val es8Pass = requireNotNull(System.getenv()["es8Pass"]) { "es8Pass was not set in env variables." }

    val mapper: ObjectMapper = jacksonObjectMapper().registerModule(GeoJsonModule())
        .enable(SerializationFeature.INDENT_OUTPUT)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    val superUserClient: Es8Api = Feign.builder()
        .encoder(TextPlainEncoder(JacksonEncoder(mapper)))
        .decoder(JacksonDecoder(mapper))
        .logger(Slf4jLogger())
        .requestInterceptor(BasicAuthRequestInterceptor(es8User, es8Pass))
        .logLevel(feign.Logger.Level.FULL)
        .target(Es8Api::class.java, es8Url)

    companion object {
        val uniquePassword = Array(16) { ('a'..'z').random() }.joinToString("")

        var roleName: String = "engineer"

        var username: String = "bob.builder"
        var createRequest = Es8CreateUserRequest(
            password = uniquePassword,
            roles = listOf(roleName),
            full_name = "Bob Builder",
            email = "bob.builder@xirgo.com",
            metadata = mapOf("likes" to "trucks"))

    }

    @Test
    @Order(0)
    fun `list all users`() {
        val users = superUserClient.getUsers()
        println(users)
    }

    @Test
    @Order(10)
    fun `create role`() {
        val response = superUserClient.createRole(roleName,
            Es8Role(
                "A software engineer, has access to CRUD and bulk operations on documents. " +
                        "Does not have access to deleting indices or cluster operations.")
        )
        assert(response.created) { "Role could not be created, does it already exist?" }
    }

    @Test
    @Order(20)
    fun `create user`() {
        val response = superUserClient.createUser(username, createRequest)
        println(response)
        assert(response.created) { "User could not be created, do they already exist?" }
    }

    @Test
    @Order(25)
    fun `list roles`() {
        val response = superUserClient.listRoles()
        assert(response.isNotEmpty())
    }

    @Test
    @Order(30)
    fun `delete role`() {
        val response = superUserClient.deleteRole(roleName)
        println(response)
    }

    @Test
    @Order(100)
    fun `disable user`() {
        val response = superUserClient.disableUser(username)
        println(response)
    }

    @Test
    @Order(110)
    fun `enable user`() {
        val response = superUserClient.enableUser(username)
        println(response)
    }

    @Test
    @Order(1000)
    fun `delete user`() {
        val response = superUserClient.deleteUser(username)
        println(response)
    }

}
