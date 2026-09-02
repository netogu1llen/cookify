pluginManagement {
    includeBuild("build-logic")
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
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
    // Gradle 9 descubre gradle/libs.versions.toml automaticamente; declararlo
    // con versionCatalogs { from(...) } seria una segunda importacion y falla.
}

// Habilita los accessors tipados de proyecto (projects.core.model en vez de project(":core:model")).
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "cookify"

// App: entry point, Hilt graph, Navigation3 backstack
include(":app")

// Core: shared library code, strict dependency direction core -> feature is forbidden
include(":core:model")     // Pure Kotlin: domain models and enums
include(":core:common")    // Pure Kotlin: dispatchers, result types, formatting
include(":core:domain")    // Pure Kotlin: repository interfaces + weekly-plan engine
include(":core:data")      // Catalog loading (assets JSON), pricing, repository impls
include(":core:database")  // Room 3: saved weeks
include(":core:ui")        // Design system: dark theme + shared composables

// Features: self-contained, never depend on each other
include(":feature:onboarding")
include(":feature:planning")
include(":feature:week")
include(":feature:home")

// Architecture guardrail: features must not depend on other features.
// Uses ProjectDependency.path (getDependencyProject() was removed in Gradle 9).
gradle.projectsLoaded {
    rootProject.allprojects {
        afterEvaluate {
            configurations.findByName("implementation")?.allDependencies
                ?.filterIsInstance<ProjectDependency>()
                ?.forEach { dependency ->
                    val from = path
                    val to = dependency.path
                    if (from.startsWith(":feature:") && to.startsWith(":feature:") && from != to) {
                        logger.warn("VIOLACION: $from depende de $to. Los modulos feature no pueden depender entre si.")
                    }
                    if (from.startsWith(":core:") && to.startsWith(":feature:")) {
                        logger.warn("VIOLACION: $from (core) depende de $to (feature). La direccion es feature -> core.")
                    }
                }
        }
    }
}
