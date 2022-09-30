plugins {
    id("com.projectronin.interop.gradle.junit")
}

dependencies {
    implementation(libs.bundles.jackson)
    implementation(libs.jakarta.validation.api)

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
