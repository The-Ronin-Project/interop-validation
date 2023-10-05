package com.projectronin.interop.validation.server.controller

import com.fasterxml.jackson.module.kotlin.readValue
import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.event.interop.resource.request.v1.InteropResourceRequestV1
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.kafka.KafkaRequestService
import com.projectronin.interop.validation.server.data.CommentDAO
import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.data.model.toCommentDO
import com.projectronin.interop.validation.server.data.model.toIssueDO
import com.projectronin.interop.validation.server.data.model.toMetadataDO
import com.projectronin.interop.validation.server.data.model.toResourceDO
import com.projectronin.interop.validation.server.generated.apis.ResourceApi
import com.projectronin.interop.validation.server.generated.models.GeneratedId
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.NewComment
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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@RestController
class ResourceController(
    private val resourceDAO: ResourceDAO,
    private val issueDAO: IssueDAO,
    private val commentDAO: CommentDAO,
    private val requestService: KafkaRequestService
) : ResourceApi {
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
        val resource = newResource.toResourceDO()

        /** deduplication logic: getting all resources in the validation database that matches the fhir id of the resource **/
        val resourceList = resource.clientFhirId?.let {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                it,
                resource.organizationId,
                resource.resourceType
            )
        }
        /** if no resources are returned, add a new resource (and its attached issues and metadata) **/
        if (resourceList.isNullOrEmpty()) {
            return addNewResource(newResource)
        } else {
            /** get a list of pairs of type and location values for each issue that came in on the resource **/
            val matchOn = newResource.issues.map { Pair(it.type, it.location) }.toSet()
            /**
             * for each resource returned from the resource database, get list of all issues for that resource from the
             * issue database, check if any of the new issues from the resource we are trying to insert match on the
             * issue type and issue location of any existing issues from the issue database
             **/
            resourceList.forEach { existingResource ->
                val existingIssues = issueDAO.getIssues(existingResource.id, listOf(IssueStatus.REPORTED, IssueStatus.ADDRESSING), Order.ASC, 200, null)
                val existingMatchOn = existingIssues.map { Pair(it.type, it.location) }.toSet()
                /**
                 * must match on all the new issues from the new resource to be considered a duplicate resource, if it
                 * is a duplicate, update the resource with repeat count, last seen, and update date time information
                 **/
                if (existingMatchOn.containsAll(matchOn)) {
                    val repeat = existingResource.repeatCount ?: 0
                    val updatedResource = resourceDAO.updateResource(existingResource.id) {
                        it.repeatCount = repeat + 1
                        it.lastSeenDateTime = OffsetDateTime.now(ZoneOffset.UTC)
                        it.updateDateTime = OffsetDateTime.now(ZoneOffset.UTC)
                    }
                    val responseResource = updatedResource?.let { createResource(updatedResource) }
                    return responseResource?.let { ResponseEntity.ok(GeneratedId(responseResource.id)) } ?: ResponseEntity.internalServerError()
                        .build()
                }
            }
            /** if nothing matched exactly in the list of existing issues, add the new resource and its issues and metadata **/
            return addNewResource(newResource)
        }
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
        val resource = resourceDAO.getResource(resourceId) ?: return ResponseEntity.notFound().build()

        val fhirId = resource.clientFhirId ?: runCatching {
            val deserializedResource =
                JacksonManager.objectMapper.readValue<com.projectronin.interop.fhir.r4.resource.Resource<*>>(resource.resource)
            deserializedResource.findFhirId()!!
        }.getOrElse { exception ->
            logger.error(exception) { "Failed to find FHIR ID on resource requested for reprocessing" }
            return ResponseEntity.internalServerError().build()
        }

        val tenant = resource.organizationId

        // The FHIR ID could be either a localized or non-localized value here, but we need to standardize on a Ronin ID.
        // We could pull in interop-fhir-ronin here as well, but just using this for now.
        val localizedFhirId = if (fhirId.startsWith("$tenant-")) fhirId else "$tenant-$fhirId"

        val resourceType = ResourceType.valueOf(resource.resourceType)
        val flowOptions = InteropResourceRequestV1.FlowOptions(
            disableDownstreamResources = false,
            normalizationRegistryMinimumTime = if (reprocessResourceRequest.refreshNormalization == true) {
                OffsetDateTime.now(
                    ZoneOffset.UTC
                )
            } else {
                null
            }
        )
        val response = requestService.pushRequestEvent(
            tenant,
            listOf(localizedFhirId),
            resourceType,
            "validation-server",
            flowOptions
        )
        if (response.failures.isNotEmpty()) {
            return ResponseEntity.internalServerError().build()
        }

        val reprocessComment = NewComment(
            author = reprocessResourceRequest.user,
            text = reprocessResourceRequest.comment
        )
        commentDAO.insertResourceComment(reprocessComment.toCommentDO(), resourceId)

        resourceDAO.updateResource(resourceId) {
            it.status = ResourceStatus.REPROCESSED
            it.reprocessDateTime = OffsetDateTime.now(ZoneOffset.UTC)
            it.reprocessedBy = reprocessResourceRequest.user
        }

        return ResponseEntity.ok().build()
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
            reprocessedBy = resourceDO.reprocessedBy,
            repeatCount = resourceDO.repeatCount,
            lastSeenDtTm = resourceDO.lastSeenDateTime
        )
    }

    private fun addNewResource(newResource: NewResource): ResponseEntity<GeneratedId> {
        val resourceUUID = resourceDAO.insertResource(newResource.toResourceDO())

        newResource.issues.forEach { issue ->
            val newIssueId = issueDAO.insertIssue(issue.toIssueDO(resourceUUID))
            issue.metadata?.forEach { metadata ->
                issueDAO.insertMetadata(metadata.toMetadataDO(newIssueId))
            }
        }
        return ResponseEntity.ok(GeneratedId(resourceUUID))
    }
}
