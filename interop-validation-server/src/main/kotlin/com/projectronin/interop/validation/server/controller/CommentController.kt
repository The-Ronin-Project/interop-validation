package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.apis.CommentApi
import com.projectronin.interop.validation.server.models.Comment
import com.projectronin.interop.validation.server.models.GeneratedId
import com.projectronin.interop.validation.server.models.NewComment
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CommentController : CommentApi {
    override fun getCommentsByResource(resourceId: UUID, order: String): ResponseEntity<List<Comment>> {
        TODO()
    }

    override fun addCommentForResource(resourceId: UUID, newComment: NewComment): ResponseEntity<GeneratedId> {
        TODO()
    }

    override fun getCommentsByIssue(resourceId: UUID, issueId: UUID, order: String): ResponseEntity<List<Comment>> {
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
