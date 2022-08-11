package com.projectronin.interop.validation.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.validation.server.generated.models.Severity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
}
