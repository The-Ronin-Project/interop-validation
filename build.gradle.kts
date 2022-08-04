plugins {
    id("com.projectronin.interop.gradle.publish") apply false
    id("com.projectronin.interop.gradle.version")
}

subprojects {
    apply(plugin = "com.projectronin.interop.gradle.publish")

    // Disable releases hub from running on the subprojects. Main project will handle it all.
    tasks.filter { it.group.equals("releases hub", ignoreCase = true) }.forEach { it.enabled = false }
}
