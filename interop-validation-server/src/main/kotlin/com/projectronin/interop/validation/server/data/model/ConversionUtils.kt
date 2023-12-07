package com.projectronin.interop.validation.server.data.model

import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.NewComment
import com.projectronin.interop.validation.server.generated.models.NewIssue
import com.projectronin.interop.validation.server.generated.models.NewMetadata
import com.projectronin.interop.validation.server.generated.models.NewResource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import mu.KotlinLogging
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Converts a [NewResource] into a [ResourceDO]
 */
fun NewResource.toResourceDO(): ResourceDO {
    val fhirId =
        runCatching {
            val resource = JacksonUtil.readJsonObject(this.resource, Resource::class)
            resource.findFhirId()
        }.getOrElse {
            KotlinLogging.logger {}.warn(it) { "Failed to cast to Resource: ${this.resource}" }
            null
        }
    return ResourceDO {
        organizationId = this@toResourceDO.organizationId
        resourceType = this@toResourceDO.resourceType
        resource = this@toResourceDO.resource
        status = ResourceStatus.REPORTED
        createDateTime = this@toResourceDO.createDtTm ?: OffsetDateTime.now()
        clientFhirId = fhirId
    }
}

/**
 * Converts a [NewIssue] into a [IssueDO]
 */
fun NewIssue.toIssueDO(resourceId: UUID): IssueDO {
    return IssueDO {
        this@IssueDO.resourceId = resourceId
        severity = this@toIssueDO.severity
        type = this@toIssueDO.type
        location = this@toIssueDO.location
        description = this@toIssueDO.description
        status = IssueStatus.REPORTED
        createDateTime = this@toIssueDO.createDtTm ?: OffsetDateTime.now()
    }
}

/**
 * Converts a [NewMetadata] into a [MetadataDO]
 */
fun NewMetadata.toMetadataDO(issueId: UUID): MetadataDO {
    return MetadataDO {
        this@MetadataDO.issueId = issueId
        registryEntryType = this@toMetadataDO.registryEntryType.toString()
        valueSetName = this@toMetadataDO.valueSetName
        valueSetUuid = this@toMetadataDO.valueSetUuid
        conceptMapName = this@toMetadataDO.conceptMapName
        conceptMapUuid = this@toMetadataDO.conceptMapUuid
        version = this@toMetadataDO.version.toString()
    }
}

/**
 * Converts a [NewComment] into a [CommentDO]
 */
fun NewComment.toCommentDO(): CommentDO {
    return CommentDO {
        author = this@toCommentDO.author
        text = this@toCommentDO.text
        createDateTime = this@toCommentDO.createDtTm ?: OffsetDateTime.now()
    }
}
