plugins {
    `maven-publish`
    alias(libs.plugins.interop.docker.integration)
    alias(libs.plugins.interop.junit)
    alias(libs.plugins.interop.spring.boot)
}

dependencies {
    implementation(platform(libs.spring.boot.parent))
    implementation(libs.springdoc.openapi.ui)

    implementation(libs.interop.commonJackson)
    implementation(libs.ehr.data.authority.models)
    implementation(libs.interop.commonKtorm)
    implementation(libs.interop.fhir)
    implementation(libs.interop.kafka)
    implementation(libs.ronin.kafka)
    implementation(libs.event.interop.resource.internal)
    implementation(libs.event.interop.resource.request)

    implementation(libs.ktorm.core)
    implementation(libs.ktorm.support.mysql)
    implementation(libs.uuid.creator)

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly(project(":interop-validation-liquibase"))
    runtimeOnly(libs.liquibase.core)
    runtimeOnly(libs.mysql.connector.java)

    // Needed to format logs for DataDog
    runtimeOnly(libs.logstash.logback.encoder)

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.interop.commonTestDb)
    testImplementation(libs.mockk)
    testImplementation(libs.rider.core)

    testRuntimeOnly("org.testcontainers:mysql")
    itImplementation(libs.common.fhir.r4.models)
    itImplementation(project(":interop-validation-client"))
    itImplementation(libs.ehr.data.authority.models)
    itImplementation(libs.interop.commonHttp)
    itImplementation(libs.interop.fhir)
    itImplementation(libs.interop.kafka)
    itImplementation(libs.event.interop.resource.request)
    itImplementation(libs.ronin.kafka)
    itImplementation(libs.interop.kafka.testing.client)
    itImplementation(libs.kafka.clients)
    itImplementation(libs.ktor.client.core)
    itImplementation(platform(libs.testcontainers.bom))
    itImplementation("org.testcontainers:mysql")
    itImplementation(libs.ktorm.core)
    itImplementation(libs.kotlin.logging)
    itImplementation(project)
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/validation-api.yaml")
    outputDir.set("$buildDir/generated")
    ignoreFileOverride.set("$projectDir/.openapi-generator-ignore")
    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "enumPropertyNaming" to "UPPERCASE",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "packageName" to "com.projectronin.interop.validation.server.generated",
            "basePackage" to "com.projectronin.interop.validation.server.generated",
            "gradleBuildFile" to "false",
        ),
    )
}
