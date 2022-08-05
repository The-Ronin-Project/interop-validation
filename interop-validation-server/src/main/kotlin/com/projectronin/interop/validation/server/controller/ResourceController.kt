package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.apis.ResourceApi
import com.projectronin.interop.validation.server.models.GeneratedId
import com.projectronin.interop.validation.server.models.NewResource
import com.projectronin.interop.validation.server.models.ReprocessResourceRequest
import com.projectronin.interop.validation.server.models.Resource
import com.projectronin.interop.validation.server.models.ResourceStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ResourceController : ResourceApi {
    override fun getResources(
        status: List<ResourceStatus>?,
        order: String,
        limit: Int,
        after: UUID?
    ): ResponseEntity<List<Resource>> {
        TODO()
    }

    override fun getResourceById(resourceId: UUID): ResponseEntity<Resource> {
        TODO()
    }

    override fun addResource(newResource: NewResource): ResponseEntity<GeneratedId> {
        TODO()
    }

    override fun reprocessResource(
        resourceId: UUID,
        reprocessResourceRequest: ReprocessResourceRequest
    ): ResponseEntity<Unit> {
        TODO()
    }
}
