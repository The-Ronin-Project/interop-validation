package com.projectronin.interop.validation.server

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.validation.client.generated.models.NewIssue
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

    private val failedIssue = NewIssue(
        severity = Severity.FAILED,
        type = "PAT_001",
        description = "No names",
        location = "Patient.name"
    )
    private val warningIssue = NewIssue(
        severity = Severity.WARNING,
        type = "PAT_001",
        description = "No birth date",
        location = "Patient.birthDate"
    )
    private val newFailedResource = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = objectMapper.writeValueAsString(patient),
        issues = listOf(failedIssue)
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

    @Test
    fun `getResources when none exist`() {
        val resources = runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 10) }

        assertTrue(resources.isEmpty())
    }

    @Test
    fun `getResources returns less than limit if not enough qualify`() {
        val newResource1Id = addResource(newFailedResource)

        val resources = runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 10) }
        assertEquals(1, resources.size)
        assertEquals(newResource1Id, resources[0].id)
    }

    @Test
    fun `getResources returns up to limit if more than limit qualify`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newWarningResource)
        val newResource3Id = addResource(newWarningAndFailedResource)

        val resources = runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 2) }
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
                limit = 10
            )
        }
        assertEquals(1, reportedResources.size)
        assertEquals(newResource1Id, reportedResources[0].id)

        val addressingResources = runBlocking {
            resourceClient.getResources(
                status = listOf(ResourceStatus.ADDRESSING),
                order = Order.ASC,
                limit = 10
            )
        }
        assertEquals(0, addressingResources.size)
    }

    @Test
    fun `getResources honors order`() {
        val newResource1Id = addResource(newFailedResource)
        val newResource2Id = addResource(newWarningResource)

        val ascendingResources =
            runBlocking { resourceClient.getResources(status = null, order = Order.ASC, limit = 2) }
        assertEquals(2, ascendingResources.size)
        assertEquals(newResource1Id, ascendingResources[0].id)
        assertEquals(newResource2Id, ascendingResources[1].id)

        val descendingResources =
            runBlocking { resourceClient.getResources(status = null, order = Order.DESC, limit = 2) }
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
                    after = newResource1Id
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
}
