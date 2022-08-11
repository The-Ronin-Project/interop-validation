package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.generated.apis.CommentApi
import com.projectronin.interop.validation.server.generated.models.Comment
import com.projectronin.interop.validation.server.generated.models.GeneratedId
import com.projectronin.interop.validation.server.generated.models.NewComment
import com.projectronin.interop.validation.server.generated.models.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CommentController : CommentApi {
    override fun getCommentsByResource(resourceId: UUID, order: Order): ResponseEntity<List<Comment>> {
        TODO()
    }

    override fun addCommentForResource(resourceId: UUID, newComment: NewComment): ResponseEntity<GeneratedId> {
        TODO()
    }

    override fun getCommentsByIssue(resourceId: UUID, issueId: UUID, order: Order): ResponseEntity<List<Comment>> {
        TODO()
    }

    override fun addCommentForIssue(
        resourceId: UUID,
        issueId: UUID,
        newComment: NewComment
    ): ResponseEntity<GeneratedId> {
        TODO()
    }
}
