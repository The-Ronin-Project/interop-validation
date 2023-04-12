package com.projectronin.interop.validation.server.data.binding

import com.projectronin.interop.common.ktorm.binding.binaryUuid
import com.projectronin.interop.common.ktorm.binding.utcDateTime
import com.projectronin.interop.validation.server.data.model.CommentDO
import org.ktorm.schema.Table
import org.ktorm.schema.varchar

object CommentDOs : Table<CommentDO>("comment") {
    val id = binaryUuid("comment_id").primaryKey().bindTo { it.id }
    val author = varchar("author").bindTo { it.author }
    val text = varchar("text").bindTo { it.text }
    val createDateTime = utcDateTime("create_dt_tm").bindTo { it.createDateTime }
}
