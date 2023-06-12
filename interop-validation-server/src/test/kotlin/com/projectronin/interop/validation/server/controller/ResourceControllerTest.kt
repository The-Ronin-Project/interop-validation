package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.data.model.toIssueDO
import com.projectronin.interop.validation.server.data.model.toMetadataDO
import com.projectronin.interop.validation.server.data.model.toResourceDO
import com.projectronin.interop.validation.server.generated.models.NewIssue
import com.projectronin.interop.validation.server.generated.models.NewMetadata
import com.projectronin.interop.validation.server.generated.models.NewResource
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.Resource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import com.projectronin.interop.validation.server.generated.models.UpdatableResourceStatus
import com.projectronin.interop.validation.server.generated.models.UpdateResource
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/*
    I tried to setup a test that proves the @Transactional annotation on addResource() is working, but for the
    life of me I couldn't get the SpringTransactionManager to play nice with our Liquibase extension and open a
    transaction.  You can run the project locally, though, connect to the MySql DB and watch it to confirm it's
    working.  If you force the insert to the issue table to fail it'll rollback the insert to resource.
 */
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

    private val resource3Id = UUID.fromString("6cb0d56c-c3cb-4838-9ab0-8946c762a5b5")
    private val resource3CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO3 = ResourceDO {
        id = resource3Id
        organizationId = "test"
        resourceType = "Patient"
        resource = "patient resource"
        status = ResourceStatus.REPORTED
        createDateTime = resource3CreateDtTm
    }

    private val resource4Id = UUID.fromString("237a48d9-15d5-4905-b5be-279aa268999c")
    private val resource4CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO4 = ResourceDO {
        id = resource4Id
        organizationId = "test"
        resourceType = "Patient"
        resource = "patient resource"
        status = ResourceStatus.REPORTED
        createDateTime = resource4CreateDtTm
    }

    private val newIssue1 = NewIssue(
        severity = Severity.FAILED,
        type = "pat-1",
        description = "No contact details",
        createDtTm = OffsetDateTime.now(),
        location = "Patient.contact"
    )

    private val newIssue2 = NewIssue(
        severity = Severity.WARNING,
        type = "pat-2",
        description = "No contact details",
        createDtTm = OffsetDateTime.now(),
        location = "Patient.contact"
    )

    private val newIssue3Meta = NewMetadata(
        registryEntryType = "value-set",
        valueSetName = "name of the value-set being referenced",
        valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec"),
        conceptMapName = "name of the concept-map being referenced",
        conceptMapUuid = UUID.fromString("29681f0f-41a7-4790-9122-ccff1ee50e0e"),
        version = "1"
    )

    private val newIssue3 = NewIssue(
        severity = Severity.WARNING,
        type = "pat-2",
        description = "No contact details",
        createDtTm = OffsetDateTime.now(),
        location = "Patient.contact",
        metadata = listOf(newIssue3Meta)
    )

    private val newResource = NewResource(
        organizationId = "testorg",
        resourceType = "Patient",
        resource = "the patient resource",
        issues = listOf(newIssue1, newIssue2),
        createDtTm = OffsetDateTime.now()
    )

    private val resourceWithIssues = NewResource(
        organizationId = "testorg",
        resourceType = "Patient",
        resource = "the patient resource",
        issues = listOf(newIssue1, newIssue3),
        createDtTm = OffsetDateTime.now()
    )

    private val issueTypePat1 = listOf("pat-1")
    private val issueTypePat1AndPat2 = listOf("pat-1", "pat-2")

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
            resourceDAO.getResources(ResourceStatus.values().toList(), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.FAILED
            )
        )

        val response = controller.getResources(null, Order.ASC, 10, afterUUID, null, null, null)
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
            resourceDAO.getResources(ResourceStatus.values().toList(), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.FAILED
            )
        )

        val response = controller.getResources(emptyList(), Order.ASC, 10, afterUUID, null, null, null)
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
            resourceDAO.getResources(listOf(ResourceStatus.IGNORED), Order.ASC, 10, afterUUID, null, null, null)
        } returns emptyList()

        val response = controller.getResources(listOf(ResourceStatus.IGNORED), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(0, resources.size)
    }

    @Test
    fun `getResources - supports no after UUID being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, null, null)
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
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
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
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING,
                Severity.FAILED
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
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
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1, resourceDO2)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id, resource2Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING,
                Severity.FAILED
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
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
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1, resourceDO2)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id, resource2Id)) } returns mapOf(
            resource1Id to setOf(Severity.FAILED),
            resource2Id to setOf(Severity.WARNING)
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
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

    @Test
    fun `getResources - supports organization id being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, "testorg", null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, "testorg", null, null)
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
    fun `getResources - supports resource type being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(
                Severity.WARNING
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", null)
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
    fun `addResource with multiple issues and meta`() {
        val resourceUUID = UUID.randomUUID()
        val issue1UUID = UUID.randomUUID()
        val issue3UUID = UUID.randomUUID()
        val metaUUID = UUID.randomUUID()
        every {
            resourceDAO.insertResource(resourceWithIssues.toResourceDO())
        } returns resourceUUID

        every {
            issueDAO.insertIssue(newIssue1.toIssueDO(resourceUUID))
        } returns issue1UUID

        every {
            issueDAO.insertIssue(newIssue3.toIssueDO(resourceUUID))
        } returns issue3UUID

        every {
            issueDAO.insertMetadata(newIssue3Meta.toMetadataDO(issue3UUID))
        } returns metaUUID

        val response = controller.addResource(resourceWithIssues)
        val resources = response.body!!

        assertEquals(resources.id, resourceUUID)
    }

    @Test
    fun `getResources - supports issue type being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", issueTypePat1)
        } returns listOf(resourceDO3)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource3Id)) } returns mapOf(
            resource3Id to setOf(
                Severity.FAILED
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", issueTypePat1)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource = Resource(
            id = resource3Id,
            organizationId = "test",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource3CreateDtTm
        )
        assertTrue(resources.contains(expectedResource))
    }

    @Test
    fun `getResources - supports multiple issue types being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", issueTypePat1AndPat2)
        } returns listOf(resourceDO3, resourceDO4)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource3Id, resource4Id)) } returns mapOf(
            resource3Id to setOf(
                Severity.FAILED
            ),
            resource4Id to setOf(
                Severity.FAILED
            )
        )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", issueTypePat1AndPat2)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(2, resources.size)

        val expectedResource1 = Resource(
            id = resource3Id,
            organizationId = "test",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource3CreateDtTm
        )
        val expectedResource2 = Resource(
            id = resource4Id,
            organizationId = "test",
            resourceType = "Patient",
            resource = "patient resource",
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource4CreateDtTm
        )
        assertTrue(resources.contains(expectedResource1))
        assertTrue(resources.contains(expectedResource2))
    }

    @Test
    fun `getResourceById - can return a resource`() {
        every {
            resourceDAO.getResource(resource1Id)
        } returns resourceDO1

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(Severity.WARNING)
        )

        val response = controller.getResourceById(resource1Id)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resource = response.body!!
        val expectedResource = Resource(
            id = resource1Id,
            organizationId = resourceDO1.organizationId,
            resourceType = resourceDO1.resourceType,
            resource = resourceDO1.resource,
            status = ResourceStatus.REPORTED,
            severity = Severity.WARNING,
            createDtTm = resource1CreateDtTm
        )
        assertEquals(expectedResource, resource)
    }

    @Test
    fun `getResourceById - handle multiple issue severities`() {
        every {
            resourceDAO.getResource(resource1Id)
        } returns resourceDO1

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf(
            resource1Id to setOf(Severity.WARNING),
            resource1Id to setOf(Severity.FAILED)
        )

        val response = controller.getResourceById(resource1Id)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resource = response.body!!
        val expectedResource = Resource(
            id = resource1Id,
            organizationId = resourceDO1.organizationId,
            resourceType = resourceDO1.resourceType,
            resource = resourceDO1.resource,
            status = ResourceStatus.REPORTED,
            severity = Severity.FAILED,
            createDtTm = resource1CreateDtTm
        )
        assertEquals(expectedResource, resource)
    }

    @Test
    fun `getResourceById - bad UUID returns a not found`() {
        every {
            resourceDAO.getResource(resource1Id)
        } returns null

        val response = controller.getResourceById(resource1Id)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getResourceById - resource with no issues returns a server error`() {
        every {
            resourceDAO.getResource(resource1Id)
        } returns resourceDO1

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns mapOf()

        val response = controller.getResourceById(resource1Id)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `insertResource - can insert new resource`() {
        val resourceUUID = UUID.randomUUID()
        val issue1UUID = UUID.randomUUID()
        val issue2UUID = UUID.randomUUID()

        every {
            resourceDAO.insertResource(newResource.toResourceDO())
        } returns resourceUUID

        every {
            issueDAO.insertIssue(newIssue1.toIssueDO(resourceUUID))
        } returns issue1UUID

        every {
            issueDAO.insertIssue(newIssue2.toIssueDO(resourceUUID))
        } returns issue2UUID

        val response = controller.addResource(newResource)
        val generatedId = response.body!!

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(resourceUUID, generatedId.id)
    }

    @Test
    fun `insertResource - handles error inserting resource`() {
        every {
            resourceDAO.insertResource(newResource.toResourceDO())
        } throws IllegalStateException("Bad insert")

        assertThrows<IllegalStateException> {
            controller.addResource(newResource)
        }
    }

    @Test
    fun `insertResource - handles error inserting issue`() {
        val resourceUUID = UUID.randomUUID()

        every {
            resourceDAO.insertResource(newResource.toResourceDO())
        } returns resourceUUID

        every {
            issueDAO.insertIssue(newIssue1.toIssueDO(resourceUUID))
        } throws IllegalStateException("Bad issue")

        assertThrows<IllegalStateException> {
            controller.addResource(newResource)
        }
    }

    @Test
    fun `updateResource returns error when no resource found`() {
        val resourceId = UUID.randomUUID()
        every { resourceDAO.getResource(resourceId) } returns null

        val response =
            controller.updateResource(resourceId, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `updateResource returns error when attempting to update a reprocessed resource`() {
        val resourceId = UUID.randomUUID()
        every { resourceDAO.getResource(resourceId) } returns mockk {
            every { status } returns ResourceStatus.REPROCESSED
        }

        val response =
            controller.updateResource(resourceId, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `updateResource returns error when update fails`() {
        val resourceId = UUID.randomUUID()
        every { resourceDAO.getResource(resourceId) } returns mockk {
            every { status } returns ResourceStatus.REPORTED
        }
        every { resourceDAO.updateResource(resourceId, captureLambda()) } returns null

        val response =
            controller.updateResource(resourceId, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `updateResource returns error when issue severities not found`() {
        every { resourceDAO.getResource(resource1Id) } returns mockk {
            every { status } returns ResourceStatus.REPORTED
        }

        every { resourceDAO.updateResource(resource1Id, captureLambda()) } returns resourceDO1
        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns emptyMap()

        val response =
            controller.updateResource(resource1Id, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `updateResource can update the resource`() {
        val resourceId = UUID.randomUUID()
        every { resourceDAO.getResource(resourceId) } returns mockk {
            every { status } returns ResourceStatus.REPORTED
        }

        val resourceDO = ResourceDO {
            id = resourceId
            organizationId = "testorg"
            resourceType = "Patient"
            resource = "patient resource"
            status = ResourceStatus.ADDRESSING
            createDateTime = resource2CreateDtTm
            updateDateTime = resource2UpdateDtTm
        }
        every { resourceDAO.updateResource(resourceId, captureLambda()) } returns resourceDO
        every { issueDAO.getIssueSeveritiesForResources(listOf(resourceId)) } returns mapOf(
            resourceId to setOf(Severity.FAILED)
        )

        val response =
            controller.updateResource(resourceId, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.OK, response.statusCode)

        val resource = response.body!!
        assertEquals(resourceId, resource.id)
        assertEquals("testorg", resource.organizationId)
        assertEquals("Patient", resource.resourceType)
        assertEquals("patient resource", resource.resource)
        assertEquals(ResourceStatus.ADDRESSING, resource.status)
        assertEquals(resource2CreateDtTm, resource.createDtTm)
        assertEquals(resource2UpdateDtTm, resource.updateDtTm)
        assertEquals(Severity.FAILED, resource.severity)
    }
}
