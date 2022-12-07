package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.request
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
import com.projectronin.interop.validation.client.generated.models.Comment
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewComment
import com.projectronin.interop.validation.client.generated.models.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommentClient(
    @Value("\${validation.server.url}")
    private val hostUrl: String,
    private val client: HttpClient,
    private val authenticationService: ValidationAuthenticationService
) {
    private val resourceUrl: String = "$hostUrl/resources"

    /**
     * Retrieves the set of comments associated to the supplied resource. This will only be comments directly
     * attached to the resource, and not nested through its issues.
     */
    suspend fun getResourceComments(resourceId: UUID, order: Order): List<Comment> {
        val authentication = authenticationService.getAuthentication()

        val urlString = "$resourceUrl/$resourceId/comments"
        val response: HttpResponse = client.request("Validation", urlString) { url ->
            get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("order", order)
            }
        }
        return response.body()
    }

    /**
     * Posts a new comment to this resource.
     */
    suspend fun addResourceComment(resourceId: UUID, newComment: NewComment): GeneratedId {
        val authentication = authenticationService.getAuthentication()

        val urlString = "$resourceUrl/$resourceId/comments"
        val response: HttpResponse = client.request("Validation", urlString) { url ->
            post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(newComment)
            }
        }
        return response.body()
    }

    /**
     * Retrieves the comments for a specific issue
     */
    suspend fun getResourceIssueComments(resourceId: UUID, issueId: UUID, order: Order): List<Comment> {
        val authentication = authenticationService.getAuthentication()

        val urlString = "$resourceUrl/$resourceId/issues/$issueId/comments"
        val response: HttpResponse = client.request("Validation", urlString) { url ->
            get(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                parameter("order", order)
            }
        }
        return response.body()
    }

    /**
     * Posts a new comment to an issue
     */
    suspend fun addResourceIssueComment(resourceId: UUID, issueId: UUID, newComment: NewComment): GeneratedId {
        val authentication = authenticationService.getAuthentication()

        val urlString = "$resourceUrl/$resourceId/issues/$issueId/comments"
        val response: HttpResponse = client.request("Validation", urlString) { url ->
            post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(newComment)
            }
        }
        return response.body()
    }
}
