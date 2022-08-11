package com.projectronin.interop.validation.server.data

import com.projectronin.interop.validation.server.data.binding.IssueDOs
import com.projectronin.interop.validation.server.generated.models.Severity
import org.ktorm.database.Database
import org.ktorm.dsl.from
import org.ktorm.dsl.inList
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
    /**
     * Retrieves the Set of issue [Severity] associated to each resource ID.
     */
    fun getIssueSeveritiesForResources(resourceIds: List<UUID>): Map<UUID, Set<Severity>> {
        return database.from(IssueDOs).select(IssueDOs.resourceId, IssueDOs.severity)
            .where(IssueDOs.resourceId inList resourceIds)
            .map { row -> row[IssueDOs.resourceId]!! to row[IssueDOs.severity]!! }
            .groupBy { it.first }.mapValues { it.value.map { pair -> pair.second }.toSet() }
    }
}
