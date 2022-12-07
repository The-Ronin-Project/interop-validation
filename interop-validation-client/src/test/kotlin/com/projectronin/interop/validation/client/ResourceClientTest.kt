package com.projectronin.interop.validation.client

import com.projectronin.interop.common.http.exceptions.ClientFailureException
import com.projectronin.interop.common.http.exceptions.ServerFailureException
import com.projectronin.interop.common.http.exceptions.ServiceUnavailableException
import com.projectronin.interop.common.http.ktor.ContentLengthSupplier
import com.projectronin.interop.common.jackson.JacksonManager
import com.projectronin.interop.validation.client.auth.ValidationAuthenticationService
import com.projectronin.interop.validation.client.generated.models.GeneratedId
import com.projectronin.interop.validation.client.generated.models.NewIssue
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.client.generated.models.Order
import com.projectronin.interop.validation.client.generated.models.ReprocessResourceRequest
import com.projectronin.interop.validation.client.generated.models.Resource
import com.projectronin.interop.validation.client.generated.models.ResourceStatus
import com.projectronin.interop.validation.client.generated.models.Severity
import com.projectronin.interop.validation.client.generated.models.UpdatableResourceStatus
import com.projectronin.interop.validation.client.generated.models.UpdateResource
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
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class ResourceClientTest {
    private val authenticationToken = "123456"
    private val authenticationService = mockk<ValidationAuthenticationService> {
        every { getAuthentication() } returns mockk {
            every { accessToken } returns authenticationToken
        }
    }
    private val client: HttpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            jackson {
                JacksonManager.setUpMapper(this)
            }
        }
        install(ContentLengthSupplier)
    }
    private val resource1Id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5")
    private val resource1CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val expectedResource = Resource(
        id = resource1Id,
        organizationId = "testorg",
        resourceType = "Patient",
        resource = "patient resource",
        status = ResourceStatus.REPORTED,
        severity = Severity.FAILED,
        createDtTm = resource1CreateDtTm
    )
    private val resource2Id = UUID.fromString("f1907389-d2c2-4b7e-a518-828a68127e5c")
    private val resource2CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusDays(2)
    private val resource2UpdateDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusDays(1)
    private val resource2ReprocessDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val expectedresource2 = Resource(
        id = resource2Id,
        organizationId = "testorg",
        resourceType = "Condition",
        resource = "condition resource",
        status = ResourceStatus.REPROCESSED,
        severity = Severity.FAILED,
        createDtTm = resource2CreateDtTm,
        updateDtTm = resource2UpdateDtTm,
        reprocessDtTm = resource2ReprocessDtTm,
        reprocessedBy = "Josh"
    )
    private val resource3Id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5")
    private val resource3CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val expectedResource3 = Resource(
        id = resource3Id,
        organizationId = "testorg",
        resourceType = "Patient",
        resource = "patient resource",
        status = ResourceStatus.ADDRESSING,
        severity = Severity.WARNING,
        createDtTm = resource3CreateDtTm
    )
    private val resource4Id = UUID.fromString("03d51d53-1a31-49a9-af74-573b456efca5")
    private val resource4CreateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
    private val expectedResource4 = Resource(
        id = resource4Id,
        organizationId = "testorg",
        resourceType = "Patient",
        resource = "patient resource",
        status = ResourceStatus.IGNORED,
        severity = Severity.WARNING,
        createDtTm = resource4CreateDtTm
    )

    @Test
    fun `getResources - works for getting 1 type of status`() {
        val resourceList = listOf(expectedResource, expectedresource2)
        val resourceJson = JacksonManager.objectMapper.writeValueAsString(resourceList)

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(resourceJson)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val response = runBlocking {
            val resources = ResourceClient(url.toString(), client, authenticationService).getResources(
                listOf(ResourceStatus.REPORTED),
                Order.ASC,
                2
            )
            resources
        }
        assertEquals(resourceList, response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources?status=REPORTED&order=ASC&limit=2"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResources - works for getting all type of statuses`() {
        val resourceList = listOf(expectedResource, expectedresource2, expectedResource3, expectedResource4)
        val resourceJson = JacksonManager.objectMapper.writeValueAsString(resourceList)

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(resourceJson)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val response = runBlocking {
            val resources = ResourceClient(url.toString(), client, authenticationService).getResources(
                listOf(),
                Order.ASC,
                10
            )
            resources
        }
        assertEquals(resourceList, response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources?order=ASC&limit=10"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResources - works for null statuses`() {
        val resourceList = listOf(expectedResource, expectedresource2, expectedResource3, expectedResource4)
        val resourceJson = JacksonManager.objectMapper.writeValueAsString(resourceList)

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(resourceJson)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val response = runBlocking {
            val resources = ResourceClient(url.toString(), client, authenticationService).getResources(
                null,
                Order.ASC,
                10
            )
            resources
        }
        assertEquals(resourceList, response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources?order=ASC&limit=10"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResources - works with provided after`() {
        val resourceList = listOf(expectedResource, expectedresource2, expectedResource3, expectedResource4)
        val resourceJson = JacksonManager.objectMapper.writeValueAsString(resourceList)

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(resourceJson)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val after = UUID.randomUUID()
        val response = runBlocking {
            val resources = ResourceClient(url.toString(), client, authenticationService).getResources(
                listOf(),
                Order.ASC,
                10,
                after
            )
            resources
        }
        assertEquals(resourceList, response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources?order=ASC&limit=10&after=$after"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResources - exception handling`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.GatewayTimeout.value)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val exception = assertThrows<ServerFailureException> {
            runBlocking {
                ResourceClient(url.toString(), client, authenticationService).getResources(
                    listOf(ResourceStatus.REPORTED),
                    Order.ASC,
                    1
                )
            }
        }
        assertNotNull(exception.message)
        exception.message?.let { assertTrue(it.contains("504")) }

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources?status=REPORTED&order=ASC&limit=1"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResourceById - works`() {

        val resourceJson = JacksonManager.objectMapper.writeValueAsString(expectedResource)
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(resourceJson)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")

        val response = runBlocking {
            val resources = ResourceClient(url.toString(), client, authenticationService).getResourceById(resource1Id)
            resources
        }
        assertEquals(expectedResource, response)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/$resource1Id"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `getResourceById - exception handling`() {
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.BadRequest.value)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val exception = assertThrows<ClientFailureException> {
            runBlocking {
                ResourceClient(url.toString(), client, authenticationService).getResourceById(resource1Id)
            }
        }

        assertNotNull(exception.message)
        exception.message?.let { assertTrue(it.contains("400")) }

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/$resource1Id"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `addResource works`() {
        val resourceId1 = GeneratedId(id = resource1Id)
        val newIssue1 = NewIssue(
            severity = Severity.FAILED,
            type = "pat-1",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact"
        )
        val newIssue2 = NewIssue(
            severity = Severity.WARNING,
            type = "pat-2",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact"
        )
        val newResource = NewResource(
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "the patient resource",
            issues = listOf(newIssue1, newIssue2),
            createDtTm = OffsetDateTime.now()
        )
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(JacksonManager.objectMapper.writeValueAsString(resourceId1))
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val response = runBlocking {
            val resourceId = ResourceClient(url.toString(), client, authenticationService).addResource(newResource)
            resourceId
        }
        assertEquals(resourceId1.id, response.id)

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `addResource - exception handling`() {
        val newIssue1 = NewIssue(
            severity = Severity.FAILED,
            type = "pat-1",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact"
        )
        val newIssue2 = NewIssue(
            severity = Severity.WARNING,
            type = "pat-2",
            description = "No contact details",
            createDtTm = OffsetDateTime.now(),
            location = "Patient.contact"
        )
        val newResource = NewResource(
            organizationId = "testorg",
            resourceType = "Patient",
            resource = "the patient resource",
            issues = listOf(newIssue1, newIssue2),
            createDtTm = OffsetDateTime.now()
        )
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.BadRequest.value)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val exception = assertThrows<ClientFailureException> {
            runBlocking {
                ResourceClient(url.toString(), client, authenticationService).addResource(newResource)
            }
        }
        assertNotNull(exception.message)
        exception.message?.let { assertTrue(it.contains("400")) }

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `reprocessResourceById - works`() {
        val reprocessResourceRequest = ReprocessResourceRequest(
            user = "testuser",
            comment = "comment"
        )
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val uuid = UUID.randomUUID()
        runBlocking {
            ResourceClient(url.toString(), client, authenticationService).reprocessResource(
                uuid, reprocessResourceRequest
            )
        }

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/$uuid/reprocess"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `reprocessResource - exception handling`() {
        val reprocessResourceRequest = ReprocessResourceRequest(
            user = "testuser",
            comment = "comment"
        )
        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.ServiceUnavailable.value)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val uuid = UUID.randomUUID()
        val exception = assertThrows<ServiceUnavailableException> {
            runBlocking {
                ResourceClient(url.toString(), client, authenticationService).reprocessResource(
                    uuid, reprocessResourceRequest
                )
            }
        }
        assertNotNull(exception.message)
        exception.message?.let { assertTrue(it.contains("503")) }

        val request = mockWebServer.takeRequest()
        assertEquals(true, request.path?.endsWith("/resources/$uuid/reprocess"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `updateResource works`() {
        val resourceId = UUID.randomUUID()
        val updateResource = UpdateResource(status = UpdatableResourceStatus.IGNORED)
        val updatedResource = Resource(
            id = resourceId,
            organizationId = "test",
            resourceType = "Patient",
            resource = "{}",
            status = ResourceStatus.IGNORED,
            severity = Severity.FAILED,
            createDtTm = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
            updateDtTm = OffsetDateTime.now(ZoneOffset.UTC)
        )

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.OK.value)
                .setBody(JacksonManager.objectMapper.writeValueAsString(updatedResource))
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val response = runBlocking {
            ResourceClient(url.toString(), client, authenticationService).updateResource(resourceId, updateResource)
        }
        assertEquals(updatedResource, response)

        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals(true, request.path?.endsWith("/resources/$resourceId"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }

    @Test
    fun `updateResource handles exceptions`() {
        val resourceId = UUID.randomUUID()
        val updateResource = UpdateResource(status = UpdatableResourceStatus.IGNORED)

        val mockWebServer = MockWebServer()
        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(HttpStatusCode.ServiceUnavailable.value)
                .setHeader("Content-Type", "application/json")
        )
        val url = mockWebServer.url("/test")
        val exception = assertThrows<ServiceUnavailableException> {
            runBlocking {
                ResourceClient(url.toString(), client, authenticationService).updateResource(resourceId, updateResource)
            }
        }
        assertNotNull(exception.message)
        exception.message?.let { assertTrue(it.contains("503")) }

        val request = mockWebServer.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals(true, request.path?.endsWith("/resources/$resourceId"))
        assertEquals("Bearer $authenticationToken", request.getHeader("Authorization"))
    }
}
