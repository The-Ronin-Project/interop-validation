plugins {
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    // We don't actually use Spring Boot, but this parent can ensure our client and server are on the same versions.
    implementation(platform(libs.spring.boot.parent))

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("jakarta.validation:jakarta.validation-api")

    testImplementation(libs.mockk)
}

openApiGenerate {
    generatorName.set("kotlin-spring")
    inputSpec.set("$projectDir/../interop-validation-server/validation-api.yaml")
    outputDir.set("$buildDir/generated")
    configOptions.set(
        mapOf(
            "enumPropertyNaming" to "UPPERCASE",
            "packageName" to "com.projectronin.interop.validation.client.generated",
            "gradleBuildFile" to "false",
            "documentationProvider" to "none" // Prevent Swagger annotations
        )
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "models" to ""
        )
    )
}
