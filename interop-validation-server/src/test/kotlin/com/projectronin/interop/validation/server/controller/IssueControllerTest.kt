package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.data.model.MetadataDO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import com.projectronin.interop.validation.server.generated.models.UpdateIssue
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class IssueControllerTest {
    lateinit var issueDAO: IssueDAO
    private lateinit var controller: IssueController

    private val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
    private val issue1Id = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
    private val issue2Id = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b777")
    private val meta1Id = UUID.fromString("12ded6af-3a6f-4326-9666-baf2f27b6eca") // Et7WrzpvQyaWZgAAuvLyew==
    private val meta2Id = UUID.fromString("c950af00-34a2-41db-b37d-6f85da0bc3b3")
    private val valueSetUUID = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec")
    private val conceptMapUUID = UUID.fromString("29681f0f-41a7-4790-9122-ccff1ee50e0e") // KWgfD0GnR5CRIgAAzP8e5Q==
    private val issueCreateTime = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO = ResourceDO {
        id = resourceId
        organizationId = "testorg"
        resourceType = "Patient"
        resource = "patient resource"
        status = ResourceStatus.REPORTED
        createDateTime = issueCreateTime
    }

    private val issue1 = IssueDO {
        id = issue1Id
        resourceId = resourceDO.id
        severity = Severity.FAILED
        type = "pat-1"
        location = "Patient.contact"
        description = "No contact details"
        status = IssueStatus.REPORTED
        createDateTime = issueCreateTime
    }
    private val issue2 = IssueDO {
        id = issue2Id
        resourceId = resourceDO.id
        severity = Severity.FAILED
        type = "pat-1"
        location = "Patient.contact"
        description = "No contact details"
        status = IssueStatus.REPORTED
        createDateTime = issueCreateTime
    }

    private val meta1 = MetadataDO {
        id = meta1Id
        issueId = issue1Id
        registryEntryType = "concept_map"
        valueSetName = "value-set-name"
        valueSetUuid = valueSetUUID
        conceptMapName = "concept-map-name"
        conceptMapUuid = conceptMapUUID
    }

    private val meta2 = MetadataDO {
        id = meta2Id
        issueId = issue2Id
        registryEntryType = "concept_map"
        valueSetName = "value-set-name"
        valueSetUuid = valueSetUUID
        conceptMapName = "concept-map-name"
        conceptMapUuid = conceptMapUUID
    }

    private val metaForIssueUpdate = MetadataDO {
        id = meta1Id
        issueId = issue2Id
        registryEntryType = "concept_map"
        valueSetName = "value-set-name"
        valueSetUuid = valueSetUUID
        conceptMapName = "concept-map-name"
        conceptMapUuid = conceptMapUUID
    }

    private val issueWithMeta = IssueDO {
        id = issue1Id
        resourceId = resourceDO.id
        severity = Severity.FAILED
        type = "pat-1"
        location = "Patient.contact"
        description = "No contact details"
        status = IssueStatus.REPORTED
        createDateTime = issueCreateTime
        metadata = meta1
    }

    @BeforeEach
    fun setup() {
        issueDAO = mockk()
        controller = IssueController(issueDAO)
    }

    @Test
    fun `getIssues - handles null statuses`() {
        issue1.metadata = meta1
        every {
            issueDAO.getIssues(resourceId, IssueStatus.values().toList(), Order.ASC, 10, null)
        } returns listOf(issue1)

        val response = controller.getIssues(resourceId, null, Order.ASC, 10, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val issues = response.body!!
        assertEquals(1, issues.size)

        assertEquals(issue1Id, issues.firstOrNull()?.id)
    }

    @Test
    fun `getIssues - handles meta data`() {
        every {
            issueDAO.getIssues(resourceId, IssueStatus.values().toList(), Order.ASC, 10, null)
        } returns listOf(issueWithMeta)

        val response = controller.getIssues(resourceId, null, Order.ASC, 10, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val issues = response.body!!
        assertEquals(1, issues.size)

        assertEquals(issue1Id, issues.firstOrNull()?.id)
        val metaData = issues[0].metadata
        assertNotNull(issues[0].metadata)
        assertEquals(metaData?.firstOrNull()?.id, meta1Id)
        assertEquals(metaData?.firstOrNull()?.registryEntryType, "concept_map")
        assertEquals(metaData?.firstOrNull()?.valueSetName, "value-set-name")
        assertEquals(metaData?.firstOrNull()?.valueSetUuid, valueSetUUID)
        assertEquals(metaData?.firstOrNull()?.conceptMapName, "concept-map-name")
        assertEquals(metaData?.firstOrNull()?.conceptMapUuid, conceptMapUUID)
    }

    @Test
    fun `getIssues - handles emptyList of statuses`() {
        issue1.metadata = meta1
        issue2.metadata = meta2
        every {
            issueDAO.getIssues(resourceId, IssueStatus.values().toList(), Order.ASC, 10, null)
        } returns listOf(issue1, issue2)

        val response = controller.getIssues(resourceId, emptyList(), Order.ASC, 10, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val issues = response.body!!
        assertEquals(2, issues.size)

        assertEquals(issue1Id, issues.firstOrNull()?.id)
    }

    @Test
    fun `getIssues - none found returns none`() {
        every {
            issueDAO.getIssues(resourceId, listOf(IssueStatus.IGNORED), Order.ASC, 10, null)
        } returns emptyList()

        val response = controller.getIssues(resourceId, listOf(IssueStatus.IGNORED), Order.ASC, 10, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val issues = response.body!!
        assertEquals(0, issues.size)
    }

    @Test
    fun `getIssueByID - returns 404 when not found`() {
        every {
            issueDAO.getIssue(resourceId, issue1Id)
        } returns null

        val response = controller.getIssueById(resourceId, issue1Id)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getIssueByID - returns 404 when mismatch resource`() {
        val wrongResourceID = UUID.randomUUID()
        every {
            issueDAO.getIssue(wrongResourceID, issue1Id)
        } returns null

        val response = controller.getIssueById(wrongResourceID, issue1Id)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getIssueByID - works`() {
        issue1.metadata = meta1
        every {
            issueDAO.getIssue(resourceId, issue1Id)
        } returns issue1
        val response = controller.getIssueById(resourceId, issue1Id)
        assertEquals(HttpStatus.OK, response.statusCode)
        val issue = response.body!!
        assertEquals(issue1Id, issue.id)
        assertEquals(Severity.FAILED, issue.severity)
        assertEquals("Patient.contact", issue.location)
    }

    @Test
    fun `updateIssue - works`() {
        val updatedIssue = IssueDO {
            id = issue2Id
            resourceId = resourceDO.id
            severity = Severity.FAILED
            type = "pat-1"
            location = "Patient.contact"
            description = "No contact details"
            status = IssueStatus.REPORTED
            createDateTime = issueCreateTime
            updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
            metadata = metaForIssueUpdate
        }
        every {
            issueDAO.updateIssue(resourceId, issue2Id, captureLambda())
        } returns updatedIssue

        val response = controller.updateIssue(resourceId, issue2Id, UpdateIssue(status = IssueStatus.REPORTED))
        assertEquals(HttpStatus.OK, response.statusCode)

        val issue = response.body!!
        assertEquals(issue.id, updatedIssue.id)
        assertEquals(issue.severity, Severity.FAILED)
        assertEquals(issue.status, IssueStatus.REPORTED)
        assertEquals(issue.updateDtTm, OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC))
    }

    @Test
    fun `updateIssue - fails`() {
        val updateIssue = IssueDO {
            severity = Severity.FAILED
            type = "pat-1"
            location = "Patient.contact"
            description = "No contact details"
            status = IssueStatus.REPORTED
            updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
        }
        every {
            issueDAO.updateIssue(resourceId, issue2Id, captureLambda())
        } returns null

        val response = controller.updateIssue(resourceId, issue2Id, UpdateIssue(status = IssueStatus.REPORTED))
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals(response.body, null)
    }
}
