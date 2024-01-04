package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.auth.InteropAuthenticationService
import com.projectronin.interop.common.http.exceptions.ClientAuthenticationException
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.common.http.ktor.ContentLengthSupplier
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.validation.client.generated.models.Comment
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewComment
import com.projectronin.interop.validation.client.generated.models.Order
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.mockk.every
import io.mockk.mockk
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
    private val authenticationToken = "123456"
    private val authenticationService =
        mockk<InteropAuthenticationService> {
            every { getAuthentication() } returns
                mockk {
                    every { accessToken } returns authenticationToken
                }
        }
    private val httpClient =
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                jackson {
                    JacksonManager.setUpMapper(this)
                }
            }
            install(ContentLengthSupplier)
        }
    private val client = CommentClient("$hostUrl", httpClient, authenticationService)
    private val expectedComment1 =
        Comment(
            id = UUID.fromString("573b456efca5-03d51d53-1a31-49a9-af74"),
            author = "tester 1",
            text = "mysterious",
            createDtTm = OffsetDateTime.now(ZoneOffset.UTC),
        )
    private val expectedComment2 =
        Comment(
            id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5"),
            author = "tester 2",
            text = "obfuscated",
            createDtTm = OffsetDateTime.now(ZoneOffset.UTC),
        )

    @Test
    fun `getResourceComments - works`() {
        val commentList = listOf(expectedComment1, expectedComment2)
        val commentListJson = JacksonManager.objectMapper.writeValueAsString(commentList)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentListJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getResourceComments(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                    Order.ASC,
                )
            }
        assertEquals(commentList, response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/1d531a31-49a9-af74-03d5-573b456efca5/comments?order=ASC"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResourceComments - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.Forbidden.value)
                .setBody("")
                .setHeader("Content-Type", "application/json"),
        )

        val exception =
            assertThrows<ClientAuthenticationException> {
                runBlocking {
                    client.getResourceComments(
                        UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                        Order.ASC,
                    )
                }
            }
        val message = exception.message!!
        assertTrue(message.contains("Received 403 Client Error when calling Validation"))
        assertTrue(message.contains("for "))
        assertTrue(message.contains("/resources/1d531a31-49a9-af74-03d5-573b456efca5/comments"))

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/1d531a31-49a9-af74-03d5-573b456efca5/comments?order=ASC"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `addResourceComment - works`() {
        val newComment1 =
            NewComment(
                author = "tester 1",
                text = "mysterious",
                createDtTm = OffsetDateTime.now(ZoneOffset.UTC),
            )
        val commentJson = JacksonManager.objectMapper.writeValueAsString(expectedComment1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.addResourceComment(
                    UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                    newComment1,
                )
            }
        assertEquals(GeneratedId(expectedComment1.id), response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/1d531a31-49a9-af74-03d5-573b456efca5/comments"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `addResourceComment - exception`() {
        val newComment1 =
            NewComment(
                author = "tester 1",
                text = "mysterious",
                createDtTm = OffsetDateTime.now(ZoneOffset.UTC),
            )
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.PayloadTooLarge.value)
                .setBody("")
                .setHeader("Content-Type", "application/json"),
        )

        val exception =
            assertThrows<ClientFailureException> {
                runBlocking {
                    client.addResourceComment(
                        UUID.fromString("1d531a31-49a9-af74-03d5-573b456efca5"),
                        newComment1,
                    )
                }
            }
        val message = exception.message!!
        assertTrue(message.contains("Received 413 Client Error when calling Validation"))
        assertTrue(message.contains("for "))
        assertTrue(message.contains("/resources/1d531a31-49a9-af74-03d5-573b456efca5/comments"))

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/1d531a31-49a9-af74-03d5-573b456efca5/comments"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResourceIssueComments - works`() {
        val commentList = listOf(expectedComment1, expectedComment2)
        val commentListJson = JacksonManager.objectMapper.writeValueAsString(commentList)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentListJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.getResourceIssueComments(
                    UUID.fromString("123449a9-af74-03d5-1a31-573b456efca5"),
                    UUID.fromString("12341a31-49a9-af74-573b-456e03d51d53"),
                    Order.DESC,
                )
            }
        assertEquals(commentList, response)

        val request = mockWebServer.takeRequest()
        request.path?.endsWith(
            "/resources/123449a9-af74-03d5-1a31-573b456efca5/issues/12341a31-49a9-af74-573b-456e03d51d53/comments?order=DESC",
        )
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResourceIssueComments - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.InternalServerError.value)
                .setBody("")
                .setHeader("Content-Type", "application/json"),
        )

        val exception =
            assertThrows<ServerFailureException> {
                runBlocking {
                    client.getResourceIssueComments(
                        UUID.fromString("123449a9-af74-03d5-1a31-573b456efca5"),
                        UUID.fromString("12341a31-49a9-af74-573b-456e03d51d53"),
                        Order.DESC,
                    )
                }
            }
        val message = exception.message!!
        assertTrue(message.contains("Received 500 Server Error when calling Validation"))
        assertTrue(message.contains("for "))
        assertTrue(message.contains("/resources/123449a9-af74-03d5-1a31-573b456efca5/issues/12341a31-49a9-af74-573b-456e03d51d53/comments"))

        val request = mockWebServer.takeRequest()
        request.path?.endsWith(
            "/resources/123449a9-af74-03d5-1a31-573b456efca5/issues/12341a31-49a9-af74-573b-456e03d51d53/comments?order=DESC",
        )
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `addResourceIssueComment - works`() {
        val newComment1 =
            NewComment(
                author = "tester 1",
                text = "mysterious",
                createDtTm = OffsetDateTime.now(ZoneOffset.UTC),
            )
        val commentJson = JacksonManager.objectMapper.writeValueAsString(expectedComment1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(commentJson)
                .setHeader("Content-Type", "application/json"),
        )

        val response =
            runBlocking {
                client.addResourceIssueComment(
                    UUID.fromString("123449a9-af74-03d5-1a31-573b456efca5"),
                    UUID.fromString("12341a31-49a9-af74-573b-456e03d51d53"),
                    newComment1,
                )
            }
        assertEquals(GeneratedId(expectedComment1.id), response)

        val request = mockWebServer.takeRequest()
        request.path?.endsWith("/resources/123449a9-af74-03d5-1a31-573b456efca5/issues/12341a31-49a9-af74-573b-456e03d51d53/comments")
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `addResourceIssueComment - exception`() {
        val newComment1 =
            NewComment(
                author = "tester 1",
                text = "mysterious",
                createDtTm = OffsetDateTime.now(ZoneOffset.UTC),
            )
        val commentJson = JacksonManager.objectMapper.writeValueAsString(expectedComment1)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.PayloadTooLarge.value)
                .setBody(commentJson)
                .setHeader("Content-Type", "application/json"),
        )

        val exception =
            assertThrows<ClientFailureException> {
                runBlocking {
                    client.addResourceIssueComment(
                        UUID.fromString("123449a9-af74-03d5-1a31-573b456efca5"),
                        UUID.fromString("12341a31-49a9-af74-573b-456e03d51d53"),
                        newComment1,
                    )
                }
            }
        val message = exception.message!!
        assertTrue(message.contains("Received 413 Client Error when calling Validation"))
        assertTrue(message.contains("for "))
        assertTrue(message.contains("/resources/123449a9-af74-03d5-1a31-573b456efca5/issues/12341a31-49a9-af74-573b-456e03d51d53/comments"))

        val request = mockWebServer.takeRequest()
        request.path?.endsWith("/resources/123449a9-af74-03d5-1a31-573b456efca5/issues/12341a31-49a9-af74-573b-456e03d51d53/comments")
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }
}
