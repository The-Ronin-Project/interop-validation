package com.projectronin.interop.validation.server

import org.ktorm.database.Database
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import javax.sql.DataSource

/**
 * Main Spring Boot application for the Interop Validation Server
 */
@SpringBootApplication
@ComponentScan("com.projectronin.interop.validation.server")
class ValidationServer {
    /**
     * The Database used by validation based off the [dataSource].
     */
    @Bean
    fun database(dataSource: DataSource): Database = Database.connect(dataSource)
}

fun main(args: Array<String>) {
    runApplication<ValidationServer>(*args)
}
