import org.jetbrains.gradle.ext.settings
import org.jetbrains.gradle.ext.taskTriggers

plugins {
    id("com.projectronin.interop.gradle.server-publish") apply false
    id("com.projectronin.interop.gradle.server-version")

    id("org.openapi.generator") apply false

    // We need to force IntelliJ to do some actions they expose through this plugin.
    id("org.jetbrains.gradle.plugin.idea-ext")
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.base")

    if (project.name != "interop-validation-server") {
        apply(plugin = "com.projectronin.interop.gradle.server-publish")
    }

    // Disable releases hub from running on the subprojects. Main project will handle it all.
    tasks.filter { it.group.equals("releases hub", ignoreCase = true) }.forEach { it.enabled = false }
}

val openapiProjects = listOf(project(":interop-validation-client"), project(":interop-validation-server"))

configure(openapiProjects) {
    apply(plugin = "org.openapi.generator")

    afterEvaluate {
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
    }
}
