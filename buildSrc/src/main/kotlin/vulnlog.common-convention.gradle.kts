plugins {
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-test-fixtures`
}

group = "io.vulnlog"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.1")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

dependencyLocking {
    lockAllConfigurations()
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

tasks.named("check").configure {
    dependsOn(tasks.named("ktlintCheck"))
}

tasks.named("check").configure {
    dependsOn(tasks.named("detekt"))
}
