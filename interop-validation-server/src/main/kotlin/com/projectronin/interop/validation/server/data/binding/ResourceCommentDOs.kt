package com.projectronin.interop.validation.server.data.binding

import com.projectronin.interop.validation.server.data.model.ResourceCommentDO
import org.ktorm.schema.Table

object ResourceCommentDOs : Table<ResourceCommentDO>("resource_comment_r") {
    val resourceId = binaryUuid("resource_id").primaryKey().bindTo { it.resourceId }
    val commentId = binaryUuid("comment_id").bindTo { it.commentId }
}
