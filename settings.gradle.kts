rootProject.name = "interop-validation"

include("interop-validation-server")

for (project in rootProject.children) {
    project.buildFileName = "${project.name}.gradle.kts"
}

pluginManagement {
    plugins {
        id("com.projectronin.interop.gradle.publish") version "2.0.0"
        id("com.projectronin.interop.gradle.version") version "2.0.0"
    }

    repositories {
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-snapshots/")
            mavenContent {
                snapshotsOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-releases/")
            mavenContent {
                releasesOnly()
            }
        }
        maven {
            url = uri("https://repo.devops.projectronin.io/repository/maven-public/")
            mavenContent {
                releasesOnly()
            }
        }
        mavenLocal()
        gradlePluginPortal()
    }
}
