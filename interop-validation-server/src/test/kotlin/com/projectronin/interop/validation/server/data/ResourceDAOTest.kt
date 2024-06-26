package com.projectronin.interop.validation.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.github.database.rider.core.api.dataset.ExpectedDataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
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
class ResourceDAOTest {
    init {
        // We have to set this to prevent some DBUnit weirdness around UTC and the local time zone that DST seems to cause.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    private val resourceDAO = ResourceDAO(KtormHelper.database())
    private val emptyIssueType = emptyList<String>()

    @Test
    @DataSet(value = ["/dbunit/resource/SingleResource.yaml"], cleanAfter = true)
    fun `getResource - no resource found`() {
        val resource = resourceDAO.getResource(UUID.randomUUID())
        assertNull(resource)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/SingleResource.yaml"], cleanAfter = true)
    fun `getResource - resource found`() {
        val uuid = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val resource = resourceDAO.getResource(uuid)
        assertNotNull(resource)

        resource!!
        assertEquals(uuid, resource.id)
        assertEquals("testorg", resource.organizationId)
        assertEquals("Patient", resource.resourceType)
        assertEquals("the patient resource", resource.resource)
        assertEquals(ResourceStatus.REPROCESSED, resource.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource.createDateTime)
        assertEquals(OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC), resource.updateDateTime)
        assertEquals(OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC), resource.reprocessDateTime)
        assertEquals("User 1", resource.reprocessedBy)
    }

    @Test
    fun `getResources - exception thrown if no statuses provided`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                resourceDAO.getResources(listOf(), Order.ASC, 2, null, null, null, null)
            }
        assertEquals("At least one status must be provided", exception.message)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - no resources found for requested statuses`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.ADDRESSING), Order.ASC, 2, null, null, null, null)
        assertEquals(0, resources.size)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResourcesWithFHIRID.yaml"], cleanAfter = true)
    fun `getResourcesByFHIRID - works`() {
        val resources =
            resourceDAO.getResourcesByFHIRID(listOf(ResourceStatus.REPORTED), "9eec9890-b926-4feb-82d9-daf5a4d3140c", "testorg", "Patient")
        assertEquals(2, resources.size)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - ASC order honors create date time`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 100, null, null, null, null)
        assertEquals(4, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Appointment", resource2.resourceType)
        assertEquals("the appointment resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)

        val resource3 = resources[2]
        assertEquals(UUID.fromString("9691d550-90f5-4fb8-83f9-e4a3840e37eb"), resource3.id)
        assertEquals("testorg", resource3.organizationId)
        assertEquals("Patient", resource3.resourceType)
        assertEquals("another patient resource", resource3.resource)
        assertEquals(ResourceStatus.REPORTED, resource3.status)
        assertEquals(OffsetDateTime.of(2022, 8, 3, 11, 18, 0, 0, ZoneOffset.UTC), resource3.createDateTime)
        assertNull(resource3.updateDateTime)
        assertNull(resource3.reprocessDateTime)
        assertNull(resource3.reprocessedBy)

        val resource4 = resources[3]
        assertEquals(UUID.fromString("ba02d211-b42a-43ef-a0ba-de2d6e5b8429"), resource4.id)
        assertEquals("testorg", resource4.organizationId)
        assertEquals("Patient", resource4.resourceType)
        assertEquals("yet another patient resource", resource4.resource)
        assertEquals(ResourceStatus.REPORTED, resource4.status)
        assertEquals(OffsetDateTime.of(2022, 8, 4, 11, 18, 0, 0, ZoneOffset.UTC), resource4.createDateTime)
        assertNull(resource4.updateDateTime)
        assertNull(resource4.reprocessDateTime)
        assertNull(resource4.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MatchingDateTimeResources.yaml"], cleanAfter = true)
    fun `getResources - ASC order honors id`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 100, null, null, null, null)
        assertEquals(2, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Appointment", resource1.resourceType)
        assertEquals("the appointment resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Patient", resource2.resourceType)
        assertEquals("the patient resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - DESC order honors create date time`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.DESC, 100, null, null, null, null)
        assertEquals(4, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("ba02d211-b42a-43ef-a0ba-de2d6e5b8429"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("yet another patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 4, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("9691d550-90f5-4fb8-83f9-e4a3840e37eb"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Patient", resource2.resourceType)
        assertEquals("another patient resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 3, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)

        val resource3 = resources[2]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource3.id)
        assertEquals("testorg", resource3.organizationId)
        assertEquals("Appointment", resource3.resourceType)
        assertEquals("the appointment resource", resource3.resource)
        assertEquals(ResourceStatus.REPORTED, resource3.status)
        assertEquals(OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC), resource3.createDateTime)
        assertNull(resource3.updateDateTime)
        assertNull(resource3.reprocessDateTime)
        assertNull(resource3.reprocessedBy)

        val resource4 = resources[3]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource4.id)
        assertEquals("testorg", resource4.organizationId)
        assertEquals("Patient", resource4.resourceType)
        assertEquals("the patient resource", resource4.resource)
        assertEquals(ResourceStatus.REPORTED, resource4.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource4.createDateTime)
        assertNull(resource4.updateDateTime)
        assertNull(resource4.reprocessDateTime)
        assertNull(resource4.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MatchingDateTimeResources.yaml"], cleanAfter = true)
    fun `getResources - DESC order honors id`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.DESC, 100, null, null, null, null)
        assertEquals(2, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Appointment", resource2.resourceType)
        assertEquals("the appointment resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - limit is honored`() {
        val resources = resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 2, null, null, null, null)
        assertEquals(2, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Appointment", resource2.resourceType)
        assertEquals("the appointment resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - no resources found for after UUID`() {
        val uuid = UUID.randomUUID()
        val exception =
            assertThrows<IllegalArgumentException> {
                resourceDAO.getResources(listOf(ResourceStatus.ADDRESSING), Order.ASC, 2, uuid, null, null, null)
            }
        assertEquals("No resource found for $uuid", exception.message)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - after is honored with ASC order`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                100,
                UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"),
                null,
                null,
                null,
            )
        assertEquals(2, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("9691d550-90f5-4fb8-83f9-e4a3840e37eb"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("another patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 3, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("ba02d211-b42a-43ef-a0ba-de2d6e5b8429"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Patient", resource2.resourceType)
        assertEquals("yet another patient resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 4, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - after is honored with DESC order`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.DESC,
                100,
                UUID.fromString("9691d550-90f5-4fb8-83f9-e4a3840e37eb"),
                null,
                null,
                null,
            )
        assertEquals(2, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Appointment", resource1.resourceType)
        assertEquals("the appointment resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Patient", resource2.resourceType)
        assertEquals("the patient resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource2.createDateTime)
        assertNull(resource2.updateDateTime)
        assertNull(resource2.reprocessDateTime)
        assertNull(resource2.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MatchingDateTimeResources.yaml"], cleanAfter = true)
    fun `getResources - after supports equivalent create date times with ASC order`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                100,
                UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"),
                null,
                null,
                null,
            )
        assertEquals(1, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MatchingDateTimeResources.yaml"], cleanAfter = true)
    fun `getResources - after supports equivalent create date times with DESC order`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.DESC,
                100,
                UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"),
                null,
                null,
                null,
            )
        assertEquals(1, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Appointment", resource1.resourceType)
        assertEquals("the appointment resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/FilteringResources.yaml"], cleanAfter = true)
    fun `getResources - no resources found for requested organization`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 2, null, "unknown", null, null)
        assertEquals(0, resources.size)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/FilteringResources.yaml"], cleanAfter = true)
    fun `getResources - resources found for requested organization`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 2, null, "testorg2", null, null)
        assertEquals(1, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("c59976b1-bb22-41c8-8a00-8b15c37b2d06"), resource1.id)
        assertEquals("testorg2", resource1.organizationId)
        assertEquals("Location", resource1.resourceType)
        assertEquals("A Location resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2023, 1, 6, 17, 0, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/FilteringResources.yaml"], cleanAfter = true)
    fun `getResources - no resources found for requested resource type`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                2,
                null,
                null,
                "DocumentReference",
                null,
            )
        assertEquals(0, resources.size)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/FilteringResources.yaml"], cleanAfter = true)
    fun `getResources - resources found for requested resource type`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 2, null, null, "Location", null)
        assertEquals(1, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("c59976b1-bb22-41c8-8a00-8b15c37b2d06"), resource1.id)
        assertEquals("testorg2", resource1.organizationId)
        assertEquals("Location", resource1.resourceType)
        assertEquals("A Location resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2023, 1, 6, 17, 0, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/SingleResourceIssueType.yaml"], cleanAfter = true)
    fun `getResources - resource found for requested issue type`() {
        val resources =
            resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 2, null, null, null, listOf("pat-1"))
        assertEquals(1, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/DoubleResourceIssueType.yaml"], cleanAfter = true)
    fun `getResources - resources found for multiple requested issue type`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                2,
                null,
                null,
                null,
                listOf("pat-1", "pat-2"),
            )
        assertEquals(1, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResourcesMultipleIssues.yaml"], cleanAfter = true)
    fun `getResources - multiple resources found with multiple issue types that were requested`() {
        val resources =
            resourceDAO.getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                8,
                null,
                null,
                null,
                listOf("pat-1", "pat-2"),
            )
        assertEquals(2, resources.size)

        val resource1 = resources[0]
        assertEquals(UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770"), resource1.id)
        assertEquals("testorg", resource1.organizationId)
        assertEquals("Patient", resource1.resourceType)
        assertEquals("the patient resource", resource1.resource)
        assertEquals(ResourceStatus.REPORTED, resource1.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)

        val resource2 = resources[1]
        assertEquals(UUID.fromString("b417d13a-234e-410a-98d1-b2c700ae1446"), resource2.id)
        assertEquals("testorg", resource2.organizationId)
        assertEquals("Patient", resource2.resourceType)
        assertEquals("another patient resource", resource2.resource)
        assertEquals(ResourceStatus.REPORTED, resource2.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource1.createDateTime)
        assertNull(resource1.updateDateTime)
        assertNull(resource1.reprocessDateTime)
        assertNull(resource1.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/NoResources.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/SingleResourceWithoutFhirID.yaml"], ignoreCols = ["resource_id"])
    fun `insertResource - can insert`() {
        resourceDAO.insertResource(
            ResourceDO {
                organizationId = "testorg"
                resourceType = "Patient"
                resource = "the patient resource"
                status = ResourceStatus.REPROCESSED
                createDateTime = OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC)
                updateDateTime = OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC)
                reprocessDateTime = OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC)
                reprocessedBy = "User 1"
            },
        )
    }

    @Test
    @DataSet(value = ["/dbunit/resource/NoResources.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/SingleResource.yaml"], ignoreCols = ["resource_id"])
    fun `insertResource - can insert with fhirID`() {
        resourceDAO.insertResource(
            ResourceDO {
                organizationId = "testorg"
                resourceType = "Patient"
                resource = "the patient resource"
                status = ResourceStatus.REPROCESSED
                createDateTime = OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC)
                updateDateTime = OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC)
                reprocessDateTime = OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC)
                reprocessedBy = "User 1"
                clientFhirId = "Test"
            },
        )
    }

    @Test
    @DataSet(value = ["/dbunit/resource/NoResources.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/NoResources.yaml"], ignoreCols = ["resource_id"])
    fun `insertResource - handles failed insert`() {
        assertThrows<IllegalStateException> {
            resourceDAO.insertResource(
                // createDateTime is not nullable and MySql won't set a default value for times, so this should throw an exception
                ResourceDO {
                    organizationId = "testorg"
                    resourceType = "Patient"
                    resource = "the patient resource"
                    status = ResourceStatus.REPROCESSED
                    // createDateTime = OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC)
                    updateDateTime = OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC)
                    reprocessDateTime = OffsetDateTime.of(2022, 8, 8, 13, 6, 0, 0, ZoneOffset.UTC)
                    reprocessedBy = "User 1"
                },
            )
        }
    }

    @Test
    @DataSet(value = ["/dbunit/resource/InitialResource.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/UpdatedResource.yaml"])
    fun `updateResource`() {
        val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val resource =
            resourceDAO.updateResource(resourceId) {
                it.organizationId = "newtest"
                it.status = ResourceStatus.IGNORED
                it.updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
            }

        resource!!
        assertEquals(resourceId, resource.id)
        assertEquals("newtest", resource.organizationId)
        assertEquals("Patient", resource.resourceType)
        assertEquals("the patient resource", resource.resource)
        assertEquals(ResourceStatus.IGNORED, resource.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource.createDateTime)
        assertEquals(OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource.updateDateTime)
        assertNull(resource.reprocessDateTime)
        assertNull(resource.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/InitialResource.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/UpdatedResource.yaml"], ignoreCols = ["update_dt_tm"])
    fun `updateResource with no provided updateDateTime`() {
        val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val resource =
            resourceDAO.updateResource(resourceId) {
                it.organizationId = "newtest"
                it.status = ResourceStatus.IGNORED
            }

        resource!!
        assertEquals(resourceId, resource.id)
        assertEquals("newtest", resource.organizationId)
        assertEquals("Patient", resource.resourceType)
        assertEquals("the patient resource", resource.resource)
        assertEquals(ResourceStatus.IGNORED, resource.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource.createDateTime)
        assertNotNull(resource.updateDateTime)
        assertNull(resource.reprocessDateTime)
        assertNull(resource.reprocessedBy)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/InitialResource.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/InitialResource.yaml"])
    fun `updateResource for unknown resource`() {
        val resource =
            resourceDAO.updateResource(UUID.randomUUID()) {
                it.status = ResourceStatus.IGNORED
            }
        assertNull(resource)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/InitialResource.yaml"], cleanAfter = true)
    @ExpectedDataSet(value = ["/dbunit/resource/UpdatedResourceRepeats.yaml"])
    fun `updateResource with repeats`() {
        val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
        val resource =
            resourceDAO.updateResource(resourceId) {
                it.repeatCount = 3
                it.lastSeenDateTime = OffsetDateTime.of(2023, 9, 26, 12, 5, 0, 0, ZoneOffset.UTC)
                it.updateDateTime = OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC)
            }

        resource!!
        assertEquals(resourceId, resource.id)
        assertEquals("Patient", resource.resourceType)
        assertEquals("the patient resource", resource.resource)
        assertEquals(ResourceStatus.REPORTED, resource.status)
        assertEquals(OffsetDateTime.of(2022, 8, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource.createDateTime)
        assertEquals(OffsetDateTime.of(2022, 9, 1, 11, 18, 0, 0, ZoneOffset.UTC), resource.updateDateTime)
        assertEquals(3, resource.repeatCount)
        assertEquals(OffsetDateTime.of(2023, 9, 26, 12, 5, 0, 0, ZoneOffset.UTC), resource.lastSeenDateTime)
        assertNull(resource.reprocessDateTime)
        assertNull(resource.reprocessedBy)
    }
}
