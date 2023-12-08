plugins {
    alias(libs.plugins.interop.junit)
    alias(libs.plugins.interop.spring.framework) // this is only needed to annotate the classes as open
}

dependencies {
    // We don't actually use Spring Boot, but this parent can ensure our client and server are on the same versions.
    implementation(platform(libs.spring.boot.parent))

    implementation(libs.bundles.jackson)
    implementation(libs.bundles.ktor)
    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.commonJackson)
    implementation(libs.jakarta.validation.api)
    implementation(libs.spring.boot.actuator)

    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation("org.springframework:spring-test")
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
            // Prevent Swagger annotations
            "documentationProvider" to "none",
        ),
    )
    globalProperties.set(
        mapOf(
            "apis" to "false",
            "models" to "",
        ),
    )
}
