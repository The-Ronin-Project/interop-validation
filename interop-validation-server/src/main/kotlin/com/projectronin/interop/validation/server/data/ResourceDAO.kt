package com.projectronin.interop.validation.server.data

import com.projectronin.interop.validation.server.data.binding.ResourceDOs
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import mu.KotlinLogging
import org.ktorm.database.Database
import org.ktorm.dsl.and
import org.ktorm.dsl.asc
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.greater
import org.ktorm.dsl.inList
import org.ktorm.dsl.less
import org.ktorm.dsl.limit
import org.ktorm.dsl.map
import org.ktorm.dsl.or
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.schema.ColumnDeclaring
import org.springframework.stereotype.Repository
import java.util.UUID

/**
 * Data Access object for managing [ResourceDO]s.
 */
@Repository
class ResourceDAO(private val database: Database) {
    val logger = KotlinLogging.logger { }

    /**
     * Retrieves the [ResourceDO] for the [id].
     */
    fun getResource(id: UUID): ResourceDO? {
        logger.info { "Looking up resource $id" }
        val resource =
            database.from(ResourceDOs).select().where(ResourceDOs.id eq id).map { ResourceDOs.createEntity(it) }
                .singleOrNull()

        if (resource == null) {
            logger.info { "No resource found for id $id" }
            return null
        }

        logger.info { "Resource loaded for $id" }
        return resource
    }

    /**
     * Retrieves [limit] number of [ResourceDO]s with the [statuses] in the requested [order].
     * If an [after] is provided, the resources returned will all be considered logically after it based on the [order].
     */
    fun getResources(statuses: List<ResourceStatus>, order: Order, limit: Int, after: UUID?): List<ResourceDO> {
        require(statuses.isNotEmpty()) { "At least one status must be provided" }

        logger.info { "Retrieving $limit resources in $order order after $after for following statuses: $statuses" }

        val afterResource =
            after?.let { getResource(it) ?: throw IllegalArgumentException("No resource found for $after") }

        // Ordering by create and then ID ensures that the conditions below can
        val orderBy = when (order) {
            Order.ASC -> listOf(ResourceDOs.createDateTime.asc(), ResourceDOs.id.asc())
            Order.DESC -> listOf(ResourceDOs.createDateTime.desc(), ResourceDOs.id.desc())
        }

        val query = database.from(ResourceDOs).select().where {
            val conditions = mutableListOf<ColumnDeclaring<Boolean>>()

            conditions += ResourceDOs.status inList statuses

            afterResource?.let {
                // With an after resource, we care about 2 different conditions:
                // 1. The time is "after" the "after resource's time". So for ASC, it's greater, and for DESC it's less.
                // 2. If the time is the same, we need to check based off the ID, our secondary sort, to ensure that we
                //    have retrieved all the resources that occurred at the same time as the "after resource".
                conditions += when (order) {
                    Order.ASC -> (
                        (ResourceDOs.createDateTime greater it.createDateTime) or
                            ((ResourceDOs.createDateTime eq it.createDateTime) and (ResourceDOs.id greater it.id))
                        )
                    Order.DESC -> (
                        (ResourceDOs.createDateTime less it.createDateTime) or
                            ((ResourceDOs.createDateTime eq it.createDateTime) and (ResourceDOs.id less it.id))
                        )
                }
            }

            conditions.reduce { a, b -> a and b }
        }.limit(limit).orderBy(orderBy)

        val resources = query.map { ResourceDOs.createEntity(it) }
        logger.info { "Found ${resources.size} resources" }
        return resources
    }
}
