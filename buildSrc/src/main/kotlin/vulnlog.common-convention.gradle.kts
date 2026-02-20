plugins {
    // disable detekt because of missing support for java 25
//    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlin.jvm")
    id("org.jlleitschuh.gradle.ktlint")
    `java-test-fixtures`
}

group = "dev.vulnlog"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.8.1")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.8.1")
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

//tasks.named("check").configure {
//    dependsOn(tasks.named("detekt"))
//}
