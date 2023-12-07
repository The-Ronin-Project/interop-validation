package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.CommentDAO
import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.CommentDO
import com.projectronin.interop.validation.server.data.model.toCommentDO
import com.projectronin.interop.validation.server.generated.apis.CommentApi
import com.projectronin.interop.validation.server.generated.models.Comment
import com.projectronin.interop.validation.server.generated.models.GeneratedId
import com.projectronin.interop.validation.server.generated.models.NewComment
import com.projectronin.interop.validation.server.generated.models.Order
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class CommentController(
    private val commentDAO: CommentDAO,
    private val issueDAO: IssueDAO,
    private val resourceDAO: ResourceDAO,
) :
    CommentApi {
    val logger = KotlinLogging.logger { }

    @PreAuthorize("hasAuthority('SCOPE_read:comments')")
    override fun getCommentsByResource(
        resourceId: UUID,
        order: Order,
    ): ResponseEntity<List<Comment>> {
        resourceDAO.getResource(resourceId) ?: return ResponseEntity.notFound().build()
        val commentDOs = commentDAO.getCommentsByResource(resourceId, order)
        if (commentDOs.isEmpty()) {
            return ResponseEntity.ok(listOf())
        }

        val comments =
            commentDOs.map {
                createComment(it)
            }
        return ResponseEntity.ok(comments)
    }

    @PreAuthorize("hasAuthority('SCOPE_create:comments')")
    override fun addCommentForResource(
        resourceId: UUID,
        newComment: NewComment,
    ): ResponseEntity<GeneratedId> {
        resourceDAO.getResource(resourceId) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(GeneratedId(commentDAO.insertResourceComment(newComment.toCommentDO(), resourceId)))
    }

    @PreAuthorize("hasAuthority('SCOPE_read:comments')")
    override fun getCommentsByIssue(
        resourceId: UUID,
        issueId: UUID,
        order: Order,
    ): ResponseEntity<List<Comment>> {
        if (!validateIssue(resourceId, issueId)) {
            return ResponseEntity.notFound().build()
        }

        // now retrieve the actual comments
        val commentDOs = commentDAO.getCommentsByIssue(issueId, order)
        if (commentDOs.isEmpty()) {
            return ResponseEntity.ok(listOf())
        }
        val comments =
            commentDOs.map {
                createComment(it)
            }
        return ResponseEntity.ok(comments)
    }

    @PreAuthorize("hasAuthority('SCOPE_create:comments')")
    override fun addCommentForIssue(
        resourceId: UUID,
        issueId: UUID,
        newComment: NewComment,
    ): ResponseEntity<GeneratedId> {
        if (!validateIssue(resourceId, issueId)) {
            return ResponseEntity.notFound().build()
        }
        return ResponseEntity.ok(GeneratedId(commentDAO.insertIssueComment(newComment.toCommentDO(), issueId)))
    }

    private fun createComment(commentDO: CommentDO): Comment {
        return Comment(
            id = commentDO.id,
            author = commentDO.author,
            text = commentDO.text,
            createDtTm = commentDO.createDateTime,
        )
    }

    private fun validateIssue(
        resourceId: UUID,
        issueId: UUID,
    ): Boolean {
        // this is annoying check to make sure the issue and the resource match from the caller
        // when we get back the comments we can't actually check the resource of the issue they're attached to
        // so just do it here first
        val issueDO = issueDAO.getIssue(resourceId, issueId)
        return issueDO != null
    }
}
