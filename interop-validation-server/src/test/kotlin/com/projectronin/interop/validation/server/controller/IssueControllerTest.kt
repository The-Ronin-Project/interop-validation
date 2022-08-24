package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
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

    @BeforeEach
    fun setup() {
        issueDAO = mockk()
        controller = IssueController(issueDAO)
    }

    @Test
    fun `getIssues - handles null statuses`() {
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
    fun `getIssues - handles emptyList of statuses`() {
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
            issueDAO.getIssue(issue1Id)
        } returns null

        val response = controller.getIssueById(resourceId, issue1Id)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getIssueByID - returns 400 when mismatch resource`() {
        every {
            issueDAO.getIssue(issue1Id)
        } returns issue1
        val wrongResourceID = UUID.randomUUID()
        val response = controller.getIssueById(wrongResourceID, issue1Id)
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `getIssueByID - works`() {
        every {
            issueDAO.getIssue(issue1Id)
        } returns issue1
        val response = controller.getIssueById(resourceId, issue1Id)
        assertEquals(HttpStatus.OK, response.statusCode)
        val issue = response.body!!
        assertEquals(issue1Id, issue.id)
        assertEquals(Severity.FAILED, issue.severity)
        assertEquals("Patient.contact", issue.location)
    }
}
