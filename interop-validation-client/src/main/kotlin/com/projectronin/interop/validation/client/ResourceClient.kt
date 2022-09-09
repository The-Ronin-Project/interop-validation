package com.projectronin.interop.validation.client

import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.client.generated.models.Resource
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import java.util.UUID

class ResourceClient {
    fun getResources(status: List<ResourceStatus>?, order: Order, limit: Int, after: UUID? = null): List<Resource> {
        TODO()
    }

    fun getResourceById(resourceId: UUID): Resource {
        TODO()
    }

    fun addResource(newResource: NewResource): GeneratedId {
        TODO()
    }

    fun reprocessResource(resourceId: UUID, reprocessResourceRequest: ReprocessResourceRequest) {
        TODO()
    }
}
