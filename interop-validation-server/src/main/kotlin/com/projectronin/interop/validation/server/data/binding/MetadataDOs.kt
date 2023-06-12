package com.projectronin.interop.validation.server.data.binding

import com.projectronin.interop.common.ktorm.binding.binaryUuid
import com.projectronin.interop.validation.server.data.model.MetadataDO
import org.ktorm.schema.Table
import org.ktorm.schema.varchar

/**
 * Table binding definition for [MetadataDO]s.
 */
object MetadataDOs : Table<MetadataDO>("meta") {
    var id = binaryUuid("meta_id").primaryKey().bindTo { it.id }
    var issueId = binaryUuid("issue_id").bindTo { it.issueId }
    var registryEntryType = varchar("registry_entry_type").bindTo { it.registryEntryType }
    var valueSetName = varchar("value_set_name").bindTo { it.valueSetName }
    var valueSetUuid = binaryUuid("value_set_uuid").bindTo { it.valueSetUuid }
    var conceptMapName = varchar("concept_map_name").bindTo { it.conceptMapName }
    var conceptMapUuid = binaryUuid("concept_map_uuid").bindTo { it.conceptMapUuid }
    var version = varchar("version").bindTo { it.version }
}
