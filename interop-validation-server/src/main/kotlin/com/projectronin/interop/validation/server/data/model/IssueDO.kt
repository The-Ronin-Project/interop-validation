package com.projectronin.interop.validation.server.data.model

import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import org.ktorm.entity.Entity
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Entity definition for the Issue data object.
 */
interface IssueDO : Entity<IssueDO> {
    companion object : Entity.Factory<IssueDO>()

    var id: UUID
    var resourceId: UUID
    var severity: Severity
    var type: String
    var location: String
    var description: String
    var status: IssueStatus
    var createDateTime: OffsetDateTime
    var updateDateTime: OffsetDateTime?
    var metadata: List<MetadataDO>?
}
