plugins {
// Disable until fully supported in JDK 25 with Kotlin 2.3.0
//    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    id("com.diffplug.spotless")
    `java-test-fixtures`
}

group = "dev.vulnlog"

repositories {
    mavenLocal()
    mavenCentral()
}

val languageLevel = 25

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(languageLevel))
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(languageLevel))
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

dependencyLocking {
    lockAllConfigurations()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(17)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.register("resolveAndLockAll") {
    notCompatibleWithConfigurationCache("Filters configurations at execution time")
    doFirst {
        require(gradle.startParameter.isWriteDependencyLocks) {
            "$path must be run from the command line with the `--write-locks` flag"
        }
    }
    doLast {
        configurations.filter { it.isCanBeResolved }.forEach { it.resolve() }
    }
}

spotless {
    kotlin {
        targetExclude("build/**")
        licenseHeader(
            """
            // Copyright the Vulnlog contributors
            // SPDX-License-Identifier: Apache-2.0
            """.trimIndent() + "\n\n",
        )
    }
}

tasks.named("check").configure {
    dependsOn(tasks.named("ktlintCheck"))
    dependsOn(tasks.named("spotlessCheck"))
}

// Disable until fully supported in JDK 25 with Kotlin 2.3.0
// tasks.named("check").configure {
//     dependsOn(tasks.named("detekt"))
// }
