package com.projectronin.interop.validation.server.controller

import com.projectronin.event.interop.internal.v1.ResourceType
import com.projectronin.event.interop.resource.request.v1.InteropResourceRequestV1
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.common.jackson.JacksonUtil
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Location
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.kafka.KafkaRequestService
import com.projectronin.interop.validation.server.data.CommentDAO
import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.CommentDO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.data.model.toIssueDO
import com.projectronin.interop.validation.server.data.model.toMetadataDO
import com.projectronin.interop.validation.server.data.model.toResourceDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.NewIssue
import com.projectronin.interop.validation.server.generated.models.NewMetadata
import com.projectronin.interop.validation.server.generated.models.NewResource
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.server.generated.models.Resource
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import com.projectronin.interop.validation.server.generated.models.UpdatableResourceStatus
import com.projectronin.interop.validation.server.generated.models.UpdateResource
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
    private val resourceDAO = mockk<ResourceDAO>()
    private val issueDAO = mockk<IssueDAO>()
    private val commentDAO = mockk<CommentDAO>()
    private val kafkaRequestService = mockk<KafkaRequestService>()
    private val controller = ResourceController(resourceDAO, issueDAO, commentDAO, kafkaRequestService)

    private val resource1Id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5")
    private val resource1CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO1 =
        ResourceDO {
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
    private val resourceDO2 =
        ResourceDO {
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
    private val resourceDO3 =
        ResourceDO {
            id = resource3Id
            organizationId = "test"
            resourceType = "Patient"
            resource = "patient resource"
            status = ResourceStatus.REPORTED
            createDateTime = resource3CreateDtTm
        }

    private val resource4Id = UUID.fromString("237a48d9-15d5-4905-b5be-279aa268999c")
    private val resource4CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO4 =
        ResourceDO {
            id = resource4Id
            organizationId = "test"
            resourceType = "Patient"
            resource = "patient resource"
            status = ResourceStatus.REPORTED
            createDateTime = resource4CreateDtTm
        }

    private val newIssue1 =
        NewIssue(
            severity = Severity.FAILED,
            type = "pat-1",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact",
        )

    private val newIssue2 =
        NewIssue(
            severity = Severity.WARNING,
            type = "pat-2",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact",
        )

    private val newMetadata1 =
        NewMetadata(
            registryEntryType = "value-set",
            valueSetName = "name of the value-set being referenced",
            valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec"),
            conceptMapName = "name of the concept-map being referenced",
            conceptMapUuid = UUID.fromString("29681f0f-41a7-4790-9122-ccff1ee50e0e"),
            version = "1",
        )

    private val newMetadata2 =
        NewMetadata(
            registryEntryType = "value-set",
            valueSetName = "name of the value-set being referenced",
            valueSetUuid = UUID.fromString("778afb2e-8c0e-44a8-ad86-9058bcec"),
            conceptMapName = "name of the concept-map being referenced",
            conceptMapUuid = UUID.fromString("201ad507-64f7-4429-810f-94bdbd51f80a"),
            version = "2",
        )

    private val newIssue3 =
        NewIssue(
            severity = Severity.WARNING,
            type = "pat-2",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact",
            metadata = listOf(newMetadata1),
        )

    private val newIssue4 =
        NewIssue(
            severity = Severity.WARNING,
            type = "pat-2",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact",
            metadata = listOf(newMetadata1, newMetadata2),
        )

    private val newResource =
        NewResource(
            organizationId = "testorg",
            resourceType = "Patient",
            resource = JacksonUtil.writeJsonValue(Patient(id = Id("123"))),
            issues = listOf(newIssue1, newIssue2),
            createDtTm = OffsetDateTime.now(),
        )

    private val resourceWithIssues =
        NewResource(
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "the patient resource",
            issues = listOf(newIssue1, newIssue3),
            createDtTm = OffsetDateTime.now(),
        )

    private val resourceWithMetas =
        NewResource(
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "the patient resource",
            issues = listOf(newIssue4),
            createDtTm = OffsetDateTime.now(),
        )

    private val issueTypePat1 = listOf("pat-1")
    private val issueTypePat1AndPat2 = listOf("pat-1", "pat-2")

    @Test
    fun `getResources - all statuses are searched when null statuses provided`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(ResourceStatus.values().toList(), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.FAILED,
                    ),
            )

        val response = controller.getResources(null, Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - all statuses are searched when empty statuses provided`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(ResourceStatus.values().toList(), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.FAILED,
                    ),
            )

        val response = controller.getResources(emptyList(), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - empty list returned when none found`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.IGNORED), Order.ASC, 10, afterUUID, null, null, null)
        } returns emptyList()

        val response =
            controller.getResources(listOf(ResourceStatus.IGNORED), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(0, resources.size)
    }

    @Test
    fun `getResources - supports no after UUID being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                    ),
            )

        val response = controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.WARNING,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - returns proper resource severity when no failed issues`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                    ),
            )

        val response =
            controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.WARNING,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - returns proper resource severity when failed issue found`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                        Severity.FAILED,
                    ),
            )

        val response =
            controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - ignores any resources with no issue severities`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1, resourceDO2)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id, resource2Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                        Severity.FAILED,
                    ),
            )

        val response =
            controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - returns multiple resources`() {
        val afterUUID = UUID.randomUUID()
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        } returns listOf(resourceDO1, resourceDO2)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id, resource2Id)) } returns
            mapOf(
                resource1Id to setOf(Severity.FAILED),
                resource2Id to setOf(Severity.WARNING),
            )

        val response =
            controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, afterUUID, null, null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(2, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))

        val expectedResource2 =
            Resource(
                id = resource2Id,
                organizationId = "testorg",
                resourceType = "Condition",
                resource = "condition resource",
                status = ResourceStatus.REPROCESSED,
                severity = Severity.WARNING,
                createDtTm = resource2CreateDtTm,
                updateDtTm = resource2UpdateDtTm,
                reprocessDtTm = resource2ReprocessDtTm,
                reprocessedBy = "Josh",
            )
        assertTrue(resources.contains(expectedResource2))
    }

    @Test
    fun `getResources - supports organization id being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, "testorg", null, null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                    ),
            )

        val response =
            controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, "testorg", null, null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.WARNING,
                createDtTm = resource1CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
    }

    @Test
    fun `getResources - supports resource type being provided`() {
        every {
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", null)
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                    ),
            )

        val response =
            controller.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 10, null, null, "Patient", null)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource1 =
            Resource(
                id = resource1Id,
                organizationId = "testorg",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.WARNING,
                createDtTm = resource1CreateDtTm,
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
            issueDAO.insertMetadata(newMetadata1.toMetadataDO(issue3UUID))
        } returns metaUUID

        val response = controller.addResource(resourceWithIssues)
        val resources = response.body!!

        assertEquals(resources.id, resourceUUID)
    }

    @Test
    fun `addResource with multiple issues and multiple meta`() {
        val resourceUUID = UUID.randomUUID()
        val issue4UUID = UUID.randomUUID()
        val metaUUID = UUID.randomUUID()
        val meta2UUID = UUID.randomUUID()
        every {
            resourceDAO.insertResource(resourceWithMetas.toResourceDO())
        } returns resourceUUID

        every {
            issueDAO.insertIssue(newIssue4.toIssueDO(resourceUUID))
        } returns issue4UUID

        every {
            issueDAO.insertMetadata(newMetadata1.toMetadataDO(issue4UUID))
        } returns metaUUID

        every {
            issueDAO.insertMetadata(newMetadata2.toMetadataDO(issue4UUID))
        } returns meta2UUID

        val response = controller.addResource(resourceWithMetas)
        val resources = response.body!!

        assertEquals(resources.id, resourceUUID)
    }

    @Test
    fun `getResources - supports issue type being provided`() {
        every {
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                10,
                null,
                null,
                "Patient",
                issueTypePat1,
            )
        } returns listOf(resourceDO3)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource3Id)) } returns
            mapOf(
                resource3Id to
                    setOf(
                        Severity.FAILED,
                    ),
            )

        val response =
            controller.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                10,
                null,
                null,
                "Patient",
                issueTypePat1,
            )
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(1, resources.size)

        val expectedResource =
            Resource(
                id = resource3Id,
                organizationId = "test",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource3CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource))
    }

    @Test
    fun `getResources - supports multiple issue types being provided`() {
        every {
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                10,
                null,
                null,
                "Patient",
                issueTypePat1AndPat2,
            )
        } returns listOf(resourceDO3, resourceDO4)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource3Id, resource4Id)) } returns
            mapOf(
                resource3Id to
                    setOf(
                        Severity.FAILED,
                    ),
                resource4Id to
                    setOf(
                        Severity.FAILED,
                    ),
            )

        val response =
            controller.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                10,
                null,
                null,
                "Patient",
                issueTypePat1AndPat2,
            )
        assertEquals(HttpStatus.OK, response.statusCode)

        val resources = response.body!!
        assertEquals(2, resources.size)

        val expectedResource1 =
            Resource(
                id = resource3Id,
                organizationId = "test",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource3CreateDtTm,
            )
        val expectedResource2 =
            Resource(
                id = resource4Id,
                organizationId = "test",
                resourceType = "Patient",
                resource = "patient resource",
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource4CreateDtTm,
            )
        assertTrue(resources.contains(expectedResource1))
        assertTrue(resources.contains(expectedResource2))
    }

    @Test
    fun `getResourceById - can return a resource`() {
        every {
            resourceDAO.getResource(resource1Id)
        } returns resourceDO1

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to setOf(Severity.WARNING),
            )

        val response = controller.getResourceById(resource1Id)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resource = response.body!!
        val expectedResource =
            Resource(
                id = resource1Id,
                organizationId = resourceDO1.organizationId,
                resourceType = resourceDO1.resourceType,
                resource = resourceDO1.resource,
                status = ResourceStatus.REPORTED,
                severity = Severity.WARNING,
                createDtTm = resource1CreateDtTm,
            )
        assertEquals(expectedResource, resource)
    }

    @Test
    fun `getResourceById - handle multiple issue severities`() {
        every {
            resourceDAO.getResource(resource1Id)
        } returns resourceDO1

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to setOf(Severity.WARNING),
                resource1Id to setOf(Severity.FAILED),
            )

        val response = controller.getResourceById(resource1Id)
        assertEquals(HttpStatus.OK, response.statusCode)

        val resource = response.body!!
        val expectedResource =
            Resource(
                id = resource1Id,
                organizationId = resourceDO1.organizationId,
                resourceType = resourceDO1.resourceType,
                resource = resourceDO1.resource,
                status = ResourceStatus.REPORTED,
                severity = Severity.FAILED,
                createDtTm = resource1CreateDtTm,
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
        val resource = newResource.toResourceDO()
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource.clientFhirId!!,
                resource.organizationId,
                resource.resourceType,
            )
        } returns listOf()

        every {
            resourceDAO.insertResource(resource)
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
        val resource = newResource.toResourceDO()
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource.clientFhirId!!,
                resource.organizationId,
                resource.resourceType,
            )
        } returns listOf()
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
        val resource = newResource.toResourceDO()
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource.clientFhirId!!,
                resource.organizationId,
                resource.resourceType,
            )
        } returns listOf()

        every {
            resourceDAO.insertResource(resource)
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
        every { resourceDAO.getResource(resourceId) } returns
            mockk {
                every { status } returns ResourceStatus.REPROCESSED
            }

        val response =
            controller.updateResource(resourceId, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `updateResource returns error when update fails`() {
        val resourceId = UUID.randomUUID()
        every { resourceDAO.getResource(resourceId) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }
        every { resourceDAO.updateResource(resourceId, captureLambda()) } returns null

        val response =
            controller.updateResource(resourceId, UpdateResource(status = UpdatableResourceStatus.ADDRESSING))
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `updateResource returns error when issue severities not found`() {
        every { resourceDAO.getResource(resource1Id) } returns
            mockk {
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
        every { resourceDAO.getResource(resourceId) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }

        val resourceDO =
            ResourceDO {
                id = resourceId
                organizationId = "testorg"
                resourceType = "Patient"
                resource = "patient resource"
                status = ResourceStatus.ADDRESSING
                createDateTime = resource2CreateDtTm
                updateDateTime = resource2UpdateDtTm
                repeatCount = 3
                lastSeenDateTime = resource2UpdateDtTm
            }
        every { resourceDAO.updateResource(resourceId, captureLambda()) } returns resourceDO
        every { issueDAO.getIssueSeveritiesForResources(listOf(resourceId)) } returns
            mapOf(
                resourceId to setOf(Severity.FAILED),
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
        assertEquals(resource2UpdateDtTm, resource.lastSeenDtTm)
    }

    @Test
    fun `reprocessResource returns 404 when unknown ID provided`() {
        val resourceId = UUID.randomUUID()
        every { resourceDAO.getResource(resourceId) } returns null

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourceId, request)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `reprocessResource returns error when resource can not be deserialized`() {
        val resourcUUId = UUID.randomUUID()
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns "not-a-real-resource"
                every { clientFhirId } returns null
            }

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `reprocessResource returns error when no id is found on resource`() {
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = null),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { clientFhirId } returns null
            }

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `reprocessResource returns error when failure occurs during publishing`() {
        val resourceFhirId = UUID.randomUUID()
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = Id(resourceFhirId.toString())),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { resourceType } returns "Location"
                every { organizationId } returns "tenant"
                every { clientFhirId } returns null
            }

        every {
            kafkaRequestService.pushRequestEvent(
                "tenant",
                listOf("tenant-$resourceFhirId"),
                ResourceType.Location,
                "validation-server",
                InteropResourceRequestV1.FlowOptions(false, null),
            )
        } returns
            mockk {
                every { failures } returns listOf(mockk())
            }

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `reprocessResource works for resource with client FHIR ID`() {
        val resourceFhirId = UUID.randomUUID()
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = Id(resourceFhirId.toString())),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { resourceType } returns "Location"
                every { organizationId } returns "tenant"
                every { clientFhirId } returns null
            }

        every {
            kafkaRequestService.pushRequestEvent(
                "tenant",
                listOf("tenant-$resourceFhirId"),
                ResourceType.Location,
                "validation-server",
                InteropResourceRequestV1.FlowOptions(false, null),
            )
        } returns
            mockk {
                every { successful } returns listOf("tenant-$resourceFhirId")
                every { failures } returns emptyList()
            }

        val commentSlot = slot<CommentDO>()
        every { commentDAO.insertResourceComment(capture(commentSlot), resourcUUId) } returns UUID.randomUUID()

        val lambdaSlot = slot<(ResourceDO) -> Unit>()
        every { resourceDAO.updateResource(resourcUUId, capture(lambdaSlot)) } returns mockk()

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val commentDO = commentSlot.captured
        assertEquals("josh", commentDO.author)
        assertEquals("reprocessing", commentDO.text)

        lambdaSlot.captured(resourceDO1)
        assertEquals(ResourceStatus.REPROCESSED, resourceDO1.status)
        assertNotNull(resourceDO1.reprocessDateTime)
        assertEquals("josh", resourceDO1.reprocessedBy)

        verify(exactly = 1) { commentDAO.insertResourceComment(any(), resourcUUId) }
        verify(exactly = 1) { resourceDAO.updateResource(resourcUUId, any()) }
    }

    @Test
    fun `reprocessResource works for resource with Ronin FHIR ID`() {
        val resourceFhirId = UUID.randomUUID()
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = Id("tenant-$resourceFhirId")),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { resourceType } returns "Location"
                every { organizationId } returns "tenant"
                every { clientFhirId } returns null
            }

        every {
            kafkaRequestService.pushRequestEvent(
                "tenant",
                listOf("tenant-$resourceFhirId"),
                ResourceType.Location,
                "validation-server",
                InteropResourceRequestV1.FlowOptions(false, null),
            )
        } returns
            mockk {
                every { successful } returns listOf("tenant-$resourceFhirId")
                every { failures } returns emptyList()
            }

        val commentSlot = slot<CommentDO>()
        every { commentDAO.insertResourceComment(capture(commentSlot), resourcUUId) } returns UUID.randomUUID()

        val lambdaSlot = slot<(ResourceDO) -> Unit>()
        every { resourceDAO.updateResource(resourcUUId, capture(lambdaSlot)) } returns mockk()

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val commentDO = commentSlot.captured
        assertEquals("josh", commentDO.author)
        assertEquals("reprocessing", commentDO.text)

        lambdaSlot.captured(resourceDO1)
        assertEquals(ResourceStatus.REPROCESSED, resourceDO1.status)
        assertNotNull(resourceDO1.reprocessDateTime)
        assertEquals("josh", resourceDO1.reprocessedBy)

        verify(exactly = 1) { commentDAO.insertResourceComment(any(), resourcUUId) }
        verify(exactly = 1) { resourceDAO.updateResource(resourcUUId, any()) }
    }

    @Test
    fun `reprocessResource supports no refreshNormalization being provided`() {
        val resourceFhirId = UUID.randomUUID()
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = Id("tenant-$resourceFhirId")),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { resourceType } returns "Location"
                every { organizationId } returns "tenant"
                every { clientFhirId } returns null
            }

        every {
            kafkaRequestService.pushRequestEvent(
                "tenant",
                listOf("tenant-$resourceFhirId"),
                ResourceType.Location,
                "validation-server",
                InteropResourceRequestV1.FlowOptions(false, null),
            )
        } returns
            mockk {
                every { successful } returns listOf("tenant-$resourceFhirId")
                every { failures } returns emptyList()
            }

        val commentSlot = slot<CommentDO>()
        every { commentDAO.insertResourceComment(capture(commentSlot), resourcUUId) } returns UUID.randomUUID()

        val lambdaSlot = slot<(ResourceDO) -> Unit>()
        every { resourceDAO.updateResource(resourcUUId, capture(lambdaSlot)) } returns mockk()

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val commentDO = commentSlot.captured
        assertEquals("josh", commentDO.author)
        assertEquals("reprocessing", commentDO.text)

        lambdaSlot.captured(resourceDO1)
        assertEquals(ResourceStatus.REPROCESSED, resourceDO1.status)
        assertNotNull(resourceDO1.reprocessDateTime)
        assertEquals("josh", resourceDO1.reprocessedBy)

        verify(exactly = 1) { commentDAO.insertResourceComment(any(), resourcUUId) }
        verify(exactly = 1) { resourceDAO.updateResource(resourcUUId, any()) }
    }

    @Test
    fun `reprocessResource supports a true refreshNormalization`() {
        val resourceFhirId = UUID.randomUUID()
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = Id("tenant-$resourceFhirId")),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { resourceType } returns "Location"
                every { organizationId } returns "tenant"
                every { clientFhirId } returns resourceFhirId.toString()
            }

        val flowOptionsSlot = slot<InteropResourceRequestV1.FlowOptions>()
        every {
            kafkaRequestService.pushRequestEvent(
                "tenant",
                listOf("tenant-$resourceFhirId"),
                ResourceType.Location,
                "validation-server",
                capture(flowOptionsSlot),
            )
        } returns
            mockk {
                every { successful } returns listOf("tenant-$resourceFhirId")
                every { failures } returns emptyList()
            }

        val commentSlot = slot<CommentDO>()
        every { commentDAO.insertResourceComment(capture(commentSlot), resourcUUId) } returns UUID.randomUUID()

        val lambdaSlot = slot<(ResourceDO) -> Unit>()
        every { resourceDAO.updateResource(resourcUUId, capture(lambdaSlot)) } returns mockk()

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
                refreshNormalization = true,
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val flowOptions = flowOptionsSlot.captured
        assertEquals(false, flowOptions.disableDownstreamResources)
        assertNotNull(flowOptions.normalizationRegistryMinimumTime)

        val commentDO = commentSlot.captured
        assertEquals("josh", commentDO.author)
        assertEquals("reprocessing", commentDO.text)

        lambdaSlot.captured(resourceDO1)
        assertEquals(ResourceStatus.REPROCESSED, resourceDO1.status)
        assertNotNull(resourceDO1.reprocessDateTime)
        assertEquals("josh", resourceDO1.reprocessedBy)

        verify(exactly = 1) { commentDAO.insertResourceComment(any(), resourcUUId) }
        verify(exactly = 1) { resourceDAO.updateResource(resourcUUId, any()) }
    }

    @Test
    fun `reprocessResource supports a false refreshNormalization`() {
        val resourceFhirId = UUID.randomUUID()
        val resourcUUId = UUID.randomUUID()
        val resourceJson =
            JacksonManager.objectMapper.writeValueAsString(
                Location(id = Id("tenant-$resourceFhirId")),
            )
        every { resourceDAO.getResource(resourcUUId) } returns
            mockk {
                every { resource } returns resourceJson
                every { resourceType } returns "Location"
                every { organizationId } returns "tenant"
                every { clientFhirId } returns null
            }

        every {
            kafkaRequestService.pushRequestEvent(
                "tenant",
                listOf("tenant-$resourceFhirId"),
                ResourceType.Location,
                "validation-server",
                InteropResourceRequestV1.FlowOptions(false, null),
            )
        } returns
            mockk {
                every { successful } returns listOf("tenant-$resourceFhirId")
                every { failures } returns emptyList()
            }

        val commentSlot = slot<CommentDO>()
        every { commentDAO.insertResourceComment(capture(commentSlot), resourcUUId) } returns UUID.randomUUID()

        val lambdaSlot = slot<(ResourceDO) -> Unit>()
        every { resourceDAO.updateResource(resourcUUId, capture(lambdaSlot)) } returns mockk()

        val request =
            ReprocessResourceRequest(
                user = "josh",
                comment = "reprocessing",
                refreshNormalization = false,
            )
        val response = controller.reprocessResource(resourcUUId, request)
        assertEquals(HttpStatus.OK, response.statusCode)

        val commentDO = commentSlot.captured
        assertEquals("josh", commentDO.author)
        assertEquals("reprocessing", commentDO.text)

        lambdaSlot.captured(resourceDO1)
        assertEquals(ResourceStatus.REPROCESSED, resourceDO1.status)
        assertNotNull(resourceDO1.reprocessDateTime)
        assertEquals("josh", resourceDO1.reprocessedBy)

        verify(exactly = 1) { commentDAO.insertResourceComment(any(), resourcUUId) }
        verify(exactly = 1) { resourceDAO.updateResource(resourcUUId, any()) }
    }

    @Test
    fun `insertResource - turns into update for duplicate resource`() {
        val resource = newResource.toResourceDO()
        val issue1 = newIssue1.toIssueDO(resource1Id)
        val issue2 = newIssue2.toIssueDO(resource1Id)
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource.clientFhirId!!,
                resource.organizationId,
                resource.resourceType,
            )
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                        Severity.FAILED,
                    ),
            )

        every {
            issueDAO.getIssues(resource1Id, listOf(IssueStatus.REPORTED, IssueStatus.ADDRESSING), Order.ASC, 200, null)
        } returns listOf(issue1, issue2)

        every { resourceDAO.getResource(resource1Id) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }

        every { resourceDAO.updateResource(resource1Id, captureLambda()) } returns resourceDO1

        val response = controller.addResource(newResource)
        val generatedId = response.body!!

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(resource1Id, generatedId.id)
    }

    @Test
    fun `insert resource for multiple resources with different issues`() {
        val newResource1 =
            NewResource(
                organizationId = "testorg",
                resourceType = "Patient",
                resource = JacksonUtil.writeJsonValue(Patient(id = Id("123"))),
                issues = listOf(newIssue1),
                createDtTm = OffsetDateTime.now(),
            )
        val newResource2 =
            NewResource(
                organizationId = "testorg",
                resourceType = "Patient",
                resource = JacksonUtil.writeJsonValue(Patient(id = Id("123"))),
                issues = listOf(newIssue1, newIssue2),
                createDtTm = OffsetDateTime.now(),
            )
        val resource1 = newResource1.toResourceDO()
        val issue1 = newIssue1.toIssueDO(resource1Id)
        val issue2 = newIssue2.toIssueDO(resource3Id)
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource1.clientFhirId!!,
                resource1.organizationId,
                resource1.resourceType,
            )
        } returns listOf(resourceDO1, resourceDO3)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.FAILED,
                    ),
            )
        every { issueDAO.getIssueSeveritiesForResources(listOf(resource3Id)) } returns
            mapOf(
                resource3Id to
                    setOf(
                        Severity.WARNING,
                    ),
            )
        every { resourceDAO.getResource(resource1Id) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }
        every { resourceDAO.getResource(resource3Id) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }
        every {
            issueDAO.getIssues(resource1Id, listOf(IssueStatus.REPORTED, IssueStatus.ADDRESSING), Order.ASC, 200, null)
        } returns listOf(issue1)
        every {
            issueDAO.getIssues(resource3Id, listOf(IssueStatus.REPORTED, IssueStatus.ADDRESSING), Order.ASC, 200, null)
        } returns listOf(issue2)
        val resourceUUID = UUID.randomUUID()
        val issue1UUID = UUID.randomUUID()
        val issue3UUID = UUID.randomUUID()
        every {
            resourceDAO.insertResource(newResource2.toResourceDO())
        } returns resourceUUID

        every {
            issueDAO.insertIssue(newIssue1.toIssueDO(resourceUUID))
        } returns issue1UUID

        every {
            issueDAO.insertIssue(newIssue2.toIssueDO(resourceUUID))
        } returns issue3UUID

        val response = controller.addResource(newResource2)
        val generatedId = response.body!!

        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotEquals(resource1Id, generatedId.id)
        assertEquals(resourceUUID, generatedId.id)
    }

    @Test
    fun `de-duplication update fails`() {
        val resource = newResource.toResourceDO()
        val issue1 = newIssue1.toIssueDO(resource1Id)
        val issue2 = newIssue2.toIssueDO(resource1Id)
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource.clientFhirId!!,
                resource.organizationId,
                resource.resourceType,
            )
        } returns listOf(resourceDO1)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                        Severity.FAILED,
                    ),
            )
        every {
            issueDAO.getIssues(resource1Id, listOf(IssueStatus.REPORTED, IssueStatus.ADDRESSING), Order.ASC, 200, null)
        } returns listOf(issue1, issue2)

        every { resourceDAO.getResource(resource1Id) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }
        every { resourceDAO.updateResource(resource1Id, captureLambda()) } returns null

        val response = controller.addResource(newResource)
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `de-duplication increments already duplicate resource`() {
        val resourceDO =
            ResourceDO {
                id = resource1Id
                organizationId = "testorg"
                resourceType = "Patient"
                resource = "patient resource"
                status = ResourceStatus.REPORTED
                createDateTime = resource1CreateDtTm
                repeatCount = 2
                lastSeenDateTime = resource3CreateDtTm
                updateDateTime = resource2UpdateDtTm
            }
        val resource = newResource.toResourceDO()
        val issue1 = newIssue1.toIssueDO(resource1Id)
        val issue2 = newIssue2.toIssueDO(resource1Id)
        every {
            resourceDAO.getResourcesByFHIRID(
                listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                resource.clientFhirId!!,
                resource.organizationId,
                resource.resourceType,
            )
        } returns listOf(resourceDO)

        every { issueDAO.getIssueSeveritiesForResources(listOf(resource1Id)) } returns
            mapOf(
                resource1Id to
                    setOf(
                        Severity.WARNING,
                        Severity.FAILED,
                    ),
            )

        every {
            issueDAO.getIssues(resource1Id, listOf(IssueStatus.REPORTED, IssueStatus.ADDRESSING), Order.ASC, 200, null)
        } returns listOf(issue1, issue2)

        every { resourceDAO.getResource(resource1Id) } returns
            mockk {
                every { status } returns ResourceStatus.REPORTED
            }

        every { resourceDAO.updateResource(resource1Id, captureLambda()) } returns resourceDO

        val response = controller.addResource(newResource)
        val generatedId = response.body!!

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(resource1Id, generatedId.id)
    }
}
