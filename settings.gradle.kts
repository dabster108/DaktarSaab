// This block defines repositories for resolving plugins.
pluginManagement {
    repositories {
        google {
            // This 'content' block helps speed up resolution by narrowing down searches
            // for common Android/Google/androidx groups to only the Google repository.
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal() // The Gradle Plugin Portal for common Gradle plugins
    }
}

// This block defines repositories for resolving dependencies for your project's modules.
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Recommended for security and consistency
    repositories {
        google() // Google's Maven repository
        mavenCentral() // Maven Central repository
        // If you have any other custom repositories, add them here.
        // maven { url 'https://jitpack.io' } // Example for JitPack (if you use libraries from JitPack)
    }
}

rootProject.name = "DaktarSaab" // Your project name
include(":app") // Include your app module

