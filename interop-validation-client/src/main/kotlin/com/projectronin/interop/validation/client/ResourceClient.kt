package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.request
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.client.generated.models.Resource
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import com.projectronin.interop.validation.client.generated.models.UpdateResource
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
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
class ResourceClient(
    @Value("\${validation.server.url:}")
    private val hostUrl: String,
    private val client: HttpClient,
    private val authenticationService: ValidationAuthenticationService,
) {
    private val resourceUrl: String = "$hostUrl/resources"

    suspend fun getResources(
        status: List<ResourceStatus>?,
        order: Order,
        limit: Int,
        after: UUID? = null,
        organizationId: String? = null,
        resourceType: String? = null,
        issueType: List<String>? = null,
    ): List<Resource> {
        val authentication = authenticationService.getAuthentication()

        val response: HttpResponse =
            client.request("Validation", resourceUrl) { url ->
                get(url) {
                    url {
                        parameters.apply {
                            if (status?.isNotEmpty() == true) {
                                appendAll("status", status.map { it.value })
                            }

                            issueType?.let { appendAll("issue_type", it) }

                            organizationId?.let { append("organization_id", it) }
                            resourceType?.let { append("resource_type", it) }

                            append("order", order.name)
                            append("limit", limit.toString())

                            after?.let { append("after", it.toString()) }
                        }
                    }

                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
            }
        return response.body()
    }

    suspend fun getResourceById(resourceId: UUID): Resource {
        val authentication = authenticationService.getAuthentication()

        val response: HttpResponse =
            client.request("Validation", "$resourceUrl/$resourceId") { url ->
                get(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                }
            }
        return response.body()
    }

    suspend fun addResource(newResource: NewResource): GeneratedId {
        val authentication = authenticationService.getAuthentication()

        val response: HttpResponse =
            client.request("Validation", resourceUrl) { url ->
                post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(newResource)
                }
            }
        return response.body()
    }

    suspend fun updateResource(
        resourceId: UUID,
        updateResource: UpdateResource,
    ): Resource {
        val authentication = authenticationService.getAuthentication()

        val response: HttpResponse =
            client.request("Validation", "$resourceUrl/$resourceId") { url ->
                patch(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                    }
                    accept(ContentType.Application.Json)
                    contentType(ContentType.Application.Json)
                    setBody(updateResource)
                }
            }
        return response.body()
    }

    suspend fun reprocessResource(
        resourceId: UUID,
        reprocessResourceRequest: ReprocessResourceRequest,
    ) {
        val authentication = authenticationService.getAuthentication()

        client.request("Validation", "$resourceUrl/$resourceId/reprocess") { url ->
            post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authentication.accessToken}")
                }
                accept(ContentType.Application.Json)
                contentType(ContentType.Application.Json)
                setBody(reprocessResourceRequest)
            }
        }
    }
}
