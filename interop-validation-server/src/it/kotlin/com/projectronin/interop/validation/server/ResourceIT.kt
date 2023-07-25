package com.projectronin.interop.validation.server

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewMetadata
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import com.projectronin.interop.validation.client.generated.models.Severity
import com.projectronin.interop.validation.client.generated.models.UpdatableResourceStatus
import com.projectronin.interop.validation.client.generated.models.UpdateResource
import com.projectronin.interop.validation.server.data.binding.ResourceDOs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.ktorm.dsl.eq
import org.ktorm.dsl.update
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ResourceIT : BaseValidationIT() {
    private val patient = Patient(
        id = Id("12345"),
        name = listOf()
    )
    private val location = Location(
        id = Id("67890")
    )

    private val failedIssue = NewIssue(
        severity = Severity.FAILED,
        type = "PAT_001",
        description = "No names",
        location = "Patient.name"
    )
    private val failedIssue2 = NewIssue(
        severity = Severity.FAILED,
        type = "PAT_002",
        description = "No names",
        location = "Patient.name"
    )
    private val warningIssue = NewIssue(
        severity = Severity.WARNING,
        type = "PAT_001",
        description = "No birth date",
        location = "Patient.birthDate"
    )
    private val metadata1 = NewMetadata(
        registryEntryType = "value-set",
        valueSetName = "name of the value-set being referenced",
        valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec"),
        version = "1"
    )
    private val metadata2 = NewMetadata(
        registryEntryType = "concept-map",
        conceptMapName = "name of the concept-map being referenced",
        conceptMapUuid = UUID.fromString("29681f0f-41a7-4790-9122-ccff1ee50e0e"),
        version = "2"
    )
    private val issueWithMeta = NewIssue(
        severity = Severity.WARNING,
        type = "PAT_001",
        description = "No birth date",
        location = "Patient.birthDate",
        metadata = listOf(metadata1, metadata2)
    )
    private val newFailedResource = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(failedIssue)
    )
    private val newFailedResource2 = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(failedIssue2)
    )
    private val newFailedResource3 = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(failedIssue, failedIssue2)
    )
    private val newWarningResource = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(warningIssue)
    )
    private val newWarningAndFailedResource = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(warningIssue, failedIssue)
    )
    private val newOtherTenantFailedResource = NewResource(
        organizationId = "other",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(failedIssue)
    )
    private val newLocationFailedResource = NewResource(
        organizationId = "ronin",
        resourceType = "Location",
        resource = objectMapper.writeValueAsString(location),
        issues = listOf(
            NewIssue(
                severity = Severity.FAILED,
                type = "LOC_001",
                description = "No name",
                location = "Location.name"
            )
        )
    )

    @Test
    fun `getResources when none exist`() {
        val resources = runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 10, issueType = null) }

        assertTrue(resources.isEmpty())
    }

    @Test
    fun `getResources returns less than limit if not enough qualify`() {
        val newResource1Id = addResource(newFailedResource)

        val resources = runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 10, issueType = null) }
        assertEquals(1, resources.size)
        assertEquals(newResource1Id, resources[0].id)
    }

    @Test
    fun `getResources returns up to limit if more than limit qualify`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newWarningResource)
        val newResource3Id = addResource(newWarningAndFailedResource)

        val resources = runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 2, issueType = null) }
        assertEquals(2, resources.size)
        assertEquals(newResource1Id, resources[0].id)
        assertEquals(newResource2Id, resources[1].id)
    }

    @Test
    fun `getResources honors status`() {
        val newResource1Id = addResource(newFailedResource)

        val reportedResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.REPORTED),
                order = Order.ASC,
                limit = 10,
                issueType = null
            )
        }
        assertEquals(1, reportedResources.size)
        assertEquals(newResource1Id, reportedResources[0].id)

        val addressingResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.ADDRESSING),
                order = Order.ASC,
                limit = 10,
                issueType = null
            )
        }
        assertEquals(0, addressingResources.size)
    }

    @Test
    fun `getResources returns one resource with requested issue types`() {
        val resource1Id = addResource(newFailedResource)
        val reportedResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.REPORTED),
                order = Order.ASC,
                limit = 10,
                issueType = listOf("PAT_001")
            )
        }
        assertEquals(1, reportedResources.size)
        assertEquals(reportedResources[0].id, resource1Id)
    }

    @Test
    fun `getResources returns multiple resources with requested issue types`() {
        val resource1Id = addResource(newFailedResource)
        val resource2Id = addResource(newFailedResource)
        val reportedResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.REPORTED),
                order = Order.ASC,
                limit = 10,
                issueType = listOf("PAT_001")
            )
        }
        assertEquals(2, reportedResources.size)
        assertEquals(reportedResources[0].id, resource1Id)
        assertEquals(reportedResources[1].id, resource2Id)
    }

    @Test
    fun `getResources returns multiple resources with multiple requested issue types`() {
        val resource1Id = addResource(newFailedResource)
        val resource2Id = addResource(newFailedResource2)
        val reportedResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.REPORTED),
                order = Order.ASC,
                limit = 10,
                issueType = listOf("PAT_001", "PAT_002")
            )
        }
        assertEquals(2, reportedResources.size)
        assertEquals(reportedResources[0].id, resource1Id)
        assertEquals(reportedResources[1].id, resource2Id)
    }

    @Test
    fun `getResources returns one resource with multiple requested issue types`() {
        val resource1Id = addResource(newFailedResource3)
        val reportedResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.REPORTED),
                order = Order.ASC,
                limit = 10,
                issueType = listOf("PAT_001", "PAT_002")
            )
        }
        assertEquals(1, reportedResources.size)
        assertEquals(reportedResources[0].id, resource1Id)
    }

    @Test
    fun `getResources honors order`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newWarningResource)

        val ascendingResources =
            runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 2, issueType = null) }
        assertEquals(2, ascendingResources.size)
        assertEquals(newResource1Id, ascendingResources[0].id)
        assertEquals(newResource2Id, ascendingResources[1].id)

        val descendingResources =
            runBlocking { resourceClient.getResources(status = null, order = Order.DESC, limit = 2, issueType = null) }
        assertEquals(2, descendingResources.size)
        assertEquals(newResource2Id, descendingResources[0].id)
        assertEquals(newResource1Id, descendingResources[1].id)
    }

    @Test
    fun `getResources honors after`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newWarningResource)

        val ascendingResources =
            runBlocking {
                resourceClient.getResources(
                    status = null,
                    order = Order.ASC,
                    limit = 10,
                    after = newResource1Id,
                    issueType = null
                )
            }
        assertEquals(1, ascendingResources.size)
        assertEquals(newResource2Id, ascendingResources[0].id)
    }

    @Test
    fun `getResources honors organization id`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newOtherTenantFailedResource)

        val ascendingResources =
            runBlocking {
                resourceClient.getResources(
                    status = null,
                    order = Order.ASC,
                    limit = 10,
                    organizationId = "other",
                    issueType = null
                )
            }
        assertEquals(1, ascendingResources.size)
        assertEquals(newResource2Id, ascendingResources[0].id)
    }

    @Test
    fun `getResources honors resource type`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newLocationFailedResource)

        val ascendingResources =
            runBlocking {
                resourceClient.getResources(
                    status = null,
                    order = Order.ASC,
                    limit = 10,
                    resourceType = "Location",
                    issueType = null
                )
            }
        assertEquals(1, ascendingResources.size)
        assertEquals(newResource2Id, ascendingResources[0].id)
    }

    @Test
    fun `getResource for unknown ID`() {
        val exception =
            assertThrows<ClientFailureException> { runBlocking { resourceClient.getResourceById(UUID.randomUUID()) } }
        assertEquals(true, exception.message?.contains("Received 404  when calling Validation"))
    }

    @Test
    fun `getResource returns for known ID`() {
        val newResource = NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssue)
        )
        val resourceId = addResource(newResource)

        val resource = runBlocking { resourceClient.getResourceById(resourceId) }

        assertEquals(resourceId, resource.id)
        assertEquals(newResource.organizationId, resource.organizationId)
        assertEquals(newResource.resourceType, resource.resourceType)
        assertEquals(newResource.resource, resource.resource)
        assertEquals(ResourceStatus.REPORTED, resource.status)
        assertEquals(Severity.FAILED, resource.severity)
        assertNotNull(resource.createDtTm)
        assertNull(resource.updateDtTm)
        assertNull(resource.reprocessDtTm)
        assertNull(resource.reprocessedBy)
    }

    @Test
    fun `addResource works without an explicit create date-time`() {
        val newResource = NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssue)
        )

        val generatedId = runBlocking { resourceClient.addResource(newResource) }
        assertNotNull(generatedId)

        val uuid = generatedId.id!!
        val readResource = runBlocking { resourceClient.getResourceById(uuid) }

        assertEquals(uuid, readResource.id)
        assertEquals(newResource.organizationId, readResource.organizationId)
        assertEquals(newResource.resourceType, readResource.resourceType)
        assertEquals(newResource.resource, readResource.resource)
        assertEquals(ResourceStatus.REPORTED, readResource.status)
        assertEquals(Severity.FAILED, readResource.severity)
        assertNotNull(readResource.createDtTm)
        assertNull(readResource.updateDtTm)
        assertNull(readResource.reprocessDtTm)
        assertNull(readResource.reprocessedBy)
    }

    @Test
    fun `addResource works with an explicit create date-time`() {
        val newResource = NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssue),
            createDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        )

        val generatedId = runBlocking { resourceClient.addResource(newResource) }
        assertNotNull(generatedId)

        val uuid = generatedId.id!!
        val readResource = runBlocking { resourceClient.getResourceById(uuid) }

        assertEquals(uuid, readResource.id)
        assertEquals(newResource.organizationId, readResource.organizationId)
        assertEquals(newResource.resourceType, readResource.resourceType)
        assertEquals(newResource.resource, readResource.resource)
        assertEquals(ResourceStatus.REPORTED, readResource.status)
        assertEquals(Severity.FAILED, readResource.severity)
        assertEquals(newResource.createDtTm!!.likeMySQL(), readResource.createDtTm)
        assertNull(readResource.updateDtTm)
        assertNull(readResource.reprocessDtTm)
        assertNull(readResource.reprocessedBy)
    }

    @Test
    fun `updateResource works for a non-reprocessed resource`() {
        val resourceId = addResource(newFailedResource)

        // Verify initial status
        val initialResource = runBlocking { resourceClient.getResourceById(resourceId) }
        assertEquals(ResourceStatus.REPORTED, initialResource.status)
        assertNull(initialResource.updateDtTm)

        val update = UpdateResource(status = UpdatableResourceStatus.ADDRESSING)
        val updatedResource = runBlocking { resourceClient.updateResource(resourceId, update) }
        assertEquals(ResourceStatus.ADDRESSING, updatedResource.status)
        assertNotNull(updatedResource.updateDtTm)

        val recheckResource = runBlocking { resourceClient.getResourceById(resourceId) }
        assertEquals(resourceId, recheckResource.id)
        assertEquals(newFailedResource.organizationId, recheckResource.organizationId)
        assertEquals(newFailedResource.resourceType, recheckResource.resourceType)
        assertEquals(newFailedResource.resource, recheckResource.resource)
        assertEquals(ResourceStatus.ADDRESSING, recheckResource.status)
        assertEquals(Severity.FAILED, recheckResource.severity)
        assertNotNull(recheckResource.createDtTm)
        assertNotNull(recheckResource.updateDtTm)
        assertNull(recheckResource.reprocessDtTm)
        assertNull(recheckResource.reprocessedBy)
    }

    @Test
    fun `updateResource fails for a reprocessed resource`() {
        val resourceId = addResource(newFailedResource)

        // Mark as Reprocessed (this currently requires modifying the DB directly)
        database.update(ResourceDOs) {
            set(it.status, com.projectronin.interop.validation.server.generated.models.ResourceStatus.REPROCESSED)
            where {
                it.id eq resourceId
            }
        }

        // Verify initial status
        val initialResource = runBlocking { resourceClient.getResourceById(resourceId) }
        assertEquals(ResourceStatus.REPROCESSED, initialResource.status)

        val update = UpdateResource(status = UpdatableResourceStatus.ADDRESSING)
        val exception = assertThrows<ClientFailureException> {
            runBlocking { resourceClient.updateResource(resourceId, update) }
        }
        println(exception.message)
        assertEquals(true, exception.message?.contains("400"))
    }

    @Test
    fun `addResource works for issue with metadata`() {
        val newResource = NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = objectMapper.writeValueAsString(patient),
            issues = listOf(issueWithMeta),
            createDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        )

        val generatedId = runBlocking { resourceClient.addResource(newResource) }
        assertNotNull(generatedId)

        val uuid = generatedId.id!!
        val readIssue = runBlocking { issueClient.getResourceIssues(uuid, Order.ASC) }
        assertEquals(1, readIssue.size)
        assertEquals(2, readIssue[0].metadata?.size)

        val metadataByType = readIssue[0].metadata?.groupBy { it.registryEntryType }
        assertEquals(2, metadataByType?.size)
        val conceptMapMetadata = metadataByType?.get("concept-map")
        assertEquals(1, conceptMapMetadata?.size)
        val valueSetMetadata = metadataByType?.get("value-set")
        assertEquals(1, valueSetMetadata?.size)

        assertEquals(conceptMapMetadata?.get(0)?.conceptMapName, metadata2.conceptMapName)
        assertEquals(conceptMapMetadata?.get(0)?.conceptMapUuid, metadata2.conceptMapUuid)
        assertEquals(valueSetMetadata?.get(0)?.valueSetName, metadata1.valueSetName)
        assertEquals(valueSetMetadata?.get(0)?.valueSetUuid, metadata1.valueSetUuid)
    }
}
