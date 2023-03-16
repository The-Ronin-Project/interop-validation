package com.projectronin.interop.validation.server.controller

import com.projectronin.interop.validation.server.data.CommentDAO
import com.projectronin.interop.validation.server.data.IssueDAO
import com.projectronin.interop.validation.server.data.ResourceDAO
import com.projectronin.interop.validation.server.data.model.CommentDO
import com.projectronin.interop.validation.server.data.model.IssueDO
import com.projectronin.interop.validation.server.data.model.ResourceDO
import com.projectronin.interop.validation.server.data.model.toCommentDO
import com.projectronin.interop.validation.server.generated.models.IssueStatus
import com.projectronin.interop.validation.server.generated.models.NewComment
import com.projectronin.interop.validation.server.generated.models.Order
import com.projectronin.interop.validation.server.generated.models.ResourceStatus
import com.projectronin.interop.validation.server.generated.models.Severity
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CommentControllerTest {
    lateinit var issueDAO: IssueDAO
    lateinit var commentDAO: CommentDAO
    lateinit var resourceDAO: ResourceDAO
    private lateinit var controller: CommentController

    private val resourceId = UUID.fromString("5f781c30-02f3-4f06-adcf-7055bcbc5770")
    private val issueId = UUID.fromString("5f2139f1-3522-4746-8eb9-5607b9e0b663")
    private val comment1ID = UUID.fromString("981d2048-eb49-4bfd-ba96-8291288641c3")
    private val comment2ID = UUID.fromString("0e66c1a1-cd12-47f5-98bd-f4f78d1f1929")
    private val commentCreateTime = OffsetDateTime.now(ZoneOffset.UTC)
    private val resourceDO = ResourceDO {
        id = resourceId
        organizationId = "testorg"
        resourceType = "Patient"
        resource = "patient resource"
        status = ResourceStatus.REPORTED
        createDateTime = commentCreateTime
    }

    private val issueDO = IssueDO {
        id = issueId
        resourceId = resourceDO.id
        severity = Severity.FAILED
        type = "pat-1"
        location = "Patient.contact"
        description = "No contact details"
        status = IssueStatus.REPORTED
        createDateTime = commentCreateTime
    }

    private val commentDO1 = CommentDO {
        id = comment1ID
        author = "Sam"
        text = "I give up"
        createDateTime = commentCreateTime
    }
    private val commentDO2 = CommentDO {
        id = comment2ID
        author = "Sam 2"
        text = "I also give up"
        createDateTime = commentCreateTime
    }

    private val newComment1 = NewComment(
        author = "Beau",
        text = "I disagree with this error"
    )

    private val newComment2 = NewComment(
        author = "Beau",
        text = "I also disagree with this error",
        createDtTm = commentCreateTime
    )

    @BeforeEach
    fun setup() {
        issueDAO = mockk<IssueDAO> {
            every {
                getIssue(resourceId, issueId)
            } returns issueDO
        }
        commentDAO = mockk()
        resourceDAO = mockk<ResourceDAO> {
            every {
                getResource(resourceId)
            } returns resourceDO
        }
        controller = CommentController(commentDAO, issueDAO, resourceDAO)
    }

    @Test
    fun `getCommentsByResource - bad resource id returns 404`() {
        every {
            resourceDAO.getResource(resourceId)
        } returns null

        val response = controller.getCommentsByResource(resourceId, Order.ASC)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getCommentsByResource - none found returns none`() {
        every {
            commentDAO.getCommentsByResource(resourceId, Order.ASC)
        } returns emptyList()

        val response = controller.getCommentsByResource(resourceId, Order.ASC)
        assertEquals(HttpStatus.OK, response.statusCode)

        val comments = response.body!!
        assertEquals(0, comments.size)
    }

    @Test
    fun `getCommentsByResource - found resources return found resources`() {
        every {
            commentDAO.getCommentsByResource(resourceId, Order.ASC)
        } returns listOf(commentDO1)

        val response = controller.getCommentsByResource(resourceId, Order.ASC)
        assertEquals(HttpStatus.OK, response.statusCode)

        val comments = response.body!!
        assertEquals(1, comments.size)
        val comment = comments.first()
        assertNotNull(comment)
        assertEquals(comment1ID, comment.id)
        assertEquals("Sam", comment.author)
        assertEquals("I give up", comment.text)
        assertEquals(commentCreateTime, comment.createDtTm)
    }

    @Test
    fun `getCommentsByIssue - bad issue returns 404`() {
        every {
            issueDAO.getIssue(resourceId, issueId)
        } returns null
        val response = controller.getCommentsByIssue(resourceId, issueId, Order.ASC)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getCommentsByIssue - mismatch resource returns bad call`() {
        every {
            issueDAO.getIssue(resourceId, issueId)
        } returns null
        val response = controller.getCommentsByIssue(resourceId, issueId, Order.ASC)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `getCommentsByIssue - none found is ok`() {
        every {
            commentDAO.getCommentsByIssue(issueId, Order.ASC)
        } returns emptyList()
        val response = controller.getCommentsByIssue(resourceId, issueId, Order.ASC)
        assertEquals(HttpStatus.OK, response.statusCode)

        val comments = response.body!!
        assertEquals(0, comments.size)
    }

    @Test
    fun `getCommentsByIssue - some found works`() {
        every {
            commentDAO.getCommentsByIssue(issueId, Order.ASC)
        } returns listOf(commentDO1, commentDO2)
        val response = controller.getCommentsByIssue(resourceId, issueId, Order.ASC)
        assertEquals(HttpStatus.OK, response.statusCode)

        val comments = response.body!!
        assertEquals(2, comments.size)
        assertEquals(comment2ID, comments[1].id)
    }

    @Test
    fun `addCommentForResource - ok`() {
        every { commentDAO.insertResourceComment(any(), resourceId) } returns comment1ID
        val response = controller.addCommentForResource(resourceId, newComment1)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `addCommentForResource - resource not found`() {
        every { resourceDAO.getResource(resourceId) } returns null
        val response = controller.addCommentForResource(resourceId, newComment1)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    @Test
    fun `addCommentForIssue - ok`() {
        every { commentDAO.insertIssueComment(newComment2.toCommentDO(), issueId) } returns comment2ID
        every { issueDAO.getIssue(resourceId, issueId) } returns issueDO
        val response = controller.addCommentForIssue(resourceId, issueId, newComment2)
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `addCommentForIssue - mismatch`() {
        every { commentDAO.insertIssueComment(newComment2.toCommentDO(), issueId) } returns comment2ID
        every { issueDAO.getIssue(resourceId, issueId) } returns null
        val response = controller.addCommentForIssue(resourceId, issueId, newComment2)
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
