package com.projectronin.interop.validation.server

import com.projectronin.ehr.dataauthority.models.kafka.KafkaTopicConfig
import com.projectronin.interop.common.jackson.JacksonManager.Companion.objectMapper
import com.projectronin.interop.fhir.r4.CodeSystem
import com.projectronin.interop.fhir.r4.datatype.Identifier
import com.projectronin.interop.fhir.r4.datatype.primitive.FHIRString
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.kafka.model.KafkaAction
import com.projectronin.interop.kafka.model.KafkaEvent
import com.projectronin.interop.kafka.spring.KafkaBootstrapConfig
import com.projectronin.interop.kafka.spring.KafkaCloudConfig
import com.projectronin.interop.kafka.spring.KafkaConfig
import com.projectronin.interop.kafka.spring.KafkaPropertiesConfig
import com.projectronin.interop.kafka.spring.KafkaPublishConfig
import com.projectronin.interop.kafka.spring.KafkaRetrieveConfig
import com.projectronin.interop.kafka.spring.KafkaSaslConfig
import com.projectronin.interop.kafka.spring.KafkaSaslJaasConfig
import com.projectronin.interop.kafka.spring.KafkaSecurityConfig
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import com.projectronin.interop.validation.client.generated.models.Severity
import com.projectronin.interop.validation.server.testclients.KafkaClient
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EHRDAListenerIT : BaseValidationIT() {

    @Test
    fun `listener polls EHRDA and marks resource as processed`() {
        val config = KafkaConfig(
            cloud = KafkaCloudConfig(
                vendor = "oci",
                region = "us-phoenix-1"
            ),
            bootstrap = KafkaBootstrapConfig(servers = "localhost:9092"),
            publish = KafkaPublishConfig(source = "interop-validation"),
            retrieve = KafkaRetrieveConfig(groupId = "interop-validation-it", serviceId = "ehr-data-authority"),
            properties = KafkaPropertiesConfig(
                security = KafkaSecurityConfig(protocol = "PLAINTEXT"),
                sasl = KafkaSaslConfig(
                    mechanism = "GSSAPI",
                    jaas = KafkaSaslJaasConfig("nothing")
                )
            )
        )
        val topic = KafkaTopicConfig(config).patientTopic()
        val patient = Patient(
            id = Id("tenant-12345"),
            name = listOf(),
            identifier = listOf(
                Identifier(system = CodeSystem.RONIN_FHIR_ID.uri, value = FHIRString("12345")),
                Identifier(system = CodeSystem.RONIN_TENANT.uri, value = FHIRString("tenant"))
            )
        )

        val resourceJson = objectMapper.writeValueAsString(patient)
        val failedIssue = NewIssue(
            severity = Severity.FAILED,
            type = "PAT_001",
            description = "No names",
            location = "Patient.name"
        )
        val failedResource = NewResource(
            organizationId = "tenant",
            resourceType = "Patient",
            resource = resourceJson,
            issues = listOf(failedIssue)
        )
        val resourceUUID = addResource(failedResource)
        val ehrdaEvent = KafkaEvent(
            domain = "ehr-data-authority",
            resource = "patient",
            action = KafkaAction.UPDATE,
            resourceId = patient.id!!.value!!,
            data = objectMapper.readValue(resourceJson, topic.eventClass.java)
        )
        KafkaClient.testingClient.client.publishEvents(topic, listOf(ehrdaEvent))
        Thread.sleep(4000) // there has to be a better way
        val updatedResource = runBlocking { resourceClient.getResourceById(resourceUUID) }
        assertEquals(ResourceStatus.CORRECTED, updatedResource.status)
    }
}
