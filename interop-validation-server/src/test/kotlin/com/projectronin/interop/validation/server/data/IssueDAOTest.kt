package com.projectronin.interop.validation.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID

@LiquibaseTest(changeLog = "validation/db/changelog/validation.db.changelog-master.yaml")
class IssueDAOTest {
    init {
        // We have to set this to prevent some DBUnit weirdness around UTC and the local time zone that DST seems to cause.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    private val issueDAO = IssueDAO(KtormHelper.database())

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    fun `getIssueSeveritiesForResources - returns empty map if no issues found for resource`() {
        val severitiesByResource = issueDAO.getIssueSeveritiesForResources(listOf(UUID.randomUUID()))
        assertEquals(0, severitiesByResource.size)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    fun `getIssueSeveritiesForResources - resource with single issue returns severity`() {
        val resourceId1 = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val severitiesByResource = issueDAO.getIssueSeveritiesForResources(listOf(resourceId1))

        assertEquals(1, severitiesByResource.size)
        assertEquals(setOf(Severity.FAILED), severitiesByResource[resourceId1])
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithSameSeverity.yaml"], cleanAfter = true)
    fun `getIssueSeveritiesForResources - resource with multiple issues with same severity returns severity`() {
        val resourceId1 = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val severitiesByResource = issueDAO.getIssueSeveritiesForResources(listOf(resourceId1))

        assertEquals(1, severitiesByResource.size)
        assertEquals(setOf(Severity.FAILED), severitiesByResource[resourceId1])
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentSeverity.yaml"], cleanAfter = true)
    fun `getIssueSeveritiesForResources - resource with multiple issues with different severities returns severities`() {
        val resourceId1 = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val severitiesByResource = issueDAO.getIssueSeveritiesForResources(listOf(resourceId1))

        assertEquals(1, severitiesByResource.size)
        assertEquals(setOf(Severity.FAILED, Severity.WARNING), severitiesByResource[resourceId1])
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleResourcesWithIssues.yaml"], cleanAfter = true)
    fun `getIssueSeveritiesForResources - multiple resources return proper severities`() {
        val resourceId1 = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val resourceId2 = UUID.fromString("b417d13a-234e-410a-98d1-b2c700ae1446")
        val severitiesByResource = issueDAO.getIssueSeveritiesForResources(listOf(resourceId1, resourceId2))

        assertEquals(2, severitiesByResource.size)
        assertEquals(setOf(Severity.FAILED, Severity.WARNING), severitiesByResource[resourceId1])
        assertEquals(setOf(Severity.WARNING), severitiesByResource[resourceId2])
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    fun `getIssue -  works`() {
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issue = issueDAO.getIssue(issueID)
        assertEquals(issueID.toString(), issue?.id.toString())
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    fun `getIssue - no issue found`() {
        val resource = issueDAO.getIssue(UUID.randomUUID())
        assertNull(resource)
    }

    @Test
    fun `getIssue - exception thrown if no statuses provided`() {
        val exception = assertThrows<IllegalArgumentException> {
            issueDAO.getIssues(UUID.randomUUID(), listOf(), Order.ASC, 2, null)
        }
        assertEquals("At least one status must be provided", exception.message)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - no issue found for requested statuses`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issues = issueDAO.getIssues(resourceUUID, listOf(IssueStatus.ADDRESSING), Order.ASC, 2, null)
        assertEquals(0, issues.size)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - throws error when no issue found for after`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val after = UUID.randomUUID()
        val exception = assertThrows<IllegalArgumentException> {
            issueDAO.getIssues(resourceUUID, listOf(IssueStatus.ADDRESSING), Order.ASC, 2, after)
        }
        assertEquals("No issue found for $after", exception.message)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - ASC order honors create date time`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issues = issueDAO.getIssues(resourceUUID, listOf(IssueStatus.REPORTED), Order.ASC, 100, null)
        assertEquals(2, issues.size)

        val issue1 = issues[0]
        assertEquals(UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"), issue1.id)
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), issue1.resourceId)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - DESC order honors create date time`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issues = issueDAO.getIssues(resourceUUID, listOf(IssueStatus.REPORTED), Order.DESC, 100, null)
        assertEquals(2, issues.size)

        val issue1 = issues[1]
        assertEquals(UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"), issue1.id)
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), issue1.resourceId)
    }
    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - after honored with asc`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issues = issueDAO.getIssues(resourceUUID, listOf(IssueStatus.REPORTED), Order.ASC, 100, issueID)
        assertEquals(1, issues.size)

        val issue1 = issues[0]
        assertEquals(UUID.fromString("897413b7-419f-4830-a20a-bf24aae16147"), issue1.id)
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), issue1.resourceId)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - limit honored`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issues = issueDAO.getIssues(resourceUUID, listOf(IssueStatus.REPORTED), Order.ASC, 1, null)
        assertEquals(1, issues.size)

        val issue1 = issues[0]
        assertEquals(UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"), issue1.id)
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), issue1.resourceId)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesWithDifferentTimestamps.yaml"], cleanAfter = true)
    fun `getIssue - after honored with desc`() {
        val resourceUUID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueID = UUID.fromString("897413b7-419f-4830-a20a-bf24aae16147")
        val issues = issueDAO.getIssues(resourceUUID, listOf(IssueStatus.REPORTED), Order.DESC, 100, issueID)
        assertEquals(1, issues.size)

        val issue1 = issues[0]
        assertEquals(UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"), issue1.id)
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), issue1.resourceId)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/NoIssues.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/SingleIssue.yaml"], ignoreCols = ["issue_id"])
    fun `insertIssue - can insert`() {
        issueDAO.insertIssue(
            IssueDO {
                resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
                severity = Severity.FAILED
                type = "pat-1"
                location = "Patient.contact"
                description = "No contact details"
                status = IssueStatus.REPORTED
                createDateTime = OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC)
            }
        )
    }

    @Test
    @DataSet(value = ["/dbunit/issue/NoIssues.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/NoIssues.yaml"], ignoreCols = ["issue_id"])
    fun `insertIssue - handles failed insert`() {
        assertThrows<IllegalStateException> {
            // createDateTime is not nullable and MySql won't set a default value for times, so this should throw an exception
            issueDAO.insertIssue(
                IssueDO {
                    resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
                    severity = Severity.FAILED
                    type = "pat-1"
                    location = "Patient.contact"
                    description = "No contact details"
                    status = IssueStatus.REPORTED
                    // createDateTime = OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC)
                }
            )
        }
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/UpdatedIssue.yaml"], ignoreCols = ["issue_id"])
    fun `updateIssue`() {
        issueDAO.updateIssue(
            UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"),
            UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663"),
            IssueDO {
                severity = Severity.WARNING
                type = "pat-1"
                location = "Patient.contact"
                description = "No contact details"
                status = IssueStatus.REPORTED
                updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
            }
        )
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/SingleIssue.yaml"], ignoreCols = ["issue_id"])
    fun `updateIssue fails`() {
        // issue with this UUID does not exist, therefore nothing gets updated.
        issueDAO.updateIssue(
            UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"),
            UUID.randomUUID(),
            IssueDO {
                severity = Severity.WARNING
                type = "pat-1"
                location = "Patient.contact"
                description = "No contact details"
                status = IssueStatus.REPORTED
                updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
            }
        )
    }
}
