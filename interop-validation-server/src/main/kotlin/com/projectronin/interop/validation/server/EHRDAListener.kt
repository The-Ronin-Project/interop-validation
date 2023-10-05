package com.projectronin.interop.validation.server

import com.projectronin.ehr.dataauthority.models.kafka.EhrDAKafkaTopic
import com.projectronin.interop.fhir.r4.resource.Resource
import com.projectronin.interop.kafka.client.KafkaClient
import com.projectronin.interop.kafka.model.KafkaAction
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import mu.KotlinLogging
import org.apache.kafka.common.errors.InterruptException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class EHRDAListener(
    private val kafkaClient: KafkaClient,
    private val topics: List<EhrDAKafkaTopic>,
    private val resourceDAO: ResourceDAO
) {
    val logger = KotlinLogging.logger { }
    val typeMap = topics.flatMap { topic ->
        listOf(
            "ronin.ehr-data-authority.${getResourceName(topic.topicName)}.${KafkaAction.CREATE.type}" to topic.resourceClass,
            "ronin.ehr-data-authority.${getResourceName(topic.topicName)}.${KafkaAction.UPDATE.type}" to topic.resourceClass
        )
    }.toMap()

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MILLISECONDS) // Basically run constantly.
    fun poll() {
        // Catch logic can be removed if https://github.com/spring-projects/spring-framework/issues/26482 is fixed
        runCatching {
            kafkaClient.retrieveMultiTopicEvents(
                topics,
                typeMap,
                "interop-validation-server_group",
                Duration.ofMillis(5)
            ).map { it.data as Resource<*> }.forEach { resource ->
                val fhirID = resource.findFhirId()!!
                val tenantID = resource.findTenantId()!!
                resourceDAO.getResourcesByFHIRID(
                    listOf(ResourceStatus.REPORTED, ResourceStatus.ADDRESSING),
                    fhirID,
                    tenantID,
                    resource.resourceType
                ).forEach { validationResource ->
                    resourceDAO.updateResource(validationResource.id) {
                        it.status = ResourceStatus.CORRECTED
                    }
                }
            }
        }.exceptionOrNull()?.let {
            when (it) {
                is InterruptException -> null
                else -> throw it
            }
        }
    }

    /**
     * Returns the Kafka resource type name for this resource.
     */
    private fun getResourceName(topicName: String): String = topicName.split(".").reversed()[1]
}
