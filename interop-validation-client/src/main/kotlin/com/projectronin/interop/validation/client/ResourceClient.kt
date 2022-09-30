package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.throwExceptionFromHttpStatus
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.client.generated.models.Resource
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class ResourceClient(
    @Value("\${validation.server.url:}")
    private val hostUrl: String,
    private val client: HttpClient
) {
    private val resourceUrl: String = "$hostUrl/resources"

    suspend fun getResources(status: List<ResourceStatus>?, order: Order, limit: Int, after: UUID? = null): List<Resource> {
        val response: HttpResponse = client.get(resourceUrl) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            parameter("status", status)
            parameter("order", order)
            parameter("limit", limit)
            parameter("after", after)
        }
        response.throwExceptionFromHttpStatus("Validation", "GET $resourceUrl")
        return response.body()
    }

    suspend fun getResourceById(resourceId: UUID): Resource {
        val response: HttpResponse = client.get("$resourceUrl/$resourceId") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
        }
        response.throwExceptionFromHttpStatus("Validation", "GET $resourceUrl/$resourceId")
        return response.body()
    }

    suspend fun addResource(newResource: NewResource): GeneratedId {
        val response: HttpResponse = client.post(resourceUrl) {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(newResource)
        }
        response.throwExceptionFromHttpStatus("Validation", "POST $resourceUrl")
        return response.body()
    }

    suspend fun reprocessResource(resourceId: UUID, reprocessResourceRequest: ReprocessResourceRequest): HttpResponse {
        // TODO reprocessResource is not fully implemented in the controller, so not sure what the return value should be
        val response: HttpResponse = client.put("$resourceUrl/$resourceId/reprocess") {
            accept(ContentType.Application.Json)
            contentType(ContentType.Application.Json)
            setBody(reprocessResourceRequest)
        }
        response.throwExceptionFromHttpStatus("Validation", "Put $resourceUrl/$resourceId/reprocess")
        return response
    }
}
