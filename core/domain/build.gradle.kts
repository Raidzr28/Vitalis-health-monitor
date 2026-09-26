// Pure Kotlin/JVM. Every calculator in this module is deterministic and unit-tested —
// this is where the 80% coverage target from spec §15.1 lives.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testImplementation(libs.truth)
    testImplementation(libs.kotlinx.coroutines.test)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
