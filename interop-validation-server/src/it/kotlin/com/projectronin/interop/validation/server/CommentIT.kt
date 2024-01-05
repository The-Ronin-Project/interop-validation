package com.projectronin.interop.validation.server

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.fhir.r4.datatype.primitive.Id
import com.projectronin.interop.fhir.r4.resource.Patient
import com.projectronin.interop.validation.client.generated.models.NewComment
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.Severity
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CommentIT : BaseValidationIT() {
    private val patient =
        Patient(
            id = Id("12345"),
            name = listOf(),
        )

    private val failedIssue =
        NewIssue(
            severity = Severity.FAILED,
            type = "PAT_001",
            description = "No names",
            location = "Patient.name",
        )

    private val newFailedResource =
        NewResource(
            organizationId = "ronin",
            resourceType = "Patient",
            resource = JacksonManager.objectMapper.writeValueAsString(patient),
            issues = listOf(failedIssue),
        )

    private val newComment1 =
        NewComment(
            author = "Jeff",
            text = "Comment1",
            createDtTm = OffsetDateTime.of(2020, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC),
        )

    private val newComment2 =
        NewComment(
            author = "Jane",
            text = "Comment2",
            createDtTm = OffsetDateTime.of(2021, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC),
        )

    private val newComment3 =
        NewComment(
            author = "Joe",
            text = "Comment3",
            createDtTm = OffsetDateTime.of(2022, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC),
        )

    @Test
    fun `addResourceComment - works without datetime`() {
        val newResourceId = addResource(newFailedResource)
        val newComment =
            NewComment(
                author = "Jeff",
                text = "Comment",
            )
        runBlocking { commentClient.addResourceComment(newResourceId, newComment) }
        val comments = runBlocking { commentClient.getResourceComments(newResourceId, Order.ASC) }

        assertEquals(1, comments.size)
        val comment = comments.single()

        assertEquals("Jeff", comment.author)
        assertEquals("Comment", comment.text)
        assertNotNull(comment.id)
        assertNotNull(comment.createDtTm)
    }

    @Test
    fun `addResourceComment - works with datetime`() {
        val newResourceId = addResource(newFailedResource)
        val newComment =
            NewComment(
                author = "Jeff",
                text = "Comment",
                createDtTm = OffsetDateTime.of(2022, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC),
            )
        runBlocking { commentClient.addResourceComment(newResourceId, newComment) }
        val comments = runBlocking { commentClient.getResourceComments(newResourceId, Order.ASC) }

        assertEquals(1, comments.size)
        val comment = comments.single()

        assertEquals("Jeff", comment.author)
        assertEquals("Comment", comment.text)
        assertEquals(newComment.createDtTm, comment.createDtTm)
        assertNotNull(comment.id)
    }

    @Test
    fun `addResourceComment - handles fake resource ID`() {
        val exception =
            assertThrows<ClientFailureException> { runBlocking { commentClient.addResourceComment(UUID.randomUUID(), newComment1) } }
        assertTrue(exception.message?.contains("404") ?: false)
    }

    @Test
    fun `getResourceComments -  properly finds none`() {
        val newResourceId = addResource(newFailedResource)
        val comments = runBlocking { commentClient.getResourceComments(newResourceId, Order.ASC) }

        assertTrue(comments.isEmpty())
    }

    @Test
    fun `getResourceComments - properly finds some`() {
        val newResourceId = addResource(newFailedResource)
        runBlocking { commentClient.addResourceComment(newResourceId, newComment1) }
        runBlocking { commentClient.addResourceComment(newResourceId, newComment2) }
        runBlocking { commentClient.addResourceComment(newResourceId, newComment3) }
        val comments = runBlocking { commentClient.getResourceComments(newResourceId, Order.ASC) }

        assertEquals(3, comments.size)
        assertEquals("Jeff", comments.first().author)
    }

    @Test
    fun `getResourceComments - properly respects order`() {
        val newResourceId = addResource(newFailedResource)
        runBlocking { commentClient.addResourceComment(newResourceId, newComment1) }
        runBlocking { commentClient.addResourceComment(newResourceId, newComment2) }
        runBlocking { commentClient.addResourceComment(newResourceId, newComment3) }
        val comments = runBlocking { commentClient.getResourceComments(newResourceId, Order.DESC) }

        assertEquals(3, comments.size)
        assertEquals("Joe", comments.first().author)
    }

    @Test
    fun `getResourceComments - handles fake resource ID`() {
        val exception =
            assertThrows<ClientFailureException> { runBlocking { commentClient.getResourceComments(UUID.randomUUID(), Order.DESC) } }
        assertTrue(exception.message?.contains("404") ?: false)
    }

    @Test
    fun `addResourceIssueComment - works without datetime`() {
        val newResourceId = addResource(newFailedResource)
        val issueId = runBlocking { issueClient.getResourceIssues(newResourceId, Order.ASC) }.single().id
        val newComment =
            NewComment(
                author = "Jeff",
                text = "Comment",
            )
        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment) }
        val comments = runBlocking { commentClient.getResourceIssueComments(newResourceId, issueId, Order.ASC) }

        assertEquals(1, comments.size)
        val comment = comments.single()

        assertEquals("Jeff", comment.author)
        assertEquals("Comment", comment.text)
        assertNotNull(comment.id)
        assertNotNull(comment.createDtTm)
    }

    @Test
    fun `addResourceIssueComment - works with datetime`() {
        val newResourceId = addResource(newFailedResource)
        val issueId = runBlocking { issueClient.getResourceIssues(newResourceId, Order.ASC) }.single().id
        val newComment =
            NewComment(
                author = "Jeff",
                text = "Comment",
                createDtTm = OffsetDateTime.of(2022, 1, 1, 9, 0, 0, 0, ZoneOffset.UTC),
            )
        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment) }
        val comments = runBlocking { commentClient.getResourceIssueComments(newResourceId, issueId, Order.ASC) }

        assertEquals(1, comments.size)
        val comment = comments.single()

        assertEquals("Jeff", comment.author)
        assertEquals("Comment", comment.text)
        assertEquals(newComment.createDtTm, comment.createDtTm)
        assertNotNull(comment.id)
    }

    @Test
    fun `addResourceIssueComment - handles fake resource ID and issue id`() {
        val exception =
            assertThrows<ClientFailureException> {
                runBlocking {
                    commentClient.addResourceIssueComment(UUID.randomUUID(), UUID.randomUUID(), newComment1)
                }
            }
        assertTrue(exception.message?.contains("404") ?: false)
    }

    @Test
    fun `getResourceIssueComments -  properly finds none`() {
        val newResourceId = addResource(newFailedResource)
        val issueId = runBlocking { issueClient.getResourceIssues(newResourceId, Order.ASC) }.single().id

        val comments = runBlocking { commentClient.getResourceIssueComments(newResourceId, issueId, Order.ASC) }

        assertTrue(comments.isEmpty())
    }

    @Test
    fun `getResourceIssueComments - properly finds some`() {
        val newResourceId = addResource(newFailedResource)
        val issueId = runBlocking { issueClient.getResourceIssues(newResourceId, Order.ASC) }.single().id

        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment1) }
        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment2) }
        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment3) }

        val comments = runBlocking { commentClient.getResourceIssueComments(newResourceId, issueId, Order.ASC) }

        assertEquals(3, comments.size)
        assertEquals("Jeff", comments.first().author)
    }

    @Test
    fun `getResourceIssueComments - properly respects order`() {
        val newResourceId = addResource(newFailedResource)
        val issueId = runBlocking { issueClient.getResourceIssues(newResourceId, Order.ASC) }.single().id

        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment1) }
        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment2) }
        runBlocking { commentClient.addResourceIssueComment(newResourceId, issueId, newComment3) }
        val comments = runBlocking { commentClient.getResourceIssueComments(newResourceId, issueId, Order.DESC) }

        assertEquals(3, comments.size)
        assertEquals("Joe", comments.first().author)
    }

    @Test
    fun `getResourceIssueComments - handles fake resource ID`() {
        val exception =
            assertThrows<ClientFailureException> {
                runBlocking { commentClient.getResourceIssueComments(UUID.randomUUID(), UUID.randomUUID(), Order.DESC) }
            }
        assertTrue(exception.message?.contains("404") ?: false)
    }
}
