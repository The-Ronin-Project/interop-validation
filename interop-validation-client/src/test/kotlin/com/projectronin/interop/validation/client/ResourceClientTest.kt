package com.projectronin.interop.validation.client

import com.projectronin.interop.validation.client.generated.models.Order
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ResourceClientTest {
    // TODO: Remove. Just a test for coverage pending actual implementation.
    @Test
    fun `test stubs`() {
        val resourceClient = ResourceClient()
        assertThrows<NotImplementedError> { resourceClient.getResources(listOf(), Order.ASC, 5, null) }
        assertThrows<NotImplementedError> { resourceClient.getResourceById(UUID.randomUUID()) }
        assertThrows<NotImplementedError> { resourceClient.addResource(mockk()) }
        assertThrows<NotImplementedError> { resourceClient.reprocessResource(UUID.randomUUID(), mockk()) }
    }
}
