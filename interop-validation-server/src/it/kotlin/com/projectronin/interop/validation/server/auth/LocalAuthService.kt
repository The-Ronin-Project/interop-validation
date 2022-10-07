package com.projectronin.interop.validation.server.auth

import com.fasterxml.jackson.databind.JsonNode
import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import java.time.Instant

class LocalAuthService(private val client: HttpClient) : ValidationAuthenticationService {
    private val authUrl = "http://localhost:8081/validation/token"

    override fun getAuthentication(): Authentication = runBlocking {
        val json: JsonNode = client.submitForm(
            url = authUrl,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", "id")
                append("client_secret", "secret")
            }
        ).body()
        val accessToken = json.get("access_token").asText()
        LocalAuthentication(accessToken)
    }
}

data class LocalAuthentication(override val accessToken: String) : Authentication {
    override val tokenType: String = "Bearer"
    override val expiresAt: Instant? = null
    override val refreshToken: String? = null
    override val scope: String? = null
}
