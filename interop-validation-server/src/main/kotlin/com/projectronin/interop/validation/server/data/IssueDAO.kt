package com.projectronin.interop.validation.server.data

import com.projectronin.interop.validation.server.data.binding.IssueDOs
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.generated.models.Severity
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.inList
import org.ktorm.dsl.insert
import org.ktorm.dsl.map
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Data Access object for managing [IssueDO]s.
 */
@Repository
class IssueDAO(private val database: Database) {
    val logger = KotlinLogging.logger { }

    /**
     * Retrieves the Set of issue [Severity] associated to each resource ID.
     */
    fun getIssueSeveritiesForResources(resourceIds: List<UUID>): Map<UUID, Set<Severity>> {
        return database.from(IssueDOs).select(IssueDOs.resourceId, IssueDOs.severity)
            .where(IssueDOs.resourceId inList resourceIds)
            .map { row -> row[IssueDOs.resourceId]!! to row[IssueDOs.severity]!! }
            .groupBy { it.first }.mapValues { it.value.map { pair -> pair.second }.toSet() }
    }

    /**
     * Inserts [issueDO] and returns its [UUID]
     */
    fun insertIssue(issueDO: IssueDO): UUID {
        val newUUID = UUID.randomUUID()
        logger.info { "Inserting issue for resource ${issueDO.resourceId} with UUID $newUUID" }

        database.insert(IssueDOs) {
            set(it.id, newUUID)
            set(it.resourceId, issueDO.resourceId)
            set(it.severity, issueDO.severity)
            set(it.type, issueDO.type)
            set(it.location, issueDO.location)
            set(it.description, issueDO.description)
            set(it.status, issueDO.status)
            set(it.createDateTime, issueDO.createDateTime)
            set(it.updateDateTime, issueDO.updateDateTime)
        }

        logger.info { "Issue $newUUID inserted" }
        return newUUID
    }
}
