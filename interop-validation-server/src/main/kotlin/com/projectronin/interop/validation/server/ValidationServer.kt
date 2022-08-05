package com.projectronin.interop.validation.server

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

/**
 * Main Spring Boot application for the Interop Validation Server
 */
@SpringBootApplication
@ComponentScan("com.projectronin.interop.validation.server")
class ValidationServer

fun main(args: Array<String>) {
    runApplication<ValidationServer>(*args)
}
