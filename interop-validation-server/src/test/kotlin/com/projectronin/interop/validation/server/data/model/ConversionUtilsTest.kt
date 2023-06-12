package com.projectronin.interop.validation.server.data.model

import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.NewIssue
import com.projectronin.interop.validation.server.generated.models.NewMetadata
import com.projectronin.interop.validation.server.generated.models.NewResource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.util.UUID

class ConversionUtilsTest {
    private val newIssue = NewIssue(
        severity = Severity.FAILED,
        type = "pat-1",
        description = "No contact details",
        createDtTm = OffsetDateTime.now(),
        location = "Patient.contact"
    )

    private val newResource = NewResource(
        organizationId = "testorg",
        resourceType = "Patient",
        resource = "the patient resource",
        issues = listOf(newIssue),
        createDtTm = OffsetDateTime.now()
    )

    private val newMeta = NewMetadata(
        registryEntryType = "value-set-test",
        valueSetName = "name-of-value-set",
        valueSetUuid = UUID.fromString("49e67956-393c-4d64-b99a-6e1e04e26c4e"),
        conceptMapName = "name-of-concept-map",
        conceptMapUuid = UUID.fromString("65508c9e-bfae-4401-8d1e-89f21f7049ce"),
        version = "1"
    )

    @Test
    fun `can convert NewMetadata to MetadataDO`() {
        val issueId = UUID.randomUUID()
        val metaDo = newMeta.toMetadataDO(issueId)

        assertEquals(issueId, metaDo.issueId)
        assertEquals(newMeta.registryEntryType, metaDo.registryEntryType)
        assertEquals(newMeta.valueSetName, metaDo.valueSetName)
        assertEquals(newMeta.valueSetUuid, metaDo.valueSetUuid)
        assertEquals(newMeta.conceptMapName, metaDo.conceptMapName)
        assertEquals(newMeta.conceptMapUuid, metaDo.conceptMapUuid)
        assertEquals(newMeta.version, metaDo.version)
    }

    @Test
    fun `can convert NewIssue to IssueDO`() {
        val resourceUUID = UUID.randomUUID()
        val issueDO = newIssue.toIssueDO(resourceUUID)

        assertEquals(resourceUUID, issueDO.resourceId)
        assertEquals(newIssue.severity, issueDO.severity)
        assertEquals(newIssue.type, issueDO.type)
        assertEquals(newIssue.location, issueDO.location)
        assertEquals(newIssue.description, issueDO.description)
        assertEquals(newIssue.severity, issueDO.severity)
        assertEquals(IssueStatus.REPORTED, issueDO.status)
        assertEquals(newIssue.createDtTm, issueDO.createDateTime)
        assertNull(issueDO.updateDateTime)
    }

    @Test
    fun `can convert NewResource to ResourceDO`() {
        val resourceDO = newResource.toResourceDO()

        assertEquals(newResource.organizationId, resourceDO.organizationId)
        assertEquals(newResource.resourceType, resourceDO.resourceType)
        assertEquals(newResource.resource, resourceDO.resource)
        assertEquals(ResourceStatus.REPORTED, resourceDO.status)
        assertEquals(newResource.createDtTm, resourceDO.createDateTime)
        assertNull(resourceDO.updateDateTime)
        assertNull(resourceDO.reprocessDateTime)
        assertNull(resourceDO.reprocessedBy)
    }
}
