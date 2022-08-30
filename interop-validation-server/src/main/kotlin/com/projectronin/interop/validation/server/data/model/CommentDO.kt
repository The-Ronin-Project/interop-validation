package com.projectronin.interop.validation.server.data.model

import org.ktorm.entity.Entity
import java.time.OffsetDateTime
import java.util.UUID

interface CommentDO : Entity<CommentDO> {
    companion object : Entity.Factory<CommentDO>()

    var id: UUID
    var author: String
    var text: String
    var createDateTime: OffsetDateTime
}
