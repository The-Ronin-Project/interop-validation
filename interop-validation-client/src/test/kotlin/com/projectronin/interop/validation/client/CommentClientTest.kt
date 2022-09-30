package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.exceptions.ClientAuthenticationException
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.validation.client.generated.models.Comment
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.Order
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class CommentClientTest {
    private val mockWebServer = MockWebServer()
    private val hostUrl = mockWebServer.url("/test")
    private val client = CommentClient("$hostUrl", HttpSpringConfig().getHttpClient())
    private val expectedComment1 = Comment(
        id = UUID.fromString("573b456efca5-03d51d53-1a31-49a9-af74"),
        author = "tester 1",
        text = "mysterious",
        createDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    )
    private val expectedComment2 = Comment(
        id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5"),
        author = "tester 2",
        text = "obfuscated",
        createDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    )

    @Test
    fun `getResourceComments - works`() {
        val commentList = listOf(expectedComment1, expectedComment2)
        val commentListJson = JacksonManager.objectMapper.writeValueAsString(commentList)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentListJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.getResourceComments(
                UUID.fromString("1a31-49a9-af74-03d51d53-573b456efca5"),
                Order.ASC
            )
        }
        assertEquals(commentList, response)
    }

    @Test
    fun `getResourceComments - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.Forbidden.value)
                .setBody("")
                .setHeader("Content-Type", "application/json")
        )

        val exception = assertThrows<ClientAuthenticationException> {
            runBlocking {
                client.getResourceComments(
                    UUID.fromString("1a31-49a9-af74-03d51d53-573b456efca5"),
                    Order.ASC
                )
            }
        }
        val message = exception.message!!
        assertTrue(message.contains("Received 403 Client Error when calling Validation"))
        assertTrue(message.contains("for GET"))
        assertTrue(message.contains("/resources/00001a31-49a9-af74-1d53-573b456efca5/comments"))
    }

    @Test
    fun `addResourceComment - works`() {
        val commentJson = JacksonManager.objectMapper.writeValueAsString(expectedComment1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.addResourceComment(
                UUID.fromString("1a31-49a9-af74-03d51d53-573b456efca5"),
                expectedComment1
            )
        }
        assertEquals(GeneratedId(expectedComment1.id), response)
    }

    @Test
    fun `addResourceComment - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.PayloadTooLarge.value)
                .setBody("")
                .setHeader("Content-Type", "application/json")
        )

        val exception = assertThrows<ClientFailureException> {
            runBlocking {
                client.addResourceComment(
                    UUID.fromString("1a31-49a9-af74-03d51d53-573b456efca5"),
                    expectedComment1
                )
            }
        }
        val message = exception.message!!
        assertTrue(message.contains("Received 413 Client Error when calling Validation"))
        assertTrue(message.contains("for POST"))
        assertTrue(message.contains("/resources/00001a31-49a9-af74-1d53-573b456efca5/comments"))
    }

    @Test
    fun `getResourceIssueComments - works`() {
        val commentList = listOf(expectedComment1, expectedComment2)
        val commentListJson = JacksonManager.objectMapper.writeValueAsString(commentList)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentListJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.getResourceIssueComments(
                UUID.fromString("49a9-af74-03d51d53-1a31-573b456efca5"),
                UUID.fromString("1a31-49a9-af74-573b456efca5-03d51d53"),
                Order.DESC
            )
        }
        assertEquals(commentList, response)
    }

    @Test
    fun `getResourceIssueComments - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.InternalServerError.value)
                .setBody("")
                .setHeader("Content-Type", "application/json")
        )

        val exception = assertThrows<ServerFailureException> {
            runBlocking {
                client.getResourceIssueComments(
                    UUID.fromString("49a9-af74-03d51d53-1a31-573b456efca5"),
                    UUID.fromString("1a31-49a9-af74-573b456efca5-03d51d53"),
                    Order.DESC
                )
            }
        }
        val message = exception.message!!
        assertTrue(message.contains("Received 500 Server Error when calling Validation"))
        assertTrue(message.contains("for GET"))
        assertTrue(message.contains("/resources/000049a9-af74-1d53-1a31-573b456efca5/issues/00001a31-49a9-af74-fca5-000003d51d53/comments"))
    }

    @Test
    fun `addResourceIssueComment - works`() {
        val commentJson = JacksonManager.objectMapper.writeValueAsString(expectedComment1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.addResourceIssueComment(
                UUID.fromString("49a9-af74-03d51d53-1a31-573b456efca5"),
                UUID.fromString("1a31-49a9-af74-573b456efca5-03d51d53"),
                expectedComment1
            )
        }
        assertEquals(GeneratedId(expectedComment1.id), response)
    }

    @Test
    fun `addResourceIssueComment - exception`() {
        val commentJson = JacksonManager.objectMapper.writeValueAsString(expectedComment1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.PayloadTooLarge.value)
                .setBody(commentJson)
                .setHeader("Content-Type", "application/json")
        )

        val exception = assertThrows<ClientFailureException> {
            runBlocking {
                client.addResourceIssueComment(
                    UUID.fromString("49a9-af74-03d51d53-1a31-573b456efca5"),
                    UUID.fromString("1a31-49a9-af74-573b456efca5-03d51d53"),
                    expectedComment1
                )
            }
        }
        val message = exception.message!!
        assertTrue(message.contains("Received 413 Client Error when calling Validation"))
        assertTrue(message.contains("for POST"))
        assertTrue(message.contains("/resources/000049a9-af74-1d53-1a31-573b456efca5/issues/00001a31-49a9-af74-fca5-000003d51d53/comments"))
    }
}
