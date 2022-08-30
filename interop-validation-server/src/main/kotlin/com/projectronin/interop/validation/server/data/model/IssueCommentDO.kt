package com.projectronin.interop.validation.server.data.model

import org.ktorm.entity.Entity
import java.util.UUID

interface IssueCommentDO : Entity<IssueCommentDO> {
    companion object : Entity.Factory<IssueCommentDO>()

    var issueId: UUID
    var commentId: UUID
}
