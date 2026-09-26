pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "vitalis"

// Lets modules depend on each other as `projects.core.model` instead of
// `project(":core:model")` — typo-proof and navigable from the IDE.
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":app")

// Core — shared foundations, no feature knowledge.
include(":core:model")
include(":core:common")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:domain")

// Data — repositories, single source of truth wiring.
include(":data")

// Feature — one module per bottom-nav destination + onboarding.
include(":feature:onboarding")
include(":feature:dashboard")
include(":feature:diary")
include(":feature:tracking")
include(":feature:progress")
include(":feature:profile")
