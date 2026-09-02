/*
 * Print APKs task configuration
 * Creates a task that prints where the built APKs land.
 *
 * AGP 9 note: SingleArtifact.APK is a *directory* artifact and only exists on
 * application variants. The old `artifacts.getAll(SingleArtifact.APK)` overload
 * takes a MultipleArtifact and no longer compiles, so this uses `get(...)` and
 * skips library variants entirely.
 */

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register

/**
 * Configure a task to print the APK output directory for each application variant.
 * Usage: ./gradlew printDebugApks
 */
internal fun Project.configurePrintApksTask(
    extension: AndroidComponentsExtension<*, *, *>,
) {
    extension.onVariants { variant ->
        if (variant !is ApplicationVariant) return@onVariants

        val variantName = variant.name
        // Captured as a Provider so the task stays configuration-cache compatible.
        val apkDirectory = variant.artifacts.get(SingleArtifact.APK)

        tasks.register("print${variantName.replaceFirstChar(Char::titlecase)}Apks") {
            group = "help"
            description = "Prints the APK output directory for the $variantName variant"

            doLast {
                println("APKs ($variantName): ${apkDirectory.get().asFile.absolutePath}")
            }
        }
    }
}
