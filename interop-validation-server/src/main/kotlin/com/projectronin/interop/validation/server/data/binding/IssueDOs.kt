package com.projectronin.interop.validation.server.data.binding

import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.varchar

/**
 * Table binding definition for [IssueDO]s.
 */
object IssueDOs : Table<IssueDO>("issue") {
    val id = binaryUuid("issue_id").primaryKey().bindTo { it.id }
    val resourceId = binaryUuid("resource_id").bindTo { it.resourceId }
    val severity = enum<Severity>("severity").bindTo { it.severity }
    val type = varchar("type").bindTo { it.type }
    val location = varchar("location").bindTo { it.location }
    val description = varchar("description").bindTo { it.description }
    val status = enum<IssueStatus>("status").bindTo { it.status }
    val createDateTime = utcDateTime("create_dt_tm").bindTo { it.createDateTime }
    val updateDateTime = utcDateTime("update_dt_tm").bindTo { it.updateDateTime }
}
