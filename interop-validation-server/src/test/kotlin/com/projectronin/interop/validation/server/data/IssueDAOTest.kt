package com.projectronin.interop.validation.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.data.model.MetadataDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        val resourceID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issue = issueDAO.getIssue(resourceID, issueID)
        assertEquals(issueID.toString(), issue?.id.toString())
        assertNull(issue?.metadata)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssueWithMeta.yaml"], cleanAfter = true)
    fun `getIssue -  works with meta`() {
        val resourceID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec")
        val issue = issueDAO.getIssue(resourceID, issueID)
        assertEquals(issueID.toString(), issue?.id.toString())

        assertNotNull(issue?.metadata)
        assertEquals(issueID, issue?.metadata?.issueId)
        assertEquals("value-set", issue?.metadata?.registryEntryType)
        assertEquals("thisIsTheValueSetBeingUsed", issue?.metadata?.valueSetName)
        assertEquals(valueSetUuid, issue?.metadata?.valueSetUuid)
        assertEquals("1", issue?.metadata?.version)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesMultipleMeta.yaml"], cleanAfter = true)
    fun `getIssue -  works with multiple issues and multiple meta`() {
        val status = listOf(IssueStatus.REPORTED)
        val resourceID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issueID2 = UUID.fromString("897413b7-419f-4830-a20a-bf24aae16147")
        val valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec")
        val issues = issueDAO.getIssues(resourceID, status, Order.ASC, 4, null)
        assertEquals(2, issues.size)
        assertNotNull(issues[0].id)
        assertEquals(issueID, issues[0].metadata?.issueId)
        assertEquals(valueSetUuid, issues[0].metadata?.valueSetUuid)

        assertNotNull(issues[1].id)
        assertEquals(issueID2, issues[1].metadata?.issueId)
        assertEquals(valueSetUuid, issues[1].metadata?.valueSetUuid)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/MultipleIssuesOneMeta.yaml"], cleanAfter = true)
    fun `getIssue -  works with multiple issues and one meta`() {
        val status = listOf(IssueStatus.REPORTED)
        val resourceID = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec")
        val issues = issueDAO.getIssues(resourceID, status, Order.ASC, 4, null)
        assertEquals(2, issues.size)
        assertNotNull(issues[0].id)
        assertEquals(issueID, issues[0].metadata?.issueId)
        assertEquals(valueSetUuid, issues[0].metadata?.valueSetUuid)

        assertNotNull(issues[1].id)
        assertNull(issues[1].metadata)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    fun `getIssue -  for wrong resourceID`() {
        val resourceID = UUID.randomUUID()
        val issueID = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issue = issueDAO.getIssue(resourceID, issueID)
        assertNull(issue)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    fun `getIssue - no issue found`() {
        val issue = issueDAO.getIssue(UUID.randomUUID(), UUID.randomUUID())
        assertNull(issue)
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
        assertNull(issue1.metadata)
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
        assertNull(issue1.metadata)
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
        assertNull(issue1.metadata)
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
        assertNull(issue1.metadata)
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
        assertNull(issue1.metadata)
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
    @DataSet(value = ["/dbunit/issue/NoMeta.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/SingleMeta.yaml"], ignoreCols = ["meta_id"])
    fun `insertMeta - can insert`() {
        issueDAO.insertMetadata(
            MetadataDO {
                issueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
                registryEntryType = "value-set"
                valueSetName = "thisIsTheValueSetBeingUsed"
                valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec")
                version = "1"
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
    @ExpectedDataSet(value = ["/dbunit/issue/UpdatedIssue.yaml"])
    fun `updateIssue`() {
        val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issue = issueDAO.updateIssue(resourceId, issueId) {
            it.severity = Severity.WARNING
            it.type = "pat-2"
            it.location = "Patient.telecom"
            it.description = "No telecom details"
            it.status = IssueStatus.ADDRESSED
            it.updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
        }

        issue!!
        assertEquals(issueId, issue.id)
        assertEquals(resourceId, issue.resourceId)
        assertEquals(Severity.WARNING, issue.severity)
        assertEquals("pat-2", issue.type)
        assertEquals("Patient.telecom", issue.location)
        assertEquals("No telecom details", issue.description)
        assertEquals(IssueStatus.ADDRESSED, issue.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), issue.createDateTime)
        assertEquals(OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC), issue.updateDateTime)
        assertNull(issue.metadata)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/UpdatedIssue.yaml"], ignoreCols = ["update_dt_tm"])
    fun `updateIssue with no provided updateDateTime`() {
        val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val issueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
        val issue = issueDAO.updateIssue(resourceId, issueId) {
            it.severity = Severity.WARNING
            it.type = "pat-2"
            it.location = "Patient.telecom"
            it.description = "No telecom details"
            it.status = IssueStatus.ADDRESSED
        }

        issue!!
        assertEquals(issueId, issue.id)
        assertEquals(resourceId, issue.resourceId)
        assertEquals(Severity.WARNING, issue.severity)
        assertEquals("pat-2", issue.type)
        assertEquals("Patient.telecom", issue.location)
        assertEquals("No telecom details", issue.description)
        assertEquals(IssueStatus.ADDRESSED, issue.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), issue.createDateTime)
        assertNotNull(issue.updateDateTime)
        assertNull(issue.metadata)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/SingleIssue.yaml"])
    fun `updateIssue for unknown resource`() {
        val issue = issueDAO.updateIssue(UUID.randomUUID(), UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")) {
            it.severity = Severity.FAILED
        }
        assertNull(issue)
    }

    @Test
    @DataSet(value = ["/dbunit/issue/SingleIssue.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/issue/SingleIssue.yaml"])
    fun `updateIssue for unknown issue`() {
        // issue with this UUID does not exist, therefore nothing gets updated.
        val issue = issueDAO.updateIssue(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), UUID.randomUUID()) {
            it.severity = Severity.FAILED
        }
        assertNull(issue)
    }
}
