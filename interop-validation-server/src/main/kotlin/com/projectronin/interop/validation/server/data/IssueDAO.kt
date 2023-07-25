package com.projectronin.interop.validation.server.data

import com.projectronin.interop.validation.server.data.binding.IssueDOs
import com.projectronin.interop.validation.server.data.binding.MetadataDOs
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.data.model.MetadataDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.Severity
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.asc
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.greater
import org.ktorm.dsl.inList
import org.ktorm.dsl.insert
import org.ktorm.dsl.less
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.or
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.ColumnDeclaring
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset
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

    fun insertMetadata(metadataDO: MetadataDO): UUID {
        val newUUID = UUID.randomUUID()
        logger.info { "Inserting metadata related to issue ${metadataDO.issueId} with UUID $newUUID" }

        database.insert(MetadataDOs) {
            set(it.id, newUUID)
            set(it.issueId, metadataDO.issueId)
            set(it.registryEntryType, metadataDO.registryEntryType)
            set(it.valueSetName, metadataDO.valueSetName)
            set(it.valueSetUuid, metadataDO.valueSetUuid)
            set(it.conceptMapName, metadataDO.conceptMapName)
            set(it.conceptMapUuid, metadataDO.conceptMapUuid)
            set(it.version, metadataDO.version)
        }
        logger.info { "Meta $newUUID inserted" }
        return newUUID
    }

    /**
     * Retrieves the [IssueDO] for the [resourceId] and [issueId].
     */
    fun getIssue(resourceId: UUID, issueId: UUID): IssueDO? {
        logger.info { "Looking up issue $issueId within resource $resourceId" }
        val issue =
            database.from(IssueDOs).select().where((IssueDOs.id eq issueId) and (IssueDOs.resourceId eq resourceId))
                .map { IssueDOs.createEntity(it) }.singleOrNull()
        if (issue == null) {
            logger.info { "No issue found for resource $resourceId and issue $issueId" }
            return null
        } else {
            issue.metadata = database.from(MetadataDOs).select().where(MetadataDOs.issueId eq issueId).map { MetadataDOs.createEntity(it) }
        }

        logger.info { "Issue loaded for resource $resourceId and issue $issueId" }
        return issue
    }

    /**
     * Retrieves a [limit]ed number of issues for a specific [resourceUUID] and matching [statuses]
     * sorted chronologically by [order], optionally starting at after [after]
     */
    fun getIssues(
        resourceUUID: UUID,
        statuses: List<IssueStatus>,
        order: Order,
        limit: Int,
        after: UUID?
    ): List<IssueDO> {
        require(statuses.isNotEmpty()) { "At least one status must be provided" }
        logger.info { "Retrieving $limit issues in $order order after $after for following statuses: $statuses" }

        val afterResource =
            after?.let { getIssue(resourceUUID, it) ?: throw IllegalArgumentException("No issue found for $after") }

        // Ordering by create and then ID ensures that issues with the same create time will always be in the same order
        val orderBy = when (order) {
            Order.ASC -> listOf(IssueDOs.createDateTime.asc(), IssueDOs.id.asc())
            Order.DESC -> listOf(IssueDOs.createDateTime.desc(), IssueDOs.id.desc())
        }

        val issues = database.from(IssueDOs)
            .select()
            .where {
                val conditions = mutableListOf<ColumnDeclaring<Boolean>>()

                conditions += IssueDOs.status inList statuses
                conditions += IssueDOs.resourceId eq resourceUUID

                afterResource?.let {
                    // With an after resource, we care about 2 different conditions:
                    // 1. The time is "after" the "after issue's time". So for ASC, it's greater, and for DESC it's less.
                    // 2. If the time is the same, we need to check based off the ID, our secondary sort, to ensure that we
                    //    have retrieved all the issues that occurred at the same time as the "after issue".
                    conditions += when (order) {
                        Order.ASC -> (
                            (IssueDOs.createDateTime greater it.createDateTime) or
                                ((IssueDOs.createDateTime eq it.createDateTime) and (IssueDOs.id greater it.id))
                            )

                        Order.DESC -> (
                            (IssueDOs.createDateTime less it.createDateTime) or
                                ((IssueDOs.createDateTime eq it.createDateTime) and (IssueDOs.id less it.id))
                            )
                    }
                }

                conditions.reduce { a, b -> a and b }
            }.limit(limit).orderBy(orderBy).map { IssueDOs.createEntity(it) }

        issues.forEach { issue ->
            issue.metadata = database.from(MetadataDOs).select().where { MetadataDOs.issueId eq issue.id }.map { MetadataDOs.createEntity(it) }
        }

        logger.info { "Found ${issues.size} issues" }
        return issues
    }

    /**
     * Updates the issue with [resourceId] and [issueId] based off the provided [updateFunction]. [updateFunction] should
     * use the provided issue's setter functions to provide an updated view of the issue.
     */
    fun updateIssue(resourceId: UUID, issueId: UUID, updateFunction: (IssueDO) -> Unit): IssueDO? {
        logger.info { "Updating issue $issueId" }

        val issue =
            database.from(IssueDOs)
                .select()
                .where((IssueDOs.resourceId eq resourceId) and (IssueDOs.id eq issueId))
                .map { IssueDOs.createEntity(it) }
                .singleOrNull()

        if (issue == null) {
            logger.info { "No issue found for resource $resourceId and issue $issueId" }
            return null
        }

        updateFunction(issue)

        if (issue.updateDateTime == null) {
            issue.updateDateTime = OffsetDateTime.now(ZoneOffset.UTC)
        }

        issue.flushChanges()

        logger.info { "Issue $issueId updated" }
        return issue
    }
}
