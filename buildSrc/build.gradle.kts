plugins {
    `kotlin-dsl`
}

repositories {
    // Use the plugin portal to apply community plugins in convention plugins.
    gradlePluginPortal()
}

dependencies {
    implementation(libs.dependencyCheckGradle)
// Disable until fully supported in JDK 25 with Kotlin 2.3.0
//    implementation(libs.detektGradlePlugin)
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.ktlintGradle)
    implementation(libs.snykGradlePlugin)
}
