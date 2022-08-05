plugins {
    id("com.projectronin.interop.gradle.publish") apply false
    id("com.projectronin.interop.gradle.version")

    // We need to force IntelliJ to do some actions they expose through this plugin.
    id("org.jetbrains.gradle.plugin.idea-ext")
}

subprojects {
    if (project.name != "interop-validation-server") {
        apply(plugin = "com.projectronin.interop.gradle.publish")
    }

    // Disable releases hub from running on the subprojects. Main project will handle it all.
    tasks.filter { it.group.equals("releases hub", ignoreCase = true) }.forEach { it.enabled = false }
}
