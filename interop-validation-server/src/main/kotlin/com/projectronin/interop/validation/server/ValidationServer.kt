package com.projectronin.interop.validation.server

import org.ktorm.database.Database
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableScheduling
import javax.sql.DataSource
import com.projectronin.ehr.dataauthority.models.kafka.KafkaTopicConfig as EHRDATopicConfig

/**
 * Main Spring Boot application for the Interop Validation Server
 */

@Import(EHRDATopicConfig::class)
@EnableScheduling
@SpringBootApplication
@ComponentScan("com.projectronin.interop.validation.server", "com.projectronin.interop.kafka")
class ValidationServer {
    /**
     * The Database used by validation based off the [dataSource].
     */
    @Bean
    fun database(dataSource: DataSource): Database = Database.connectWithSpringSupport(dataSource)
}

fun main(args: Array<String>) {
    runApplication<ValidationServer>(*args)
}
