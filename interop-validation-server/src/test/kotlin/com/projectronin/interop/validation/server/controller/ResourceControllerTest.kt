package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.Resource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ResourceControllerTest {
    lateinit var resourceDAO: ResourceDAO
    lateinit var issueDAO: IssueDAO
    lateinit var controller: ResourceController

    private val resource1Id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5")
    private val resource1CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO1 = ResourceDO {
        id = resource1Id
        organizationId = "testorg"
        resourceType = "Patient"
        resource = "patient resource"
        status = ResourceStatus.REPORTED
        createDateTime = resource1CreateDtTm
    }

    private val resource2Id = UUID.fromString("f1907389-d2c2-4b7e-a518-828a68127e5c")
    private val resource2CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2)
    private val resource2UpdateDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
    private val resource2ReprocessDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO2 = ResourceDO {
        id = resource2Id
        organizationId = "testorg"
        resourceType = "Condition"
        resource = "condition resource"
        status = ResourceStatus.REPROCESSED
        createDateTime = resource2CreateDtTm
        updateDateTime = resource2UpdateDtTm
        reprocessDateTime = resource2ReprocessDtTm
        reprocessedBy = "Josh"
    }

    @BeforeEach
    fun setup() {
        resourceDAO = mockk()
        issueDAO = mockk()
        controller = ResourceController(resourceDAO, issueDAO)
    }

    @Test
    fun `getResources - all statuses are searched when null statuses provided`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(ResourceStatus.values().toList(), Order.ASC, 10, afterUUID)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.FAILED
            )
        )

        val response = controller.getResources(null, Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - all statuses are searched when empty statuses provided`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(ResourceStatus.values().toList(), Order.ASC, 10, afterUUID)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.FAILED
            )
        )

        val response = controller.getResources(emptyList(), Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - empty list returned when none found`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.IGNORED), Order.ASC, 10, afterUUID)
        } returns emptyList()

        val response = controller.getResources(listOf(ResourceStatus.IGNORED), Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(0, resources.size)
    }

    @Test
    fun `getResources - supports no after UUID being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.WARNING,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - returns proper resource severity when no failed issues`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.WARNING,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - returns proper resource severity when failed issue found`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING, Severity.FAILED
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - ignores any resources with no issue severities`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        } returns listOf(resourceDO1, resourceDO2)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id, resource2Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING, Severity.FAILED
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - returns multiple resources`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        } returns listOf(resourceDO1, resourceDO2)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id, resource2Id)) } returns mapOf(
            resource1Id to setOf(Severity.FAILED), resource2Id to setOf(Severity.WARNING)
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(2, resources.size)

        val expectedResource1 = Resource(
            id = resource1Id,
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource1CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))

        val expectedResource2 = Resource(
            id = resource2Id,
            organizationId = "testorg",
            resourceType = "Condition",
            resource = "condition resource",
            status = ResourceStatus.REPROCESSED,
            severity = Severity.WARNING,
            createDtTm = resource2CreateDtTm,
            updateDtTm = resource2UpdateDtTm,
            reprocessDtTm = resource2ReprocessDtTm,
            reprocessedBy = "Josh"
        )
        assertTrue(resources.contains(expectedResource2))
    }
}
