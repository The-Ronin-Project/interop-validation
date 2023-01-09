package com.projectronin.interop.validation.server.data.helper

import com.github.f4b6a3.uuid.UuidCreator
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.Base64
import java.util.UUID

// This is a helper class for generating UUIDs for use in the DBUnit files. It is disabled by default and should only be removed for local uses of generting a new sample ID.
@Disabled
class BinaryUuidHelper {
    @Test
    fun generate() {
        val uuid = UUID.randomUUID()
        val uuidBytes = UuidCreator.toBytes(uuid)
        val base64 = String(Base64.getEncoder().encode(uuidBytes))

        println("UUID: $uuid")
        println("Binary: $base64")
    }
}
