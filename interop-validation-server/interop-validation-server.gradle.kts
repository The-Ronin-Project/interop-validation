import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    `maven-publish`
    id("com.projectronin.interop.gradle.junit")
    id("com.projectronin.interop.gradle.spring")
    id("org.springframework.boot")

    id("org.openapi.generator")
    id("org.jetbrains.gradle.plugin.idea-ext")
}

dependencies {
    implementation(platform(libs.spring.boot.parent))
    implementation(libs.springdoc.openapi.ui)

    implementation(libs.ktorm.core)
    implementation(libs.ktorm.support.mysql)
    implementation(libs.uuid.creator)

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    runtimeOnly(project(":interop-validation-liquibase"))
    runtimeOnly(libs.liquibase.core)
    runtimeOnly(libs.mysql.connector.java)

    testImplementation(libs.interop.commonTestDb)
    testImplementation(libs.mockk)
    testImplementation(libs.rider.core)

    testRuntimeOnly(libs.testcontainers.mysql)
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

tasks {
    val openApiGenerate by getting

    sourceSets {
        main {
            java {
                srcDir(openApiGenerate)
            }
        }
    }

    val compileJava by getting

    // Fixes some implicit dependency mess caused by the above
    val sourcesJar by getting {
        dependsOn(compileJava)
    }

    rootProject.idea.project.settings {
        taskTriggers {
            afterSync(openApiGenerate)
        }
    }
}

ktlint {
    filter {
        exclude {
            it.file.path.contains("/generated/")
        }
    }
}

tasks.withType(JacocoReport::class.java).forEach {
    afterEvaluate {
        it.classDirectories.setFrom(
            files(
                it.classDirectories.files.map {
                    fileTree(it).apply {
                        exclude(
                            "**/generated/**"
                        )
                    }
                }
            )
        )
    }
}

publishing {
    repositories {
        maven {
            name = "nexus"
            credentials {
                username = System.getenv("NEXUS_USER")
                password = System.getenv("NEXUS_TOKEN")
            }
            url = if (project.version.toString().endsWith("SNAPSHOT")) {
                uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            } else {
                uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            }
        }
    }
    publications {
        create<MavenPublication>("bootJava") {
            artifact(tasks.getByName("bootJar"))
        }
    }
}