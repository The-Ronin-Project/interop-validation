package com.projectronin.interop.validation.client.auth.auth0

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.projectronin.interop.common.auth.Authentication
import com.projectronin.interop.common.auth.BrokeredAuthenticator
import com.projectronin.interop.common.http.throwExceptionFromHttpStatus
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * Auth0 implementation of [ValidationAuthenticationService]
 */
@Component
class ValidationAuth0Service(
    private val client: HttpClient,
    @Value("\${validation.auth.token.url}")
    private val authTokenUrl: String,
    @Value("\${validation.auth.audience}")
    private val audience: String,
    @Value("\${validation.auth.client.id}")
    private val authClientId: String,
    @Value("\${validation.auth.client.secret}")
    private val authClientSecret: String
) : BrokeredAuthenticator(), ValidationAuthenticationService {
    override fun reloadAuthentication(): Authentication = runBlocking {
        val payload = Auth0Payload(authClientId, authClientSecret, audience)
        val httpResponse: HttpResponse = client.post(authTokenUrl) {
            contentType(ContentType.Application.Json)
            setBody(payload)
        }
        httpResponse.throwExceptionFromHttpStatus("Auth0", "POST $authTokenUrl")
        httpResponse.body<Auth0Authentication>()
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    data class Auth0Payload(
        val clientId: String,
        val clientSecret: String,
        val audience: String,
        val grantType: String = "client_credentials"
    )
}
