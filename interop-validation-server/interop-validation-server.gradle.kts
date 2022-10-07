plugins {
    `maven-publish`
    id("com.projectronin.interop.gradle.docker-integration")
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring-boot")
}

dependencies {
    implementation(platform(libs.spring.boot.parent))
    implementation(libs.springdoc.openapi.ui)

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

    testImplementation(platform(libs.testcontainers.bom))
    testImplementation(libs.interop.commonTestDb)
    testImplementation(libs.mockk)
    testImplementation(libs.rider.core)

    testRuntimeOnly("org.testcontainers:mysql")

    itImplementation(project(":interop-validation-client"))
    itImplementation(libs.interop.commonHttp)
    itImplementation(libs.interop.fhir)
    itImplementation(libs.ktor.client.core)
    itImplementation("org.testcontainers:testcontainers")
    itImplementation("org.testcontainers:junit-jupiter")
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/validation-api.yaml")
    outputDir.set("$buildDir/generated")
    ignoreFileOverride.set("$projectDir/.openapi-generator-ignore")
    configOptions.set(
        mapOf(
            "enumPropertyNaming" to "UPPERCASE",
            "interfaceOnly" to "true",
            "useTags" to "true",
            "packageName" to "com.projectronin.interop.validation.server.generated",
            "basePackage" to "com.projectronin.interop.validation.server.generated",
            "gradleBuildFile" to "false"
        )
    )
}
