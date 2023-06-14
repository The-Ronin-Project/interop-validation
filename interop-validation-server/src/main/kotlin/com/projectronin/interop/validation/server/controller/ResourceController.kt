package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.data.model.toIssueDO
import com.projectronin.interop.validation.server.data.model.toMetadataDO
import com.projectronin.interop.validation.server.data.model.toResourceDO
import com.projectronin.interop.validation.server.generated.apis.ResourceApi
import com.projectronin.interop.validation.server.generated.models.GeneratedId
import com.projectronin.interop.validation.server.generated.models.NewResource
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.server.generated.models.Resource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import com.projectronin.interop.validation.server.generated.models.UpdateResource
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ResourceController(private val resourceDAO: ResourceDAO, private val issueDAO: IssueDAO) : ResourceApi {
    val logger = KotlinLogging.logger { }

    @PreAuthorize("hasAuthority('SCOPE_read:resources')")
    override fun getResources(
        status: List<ResourceStatus>?,
        order: Order,
        limit: Int,
        after: UUID?,
        organizationId: String?,
        resourceType: String?,
        issueType: List<String>?
    ): ResponseEntity<List<Resource>> {
        val statuses = if (status.isNullOrEmpty()) ResourceStatus.values().toList() else status

        val resourceDOs =
            resourceDAO.getResources(statuses, order, limit, after, organizationId, resourceType, issueType)
        if (resourceDOs.isEmpty()) {
            return ResponseEntity.ok(listOf())
        }

        val resources = createResources(resourceDOs)
        return ResponseEntity.ok(resources)
    }

    @PreAuthorize("hasAuthority('SCOPE_read:resources')")
    override fun getResourceById(resourceId: UUID): ResponseEntity<Resource> {
        val resourceDO = resourceDAO.getResource(resourceId)
            ?: return ResponseEntity.notFound().build()

        val resource = createResource(resourceDO)

        // We either have a good resource, or our resource didn't have any issues assigned to it.
        return resource?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.internalServerError().build()
    }

    @PreAuthorize("hasAuthority('SCOPE_create:resources')")
    @Transactional
    override fun addResource(newResource: NewResource): ResponseEntity<GeneratedId> {
        val resourceUUID = resourceDAO.insertResource(newResource.toResourceDO())

        newResource.issues.forEach { issue ->
            val newIssueId = issueDAO.insertIssue(issue.toIssueDO(resourceUUID))

            issue.metadata?.let { metadata ->
                issueDAO.insertMetadata(metadata.toMetadataDO(newIssueId))
            }
        }

        return ResponseEntity.ok(GeneratedId(resourceUUID))
    }

    @PreAuthorize("hasAuthority('SCOPE_update:resources')")
    override fun updateResource(resourceId: UUID, updateResource: UpdateResource): ResponseEntity<Resource> {
        val resource = resourceDAO.getResource(resourceId) ?: return ResponseEntity.notFound().build()
        if (resource.status == ResourceStatus.REPROCESSED) {
            logger.info { "Unable to update $resourceId as it has already been reprocessed" }
            return ResponseEntity.badRequest().build()
        }

        val updatedResource = resourceDAO.updateResource(resourceId) {
            it.status = ResourceStatus.valueOf(updateResource.status.value)
        }

        val responseResource = updatedResource?.let { createResource(updatedResource) }
        return responseResource?.let { ResponseEntity.ok(responseResource) } ?: ResponseEntity.internalServerError()
            .build()
    }

    @PreAuthorize("hasAuthority('SCOPE_update:resources')")
    override fun reprocessResource(
        resourceId: UUID,
        reprocessResourceRequest: ReprocessResourceRequest
    ): ResponseEntity<Unit> {
        TODO()
    }

    private fun createResources(resourceDOs: List<ResourceDO>): List<Resource> {
        val resourceIds = resourceDOs.map { it.id }
        val issueSeveritiesByResource = issueDAO.getIssueSeveritiesForResources(resourceIds)

        return resourceDOs.mapNotNull {
            val issueSeverities = issueSeveritiesByResource[it.id]
            if (issueSeverities == null) {
                logger.warn { "No issue severities found for resource ${it.id}" }
                null
            } else {
                createResource(it, issueSeverities)
            }
        }
    }

    private fun createResource(resourceDO: ResourceDO): Resource? = createResources(listOf(resourceDO)).firstOrNull()

    private fun createResource(resourceDO: ResourceDO, issueSeverities: Set<Severity>): Resource {
        val resourceSeverity = if (issueSeverities.contains(Severity.FAILED)) Severity.FAILED else Severity.WARNING

        return Resource(
            id = resourceDO.id,
            organizationId = resourceDO.organizationId,
            resourceType = resourceDO.resourceType,
            resource = resourceDO.resource,
            status = resourceDO.status,
            severity = resourceSeverity,
            createDtTm = resourceDO.createDateTime,
            updateDtTm = resourceDO.updateDateTime,
            reprocessDtTm = resourceDO.reprocessDateTime,
            reprocessedBy = resourceDO.reprocessedBy
        )
    }
}
