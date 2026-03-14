plugins {
    `kotlin-dsl`
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

// Compile with JDK 25 but emit JVM 17 bytecode so the compiled convention
// plugins can be loaded by any JVM that satisfies Gradle 9's minimum (JDK 17).
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation(libs.graalvmNativeGradlePlugin)
// Disable until fully supported in JDK 25 with Kotlin 2.3.0
//    implementation(libs.detektGradlePlugin)
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.ktlintGradle)
}
