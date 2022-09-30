package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.exceptions.ClientAuthenticationException
import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.validation.client.generated.models.Issue
import com.projectronin.interop.validation.client.generated.models.IssueStatus
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.Severity
import com.projectronin.interop.validation.client.generated.models.UpdateIssue
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

class IssueClientTest {
    private val mockWebServer = MockWebServer()
    private val hostUrl = mockWebServer.url("/test")
    private val client = IssueClient("$hostUrl", HttpSpringConfig().getHttpClient())
    private val created = OffsetDateTime.now(ZoneOffset.UTC)
    private val updated = OffsetDateTime.now(ZoneOffset.UTC)
    private val expectedIssue1 = Issue(
        id = UUID.fromString("573b456efca5-03d51d53-1a31-49a9-af74"),
        severity = Severity.WARNING,
        type = "Validation",
        description = "murky",
        status = IssueStatus.REPORTED,
        createDtTm = created,
        location = "everywhere",
        updateDtTm = updated,
    )
    private val expectedIssue2 = Issue(
        id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5"),
        severity = Severity.FAILED,
        type = "Validation",
        description = "murky",
        status = IssueStatus.IGNORED,
        createDtTm = created,
        location = "anywhere",
        updateDtTm = updated,
    )
    private val updateIssue = UpdateIssue(
        severity = Severity.FAILED,
        type = "Validation",
        description = "murky",
        status = IssueStatus.IGNORED,
        createDtTm = created,
        location = "anywhere",
        updateDtTm = updated,
    )

    @Test
    fun `getResourceIssues - works`() {
        val issueList = listOf(expectedIssue1, expectedIssue2)
        val issueListJson = JacksonManager.objectMapper.writeValueAsString(issueList)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(issueListJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.getResourceIssues(
                UUID.fromString("1a31-49a9-af74-03d51d53-573b456efca5"),
                Order.ASC
            )
        }
        assertEquals(issueList, response)
    }

    @Test
    fun `getResourceIssues - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.Forbidden.value)
                .setBody("")
                .setHeader("Content-Type", "application/json")
        )

        val exception = assertThrows<ClientAuthenticationException> {
            runBlocking {
                client.getResourceIssues(
                    UUID.fromString("1a31-49a9-af74-03d51d53-573b456efca5"),
                    Order.ASC
                )
            }
        }
        val message = exception.message!!
        assertTrue(message.contains("Received 403 Client Error when calling Validation"))
        assertTrue(message.contains("for GET"))
        assertTrue(message.contains("/resources/00001a31-49a9-af74-1d53-573b456efca5/issues"))
    }

    @Test
    fun `updateResourceIssue - works`() {
        val issueJson = JacksonManager.objectMapper.writeValueAsString(expectedIssue2)

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(issueJson)
                .setHeader("Content-Type", "application/json")
        )

        val response = runBlocking {
            client.updateResourceIssue(
                UUID.fromString("49a9-af74-03d51d53-1a31-573b456efca5"),
                UUID.fromString("1a31-49a9-af74-573b456efca5-03d51d53"),
                updateIssue
            )
        }
        assertEquals(expectedIssue2, response)
    }

    @Test
    fun `updateResourceIssue - exception`() {
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.PayloadTooLarge.value)
                .setBody("")
                .setHeader("Content-Type", "application/json")
        )

        val exception = assertThrows<ClientFailureException> {
            runBlocking {
                client.updateResourceIssue(
                    UUID.fromString("49a9-af74-03d51d53-1a31-573b456efca5"),
                    UUID.fromString("1a31-49a9-af74-573b456efca5-03d51d53"),
                    updateIssue
                )
            }
        }
        val message = exception.message!!
        assertTrue(message.contains("Received 413 Client Error when calling Validation"))
        assertTrue(message.contains("for PATCH"))
        assertTrue(message.contains("/resources/000049a9-af74-1d53-1a31-573b456efca5/issues/00001a31-49a9-af74-fca5-000003d51d53"))
    }
}
