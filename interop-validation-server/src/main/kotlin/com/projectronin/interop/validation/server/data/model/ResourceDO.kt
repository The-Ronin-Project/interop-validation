package com.projectronin.interop.validation.server.data.model

import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import org.ktorm.entity.Entity
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Entity definition for the Resource data object.
 */
interface ResourceDO : Entity<ResourceDO> {
    companion object : Entity.Factory<ResourceDO>()

    var id: UUID
    var organizationId: String
    var resourceType: String
    var resource: String
    var status: ResourceStatus
    var createDateTime: OffsetDateTime
    var updateDateTime: OffsetDateTime?
    var reprocessDateTime: OffsetDateTime?
    var reprocessedBy: String?
    var clientFhirId: String?
    var repeatCount: Int?
    var lastSeenDateTime: OffsetDateTime?
}
