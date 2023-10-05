package com.projectronin.interop.validation.server

import com.projectronin.ehr.dataauthority.models.kafka.EhrDAKafkaTopic
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.validation.server.data.ResourceDAO
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.kafka.common.errors.InterruptException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

internal class EHRDAListenerTest {
    private val kafkaClient = mockk<KafkaClient>()
    private val topics = listOf(
        mockk<EhrDAKafkaTopic> {
            every { topicName } returns "ehrdaTopic.patient.v1"
            every { resourceClass } returns Patient::class
        }
    )
    private val resourceDAO = mockk<ResourceDAO>()
    private val listener = EHRDAListener(kafkaClient, topics, resourceDAO)

    @Test
    fun `poll works`() {
        val fakeResource = mockk<Patient>()

        every { fakeResource.findFhirId() } returns "fhirId"
        every { fakeResource.findTenantId() } returns "tenantId"
        every { fakeResource.resourceType } returns "Patient"

        every { kafkaClient.retrieveMultiTopicEvents(any(), any(), any(), any()) } returns listOf(
            mockk {
                every { data } returns fakeResource
            }
        )
        every { resourceDAO.getResourcesByFHIRID(any(), "fhirId", "tenantId", "Patient") } returns listOf(
            mockk {
                every { id } returns UUID.randomUUID()
            }
        )
        every { resourceDAO.updateResource(any(), any()) } returns mockk()
        listener.poll()
        verify { resourceDAO.updateResource(any(), any()) }
    }

    @Test
    fun `interrupted poll does not throw exception`() {
        every {
            kafkaClient.retrieveMultiTopicEvents(
                any(),
                any(),
                any(),
                any()
            )
        } throws InterruptException("Interrupted")

        listener.poll()

        verify { kafkaClient.retrieveMultiTopicEvents(any(), any(), any(), any()) }
    }

    @Test
    fun `other exceptions are thrown`() {
        every { kafkaClient.retrieveMultiTopicEvents(any(), any(), any(), any()) } throws RuntimeException()

        assertThrows<RuntimeException> { listener.poll() }

        verify { kafkaClient.retrieveMultiTopicEvents(any(), any(), any(), any()) }
    }

    @Test
    fun `type map works`() {
        Assertions.assertEquals(listener.typeMap["ronin.ehr-data-authority.patient.create"], Patient::class)
        Assertions.assertEquals(listener.typeMap["ronin.ehr-data-authority.patient.update"], Patient::class)
    }
}
