package com.projectronin.interop.validation.server

import com.projectronin.interop.common.http.spring.HttpSpringConfig
import com.projectronin.interop.validation.client.ResourceClient
import com.projectronin.interop.validation.client.generated.models.NewResource
import com.projectronin.interop.validation.server.auth.LocalAuthService
import com.projectronin.interop.validation.server.data.binding.CommentDOs
import com.projectronin.interop.validation.server.data.binding.IssueCommentDOs
import com.projectronin.interop.validation.server.data.binding.IssueDOs
import com.projectronin.interop.validation.server.data.binding.ResourceCommentDOs
import com.projectronin.interop.validation.server.data.binding.ResourceDOs
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.ktorm.database.Database
import org.ktorm.dsl.deleteAll
import org.testcontainers.containers.DockerComposeContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID

@Testcontainers
abstract class BaseValidationIT {
    companion object {
        @Container
        val docker =
            DockerComposeContainer(File(ResourceIT::class.java.getResource("docker-compose.yaml")!!.file))
                .waitingFor("validation-server", Wait.forLogMessage(".*Started ValidationServerKt.*", 1))
    }

    protected val database = Database.connect(url = "jdbc:mysql://springuser:ThePassword@localhost:3306/validation-db")

    protected val serverUrl = "http://localhost:8080"
    protected val httpClient = HttpSpringConfig().getHttpClient()
    protected val authenticationService = LocalAuthService(httpClient)

    protected val resourceClient = ResourceClient(serverUrl, httpClient, authenticationService)

    @AfterEach
    fun tearDown() {
        purgeData()
    }

    /**
     * Adds a new resource.
     */
    protected fun addResource(newResource: NewResource): UUID = runBlocking {
        resourceClient.addResource(newResource).id!!
    }

    private fun purgeData() {
        database.deleteAll(CommentDOs)
        database.deleteAll(IssueCommentDOs)
        database.deleteAll(ResourceCommentDOs)
        database.deleteAll(IssueDOs)
        database.deleteAll(ResourceDOs)
    }

    /**
     * Converts the OffsetDateTime to more closely resemble MySQL. This means it is capped at microseconds, but with a rounding based off the nanoseconds.
     */
    protected fun OffsetDateTime.likeMySQL(): OffsetDateTime {
        val nanoseconds = nano % 1000
        val adjustment = if (nanoseconds >= 500) 1 else 0
        val microseconds = (nano / 1000) + adjustment

        return this.withNano(microseconds * 1000)
    }
}
