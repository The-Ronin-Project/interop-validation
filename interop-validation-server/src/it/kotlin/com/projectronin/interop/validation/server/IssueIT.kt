package com.projectronin.interop.validation.server

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.validation.client.generated.models.Issue
import com.projectronin.interop.validation.client.generated.models.IssueStatus
import com.projectronin.interop.validation.client.generated.models.Metadata
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewMetadata
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.UpdateIssue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import com.projectronin.interop.validation.client.generated.models.Severity as ClientSeverity

class IssueIT : BaseValidationIT() {
    private val patient =
        Patient(
            id = Id("12345"),
            name = listOf(),
        )

    private val failedIssue =
        NewIssue(
            severity = ClientSeverity.FAILED,
            type = "PAT_001",
            description = "No names",
            location = "Patient.name",
        )

    private val warningIssue =
        NewIssue(
            severity = ClientSeverity.WARNING,
            type = "PAT_001",
            description = "No birth date",
            location = "Patient.birthDate",
        )

    private val resource =
        NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = JacksonManager.objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssue, warningIssue),
        )

    private val meta1 =
        NewMetadata(
            registryEntryType = "value-set",
            valueSetName = "name of the value-set being referenced",
            valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec"),
            conceptMapName = "name of the concept-map being referenced",
            conceptMapUuid = UUID.fromString("29681f0f-41a7-4790-9122-ccff1ee50e0e"),
            version = "1",
        )

    private val meta2 =
        NewMetadata(
            registryEntryType = "concept-map",
            conceptMapName = "name of the concept-map being referenced",
            conceptMapUuid = UUID.fromString("5de34981-4ad1-43c9-b70e-8cb519151df0"),
            version = "2",
        )

    private val failedIssueWithMeta =
        NewIssue(
            severity = ClientSeverity.FAILED,
            type = "PAT_001",
            description = "No names",
            location = "Patient.name",
            metadata = listOf(meta1),
        )

    private val failedIssueWithMetas =
        NewIssue(
            severity = ClientSeverity.FAILED,
            type = "PAT_001",
            description = "No names",
            location = "Patient.name",
            metadata = listOf(meta1, meta2),
        )
    private val failedIssueWithOutMeta =
        NewIssue(
            severity = ClientSeverity.FAILED,
            type = "PAT_001",
            description = "No DOB",
            location = "Patient.birthDate",
        )

    private val resource1 =
        NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = JacksonManager.objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssueWithMeta),
        )

    private val resource2 =
        NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = JacksonManager.objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssueWithOutMeta),
        )

    private val resource3 =
        NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = JacksonManager.objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssueWithMeta, failedIssueWithOutMeta),
        )

    private val resource4 =
        NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = JacksonManager.objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssueWithMetas),
        )

    @Test
    fun `getResourceIssues - gets issues in order`() {
        val resourceId = addResource(resource)
        val issuesAsc = runBlocking { issueClient.getResourceIssues(resourceId, Order.ASC) }
        val issuesDesc = runBlocking { issueClient.getResourceIssues(resourceId, Order.DESC) }

        assertEquals(2, issuesAsc.size)
        assertTrue(issuesAsc[0].createdFrom(failedIssue))
        assertTrue(issuesAsc[1].createdFrom(warningIssue))

        assertEquals(2, issuesDesc.size)
        assertTrue(issuesDesc[0].createdFrom(warningIssue))
        assertTrue(issuesDesc[1].createdFrom(failedIssue))
    }

    @Test
    fun `getResourceIssues - gets issues with meta if it exists`() {
        val resourceId1 = addResource(resource1)
        val resourceId2 = addResource(resource2)
        val issuesResource1 = runBlocking { issueClient.getResourceIssues(resourceId1, Order.ASC) }
        val issuesResource2 = runBlocking { issueClient.getResourceIssues(resourceId2, Order.ASC) }

        assertEquals(1, issuesResource1.size)
        assertNotNull(issuesResource1[0].metadata?.get(0)?.id)
        assertEquals(issuesResource1[0].metadata?.get(0)?.conceptMapName, meta1.conceptMapName)
        assertEquals(issuesResource1[0].metadata?.get(0)?.valueSetName, meta1.valueSetName)
        assertEquals(issuesResource1[0].metadata?.get(0)?.valueSetName, meta1.valueSetName)

        assertEquals(1, issuesResource2.size)
        assertEquals(listOf<Metadata>(), issuesResource2[0].metadata)
    }

    @Test
    fun `getResourceIssues - gets issues with metas if it exists`() {
        val resourceId4 = addResource(resource4)
        val issuesResource1 = runBlocking { issueClient.getResourceIssues(resourceId4, Order.ASC) }

        assertEquals(1, issuesResource1.size)
        assertEquals(2, issuesResource1[0].metadata?.size)
        assertNotNull(issuesResource1[0].metadata?.get(0)?.id)
        assertNotNull(issuesResource1[0].metadata?.get(1)?.id)

        val metadataByType = issuesResource1[0].metadata?.groupBy { it.registryEntryType }
        assertEquals(2, metadataByType?.size)

        val conceptMapMetadata = metadataByType?.get("concept-map")
        assertEquals(1, conceptMapMetadata?.size)
        val valueSetMetadata = metadataByType?.get("value-set")
        assertEquals(1, valueSetMetadata?.size)
        assertEquals(conceptMapMetadata?.get(0)?.conceptMapName, meta2.conceptMapName)
        assertEquals(conceptMapMetadata?.get(0)?.conceptMapUuid, meta2.conceptMapUuid)
        assertEquals(valueSetMetadata?.get(0)?.valueSetName, meta1.valueSetName)
        assertEquals(valueSetMetadata?.get(0)?.valueSetUuid, meta1.valueSetUuid)
    }

    @Test
    fun `getResourceIssues - gets issues even with no meta passed`() {
        val resourceId2 = addResource(resource2)
        val issuesResource2 = runBlocking { issueClient.getResourceIssues(resourceId2, Order.ASC) }

        assertEquals(1, issuesResource2.size)
        assertEquals(listOf<Metadata>(), issuesResource2[0].metadata)
    }

    @Test
    fun `getResourceIssues - gets issues for one resource have one issue with meta and one with no meta`() {
        val resourceId = addResource(resource3)
        val issuesResource = runBlocking { issueClient.getResourceIssues(resourceId, Order.ASC) }

        assertEquals(2, issuesResource.size)
        assertNotNull(issuesResource[0].metadata)
        assertEquals(listOf<Metadata>(), issuesResource[1].metadata)
    }

    @Test
    fun `getResourceIssues - handles issue that doesn't exist`() {
        val issues = runBlocking { issueClient.getResourceIssues(UUID.randomUUID(), Order.ASC) }

        assertTrue(issues.isEmpty())
    }

    @Test
    fun `updateResourceIssues - works`() {
        val resourceId = addResource(resource)
        val issues = runBlocking { issueClient.getResourceIssues(resourceId, Order.ASC) }

        val updateIssue = UpdateIssue(IssueStatus.IGNORED)
        val updatedIssue = runBlocking { issueClient.updateResourceIssue(resourceId, issues[0].id, updateIssue) }

        assertEquals(updateIssue.status, updatedIssue.status)
    }

    @Test
    fun `updateResourceIssues - handles issue not found`() {
        val resourceId = addResource(resource)

        val updateIssue = UpdateIssue(IssueStatus.IGNORED)

        assertThrows<ClientFailureException> {
            runBlocking { issueClient.updateResourceIssue(resourceId, UUID.randomUUID(), updateIssue) }
        }
    }

    @Test
    fun `updateResourceIssues - handles resource not found`() {
        val resourceId = addResource(resource)
        val issues = runBlocking { issueClient.getResourceIssues(resourceId, Order.ASC) }

        val updateIssue = UpdateIssue(IssueStatus.IGNORED)

        assertThrows<ClientFailureException> {
            runBlocking { issueClient.updateResourceIssue(UUID.randomUUID(), issues[0].id, updateIssue) }
        }
    }

    /**
     * Helper function to compare the [NewIssue] sent to the [issueClient] with the [Issue] it returns.
     * Returns true if the [Issue] has the same values as the [newIssue], false if they don't.
     * This is really just here so I don't have to write a bunch of assertEquals statements in every test.
     */
    private fun Issue.createdFrom(newIssue: NewIssue): Boolean {
        return (severity == newIssue.severity) &&
            (type == newIssue.type) &&
            (description == newIssue.description) &&
            (location == newIssue.location)
    }
}
