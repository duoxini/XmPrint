pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "XmPrintTablet"
include(":app")

// Local research modules are intentionally excluded from the public source tree.
// Keep them available in this working copy without making a clean clone depend on them.
listOf("research", "protocol", "tasks").forEach { module ->
    if (file(module).resolve("build.gradle.kts").isFile) {
        include(":$module")
    }
}
