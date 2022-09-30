plugins {
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.ktor)
    implementation(libs.interop.common)
    implementation(libs.interop.commonHttp)
    implementation(libs.interop.commonJackson)
    implementation(libs.jakarta.validation.api)
    implementation(libs.spring.boot.actuator)

    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
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
