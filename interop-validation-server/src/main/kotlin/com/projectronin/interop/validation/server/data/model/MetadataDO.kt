package com.projectronin.interop.validation.server.data.model

import org.ktorm.entity.Entity
import java.util.UUID

/**
 * Entity definition for the Meta data object.
 */
interface MetadataDO : Entity<MetadataDO> {
    companion object : Entity.Factory<MetadataDO>()

    var id: UUID?
    var issueId: UUID
    var registryEntryType: String
    var valueSetName: String?
    var valueSetUuid: UUID?
    var conceptMapName: String?
    var conceptMapUuid: UUID?
    var version: String
}
