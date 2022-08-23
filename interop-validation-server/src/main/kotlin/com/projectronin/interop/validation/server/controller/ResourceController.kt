package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.data.model.toIssueDO
import com.projectronin.interop.validation.server.data.model.toResourceDO
import com.projectronin.interop.validation.server.generated.apis.ResourceApi
import com.projectronin.interop.validation.server.generated.models.GeneratedId
import com.projectronin.interop.validation.server.generated.models.NewResource
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.server.generated.models.Resource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ResourceController(private val resourceDAO: ResourceDAO, private val issueDAO: IssueDAO) : ResourceApi {
    val logger = KotlinLogging.logger { }

    override fun getResources(
        status: List<ResourceStatus>?,
        order: Order,
        limit: Int,
        after: UUID?
    ): ResponseEntity<List<Resource>> {
        val statuses = if (status == null || status.isEmpty()) ResourceStatus.values().toList() else status

        val resourceDOs = resourceDAO.getResources(statuses, order, limit, after)
        if (resourceDOs.isEmpty()) {
            return ResponseEntity.ok(listOf())
        }

        val resourceIds = resourceDOs.map { it.id }
        val issueSeveritiesByResource = issueDAO.getIssueSeveritiesForResources(resourceIds)

        val resources = resourceDOs.mapNotNull {
            val issueSeverities = issueSeveritiesByResource[it.id]
            if (issueSeverities == null) {
                logger.warn { "No issue severities found for resource ${it.id}" }
                null
            } else {
                createResource(it, issueSeverities)
            }
        }

        return ResponseEntity.ok(resources)
    }

    override fun getResourceById(resourceId: UUID): ResponseEntity<Resource> {
        val resourceDO = resourceDAO.getResource(resourceId)
            ?: return ResponseEntity.notFound().build()

        val issueSeverities = issueDAO.getIssueSeveritiesForResources(listOf(resourceDO.id))[resourceDO.id]

        val resource = if (issueSeverities == null) {
            // Once we figure out how we're going to handle logging errors in DAOs, this will need to be changed.  For
            // now, I'm leaving it as a warning.
            logger.warn { "No issue severities found for resource ${resourceDO.id}" }
            null
        } else {
            createResource(resourceDO, issueSeverities)
        }

        // We either have a good resource, or our resource didn't have any issues assigned to it.
        return resource?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.internalServerError().build()
    }

    @Transactional
    override fun addResource(newResource: NewResource): ResponseEntity<GeneratedId> {
        val resourceUUID = resourceDAO.insertResource(newResource.toResourceDO())

        newResource.issues.forEach {
            issueDAO.insertIssue(it.toIssueDO(resourceUUID))
        }

        return ResponseEntity.ok(GeneratedId(resourceUUID))
    }

    override fun reprocessResource(
        resourceId: UUID,
        reprocessResourceRequest: ReprocessResourceRequest
    ): ResponseEntity<Unit> {
        TODO()
    }

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
