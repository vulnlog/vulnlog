plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.detektGradlePlugin)
    implementation(libs.kotlinGradlePlugin)
    implementation(libs.ktlintGradle)
}

kotlin {
    jvmToolchain(17)
}
