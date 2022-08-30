package com.projectronin.interop.validation.server.data.model

import org.ktorm.entity.Entity
import java.util.UUID

interface ResourceCommentDO : Entity<ResourceCommentDO> {
    companion object : Entity.Factory<ResourceCommentDO>()

    var resourceId: UUID
    var commentId: UUID
}
