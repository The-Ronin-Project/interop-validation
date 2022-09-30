package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.throwExceptionFromHttpStatus
import com.projectronin.interop.validation.client.generated.models.Comment
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.Order
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class CommentClient(
    @Value("\${validation.server.url}")
    private val hostUrl: String,
    private val client: HttpClient
) {
    private val resourceUrl: String = "$hostUrl/resources"

    /**
     * Retrieves the set of comments associated to the supplied resource. This will only be comments directly
     * attached to the resource, and not nested through its issues.
     */
    suspend fun getResourceComments(resourceId: UUID, order: Order): List<Comment> {
        val urlString = "$resourceUrl/$resourceId/comments"
        val response: HttpResponse = client.get(urlString) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            parameter("order", order)
        }
        response.throwExceptionFromHttpStatus("Validation", "GET $urlString")
        return response.body()
    }

    /**
     * Posts a new comment to this resource.
     */
    suspend fun addResourceComment(resourceId: UUID, newComment: Comment): GeneratedId {
        val urlString = "$resourceUrl/$resourceId/comments"
        val response: HttpResponse = client.post(urlString) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(newComment)
        }
        response.throwExceptionFromHttpStatus("Validation", "POST $urlString")
        return response.body()
    }

    /**
     * Retrieves the comments for a specific issue
     */
    suspend fun getResourceIssueComments(resourceId: UUID, issueId: UUID, order: Order): List<Comment> {
        val urlString = "$resourceUrl/$resourceId/issues/$issueId/comments"
        val response: HttpResponse = client.get(urlString) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            parameter("order", order)
        }
        response.throwExceptionFromHttpStatus("Validation", "GET $urlString")
        return response.body()
    }

    /**
     * Posts a new comment to an issue
     */
    suspend fun addResourceIssueComment(resourceId: UUID, issueId: UUID, newComment: Comment): GeneratedId {
        val urlString = "$resourceUrl/$resourceId/issues/$issueId/comments"
        val response: HttpResponse = client.post(urlString) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(newComment)
        }
        response.throwExceptionFromHttpStatus("Validation", "POST $urlString")
        return response.body()
    }
}
