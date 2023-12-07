package com.projectronin.interop.validation.server.data

import com.projectronin.interop.validation.server.data.binding.CommentDOs
import com.projectronin.interop.validation.server.data.binding.IssueCommentDOs
import com.projectronin.interop.validation.server.data.binding.ResourceCommentDOs
import com.projectronin.interop.validation.server.data.model.CommentDO
import com.projectronin.interop.validation.server.generated.models.Order
import org.ktorm.database.Database
import org.ktorm.dsl.asc
import org.ktorm.dsl.desc
import org.ktorm.dsl.eq
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.leftJoin
import org.ktorm.dsl.map
import org.ktorm.dsl.orderBy
import org.ktorm.dsl.select
import org.ktorm.dsl.where
import org.ktorm.expression.OrderByExpression
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class CommentDAO(private val database: Database) {
    /**
     *  retrieves all [CommentDO]s sorted by [order] for a given [resourceID]
     */
    fun getCommentsByResource(
        resourceID: UUID,
        order: Order,
    ): List<CommentDO> {
        return database.from(ResourceCommentDOs)
            .leftJoin(CommentDOs, ResourceCommentDOs.commentId eq CommentDOs.id)
            .select()
            .where(ResourceCommentDOs.resourceId eq resourceID)
            .orderBy(getOrderBy(order))
            .map { CommentDOs.createEntity(it) }
    }

    /**
     *  retrieves all [CommentDO]s sorted by [order] for a given [issueId]
     */
    fun getCommentsByIssue(
        issueId: UUID,
        order: Order,
    ): List<CommentDO> {
        return database.from(IssueCommentDOs)
            .leftJoin(CommentDOs, IssueCommentDOs.commentId eq CommentDOs.id)
            .select()
            .where(IssueCommentDOs.issueId eq issueId)
            .orderBy(getOrderBy(order))
            .map { CommentDOs.createEntity(it) }
    }

    // helper function to just sort the list
    private fun getOrderBy(order: Order): List<OrderByExpression> {
        return when (order) {
            Order.ASC -> listOf(CommentDOs.createDateTime.asc(), CommentDOs.id.asc())
            Order.DESC -> listOf(CommentDOs.createDateTime.desc(), CommentDOs.id.desc())
        }
    }

    @Transactional
    fun insertResourceComment(
        commentDO: CommentDO,
        resourceID: UUID,
    ): UUID {
        val commentUUID = insertComment(commentDO)
        database.insert(ResourceCommentDOs) {
            set(it.commentId, commentUUID)
            set(it.resourceId, resourceID)
        }
        return commentUUID
    }

    @Transactional
    fun insertIssueComment(
        commentDO: CommentDO,
        issueID: UUID,
    ): UUID {
        val commentUUID = insertComment(commentDO)
        database.insert(IssueCommentDOs) {
            set(it.commentId, commentUUID)
            set(it.issueId, issueID)
        }
        return commentUUID
    }

    private fun insertComment(commentDO: CommentDO): UUID {
        val newUUID = UUID.randomUUID()
        database.insert(CommentDOs) {
            set(it.id, newUUID)
            set(it.author, commentDO.author)
            set(it.text, commentDO.text)
            set(it.createDateTime, commentDO.createDateTime)
        }
        return newUUID
    }
}
