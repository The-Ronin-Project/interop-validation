package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.throwExceptionFromHttpStatus
import com.projectronin.interop.validation.client.generated.models.Issue
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.UpdateIssue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IssueClient(
    @Value("\${validation.server.url}")
    private val hostUrl: String,
    private val client: HttpClient
) {
    private val resourceUrl: String = "$hostUrl/resources"

    /**
     * Retrieves the set of issues associated to the supplied resource.
     */
    suspend fun getResourceIssues(resourceId: UUID, order: Order): List<Issue> {
        val urlString = "$resourceUrl/$resourceId/issues"
        val response: HttpResponse = client.get(urlString) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            parameter("order", order)
        }
        response.throwExceptionFromHttpStatus("Validation", "GET $urlString")
        return response.body()
    }

    /**
     * Updates the issue. Only status updates will be supported at this time.
     */
    suspend fun updateResourceIssue(resourceId: UUID, issueId: UUID, updateIssue: UpdateIssue): Issue {
        val urlString = "$resourceUrl/$resourceId/issues/$issueId"
        val response: HttpResponse = client.patch(urlString) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(updateIssue)
        }
        response.throwExceptionFromHttpStatus("Validation", "PATCH $urlString")
        return response.body()
    }
}
