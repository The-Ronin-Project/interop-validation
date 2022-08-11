package com.projectronin.interop.validation.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
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
        val exception = assertThrows<IllegalArgumentException> {
            resourceDAO.getResources(listOf(), Order.ASC, 2, null)
        }
        assertEquals("At least one status must be provided", exception.message)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - no resources found for requested statuses`() {
        val resources = resourceDAO.getResources(listOf(ResourceStatus.ADDRESSING), Order.ASC, 2, null)
        assertEquals(0, resources.size)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - ASC order honors create date time`() {
        val resources = resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 100, null)
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
        val resources = resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 100, null)
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
        val resources = resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.DESC, 100, null)
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
        val resources = resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.DESC, 100, null)
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
        val resources = resourceDAO.getResources(listOf(ResourceStatus.REPORTED), Order.ASC, 2, null)
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
        val exception = assertThrows<IllegalArgumentException> {
            resourceDAO.getResources(listOf(ResourceStatus.ADDRESSING), Order.ASC, 2, uuid)
        }
        assertEquals("No resource found for $uuid", exception.message)
    }

    @Test
    @DataSet(value = ["/dbunit/resource/MultipleResources.yaml"], cleanAfter = true)
    fun `getResources - after is honored with ASC order`() {
        val resources = resourceDAO.getResources(
            listOf(ResourceStatus.REPORTED),
            Order.ASC,
            100,
            UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7")
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
        val resources = resourceDAO.getResources(
            listOf(ResourceStatus.REPORTED),
            Order.DESC,
            100,
            UUID.fromString("9691d550-90f5-4fb8-83f9-e4a3840e37eb")
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
        val resources = resourceDAO.getResources(
            listOf(ResourceStatus.REPORTED),
            Order.ASC,
            100,
            UUID.fromString("4e0f261f-3dd4-41ad-9066-9733c41993a7")
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
        val resources = resourceDAO.getResources(
            listOf(ResourceStatus.REPORTED),
            Order.DESC,
            100,
            UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
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
}
