package com.projectronin.interop.validation.server.data.binding

import com.projectronin.interop.common.ktorm.binding.binaryUuid
import com.projectronin.interop.validation.server.data.model.IssueCommentDO
import org.ktorm.schema.Table

object IssueCommentDOs : Table<IssueCommentDO>("issue_comment_r") {
    val issueId = binaryUuid("issue_id").primaryKey().bindTo { it.issueId }
    val commentId = binaryUuid("comment_id").bindTo { it.commentId }
}
