// Pure Kotlin/JVM — no Android APIs, so the domain vocabulary stays framework-free
// and is ready to move to a KMP source set later (spec §6.3).
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(libs.versions.jvmTarget.get().toInt())
}

dependencies {
    testImplementation(libs.junit5.api)
    testImplementation(libs.truth)
    testRuntimeOnly(libs.junit5.engine)
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
