import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.app.cookify"
version = "1.0.0"

// Se compila PARA Java 21 aunque se compile CON el JDK que tenga el equipo.
// Asi el artefacto corre en la imagen de produccion, que es un JRE 21, sin
// obligar a cada integrante a instalar exactamente ese JDK.
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        // Las advertencias del compilador son defectos que todavia no fallaron.
        allWarningsAsErrors.set(true)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.cors)
    implementation(libs.ktor.serialization.json)

    // Cliente HTTP del recolector. CIO y no el motor de Android porque esto
    // corre en un servidor.
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)

    implementation(libs.kotlinx.coroutines)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.hikari)
    runtimeOnly(libs.postgresql)
    implementation(libs.logback)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.ktor.server.test.host)
}

application {
    mainClass.set("com.app.cookify.server.AplicacionKt")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
