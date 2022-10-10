package com.projectronin.interop.validation.server

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.validation.client.generated.models.Issue
import com.projectronin.interop.validation.client.generated.models.IssueStatus
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.UpdateIssue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID
import com.projectronin.interop.validation.client.generated.models.Severity as ClientSeverity

class IssueIT : BaseValidationIT() {
    private val patient = Patient(
        id = Id("12345"),
        name = listOf()
    )

    private val failedIssue = NewIssue(
        severity = ClientSeverity.FAILED,
        type = "PAT_001",
        description = "No names",
        location = "Patient.name"
    )

    private val warningIssue = NewIssue(
        severity = ClientSeverity.WARNING,
        type = "PAT_001",
        description = "No birth date",
        location = "Patient.birthDate"
    )

    private val resource = NewResource(
        organizationId = "ronin",
        resourceType = "Patient",
        resource = JacksonManager.objectMapper.writeValueAsString(patient),
        issues = listOf(failedIssue, warningIssue)
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
