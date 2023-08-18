package com.projectronin.interop.validation.server.data.binding

import com.projectronin.interop.common.ktorm.binding.binaryUuid
import com.projectronin.interop.common.ktorm.binding.utcDateTime
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import org.ktorm.schema.Table
import org.ktorm.schema.enum
import org.ktorm.schema.text
import org.ktorm.schema.varchar

/**
 * Table binding definition for [ResourceDO]s.
 */
object ResourceDOs : Table<ResourceDO>("resource") {
    val id = binaryUuid("resource_id").primaryKey().bindTo { it.id }
    val organizationId = varchar("organization_id").bindTo { it.organizationId }
    val resourceType = varchar("resource_type").bindTo { it.resourceType }
    val resource = text("resource").bindTo { it.resource }
    val status = enum<ResourceStatus>("status").bindTo { it.status }
    val createDateTime = utcDateTime("create_dt_tm").bindTo { it.createDateTime }
    val updateDateTime = utcDateTime("update_dt_tm").bindTo { it.updateDateTime }
    val reprocessDateTime = utcDateTime("reprocess_dt_tm").bindTo { it.reprocessDateTime }
    val reprocessedBy = varchar("reprocessed_by").bindTo { it.reprocessedBy }
    val clientFhirId = varchar("client_fhir_id").bindTo { it.clientFhirId }
}
