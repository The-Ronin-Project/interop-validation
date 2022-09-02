package com.projectronin.interop.validation.server.data

import com.github.database.rider.core.api.connection.ConnectionHolder
import com.github.database.rider.core.api.dataset.DataSet
import com.projectronin.interop.common.test.database.dbrider.DBRiderConnection
import com.projectronin.interop.common.test.database.ktorm.KtormHelper
import com.projectronin.interop.common.test.database.liquibase.LiquibaseTest
import com.projectronin.interop.validation.server.data.model.CommentDO
import com.projectronin.interop.validation.server.generated.models.Order
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.TimeZone
import java.util.UUID

@LiquibaseTest(changeLog = "validation/db/changelog/validation.db.changelog-master.yaml")
class CommentDAOTest {
    init {
        // We have to set this to prevent some DBUnit weirdness around UTC and the local time zone that DST seems to cause.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @DBRiderConnection
    lateinit var connectionHolder: ConnectionHolder

    private val commentDAO = CommentDAO(KtormHelper.database())
    private val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
    private val issueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")

    @Test
    @DataSet(value = ["/dbunit/comment/NoComments.yaml"], cleanAfter = true)
    fun `getCommentsByResource - nothing from nothing`() {
        val comments = commentDAO.getCommentsByResource(resourceId, Order.ASC)
        assertTrue(comments.isEmpty())
    }

    @Test
    @DataSet(value = ["/dbunit/comment/SeveralComments.yaml"], cleanAfter = true)
    fun `getCommentsByResource - found and sorted correctly`() {
        val comments = commentDAO.getCommentsByResource(resourceId, Order.ASC)
        assertEquals(2, comments.size)
        val second = comments[1]
        assertNotNull(second)
        val uuid = UUID.fromString("ba6eca45-15cd-46a7-a5f5-5d8dc2c4d994")
        assertEquals(uuid, second.id)
    }

    @Test
    @DataSet(value = ["/dbunit/comment/NoComments.yaml"], cleanAfter = true)
    fun `getCommentsByIssue - nothing from nothing`() {
        val comments = commentDAO.getCommentsByIssue(UUID.randomUUID(), Order.ASC)
        assertTrue(comments.isEmpty())
    }

    @Test
    @DataSet(value = ["/dbunit/comment/SeveralComments.yaml"], cleanAfter = true)
    fun `getCommentByIssue - found and everything looks good`() {
        val comments = commentDAO.getCommentsByIssue(issueId, Order.DESC)
        assertEquals(2, comments.size)
        val first = comments.first()
        assertNotNull(first)
        val uuid = UUID.fromString("0e66c1a1-cd12-47f5-98bd-f4f78d1f1929")
        assertEquals(uuid, first.id)
        assertEquals("Sam", first.author)
        assertEquals("No thanks. I'm going on vacation", first.text)
        assertEquals(OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC), first.createDateTime)
    }

    @Test
    @DataSet(value = ["/dbunit/comment/NoComments.yaml"], cleanAfter = true)
    fun `insert issue comment`() {
        val commentDO = CommentDO {
            author = "Beau"
            text = "Oof this one is bad"
            createDateTime = OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC)
        }
        val commentID = commentDAO.insertIssueComment(commentDO, issueId)
        val commentsFromDB = commentDAO.getCommentsByIssue(issueId, order = Order.DESC)
        assertEquals(commentID, commentsFromDB.first().id)
        assertEquals("Beau", commentsFromDB.first().author)
    }

    @Test
    @DataSet(value = ["/dbunit/comment/NoComments.yaml"], cleanAfter = true)
    fun `insert resource comment`() {
        val commentDO = CommentDO {
            author = "Beau"
            text = "Oof this one is bad"
            createDateTime = OffsetDateTime.of(2022, 8, 2, 11, 18, 0, 0, ZoneOffset.UTC)
        }
        val resourceID = commentDAO.insertResourceComment(commentDO, resourceId)
        val resourcesFromDB = commentDAO.getCommentsByResource(resourceId, order = Order.DESC)
        assertEquals(resourceID, resourcesFromDB.first().id)
        assertEquals("Beau", resourcesFromDB.first().author)
    }
}
