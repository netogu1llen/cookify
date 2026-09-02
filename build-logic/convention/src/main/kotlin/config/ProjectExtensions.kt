/*
 * Project extension utilities
 * Provides: Version catalog accessor
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Access the libs version catalog from any Project.
 *
 * MUST stay `internal`. As a public top-level extension in the default package it
 * lands on every consumer module's buildscript classpath and shadows Gradle's
 * generated `LibrariesForLibs` accessor, which makes `libs.androidx.core` and
 * friends fail to resolve in every build.gradle.kts.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")
