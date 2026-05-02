import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.plugin.devel.tasks.PluginUnderTestMetadata

plugins {
    id("vulnlog.common-convention")
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "2.1.1"
    id("com.gradleup.shadow") version "9.4.1"
}

description = "Vulnlog Gradle plugin"

group = "dev.vulnlog"

val shaded: Configuration by configurations.creating

configurations {
    compileOnly.get().extendsFrom(shaded)
    testImplementation.get().extendsFrom(shaded)
}

dependencies {
    shaded(project(":lib"))

    testImplementation(gradleTestKit())
    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}

val generateBuildInfo by tasks.registering {
    val versionValue = project.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/source/buildInfo")

    inputs.property("version", versionValue)
    outputs.dir(outputDir)

    doLast {
        val buildInfoFile = outputDir.get().file("dev/vulnlog/gradle/BuildInfo.kt").asFile
        buildInfoFile.parentFile.mkdirs()
        buildInfoFile.writeText(
            """
            package dev.vulnlog.gradle

            internal object BuildInfo {
                const val VERSION = "$versionValue"
            }
            """.trimIndent() + "\n",
        )
    }
}

kotlin {
    sourceSets {
        main {
            kotlin.srcDir(generateBuildInfo)
        }
    }
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    configurations = listOf(shaded)
    relocate("tools.jackson", "dev.vulnlog.shaded.tools.jackson")
    relocate("com.fasterxml.jackson.annotation", "dev.vulnlog.shaded.com.fasterxml.jackson.annotation")
    relocate("com.github.packageurl", "dev.vulnlog.shaded.com.github.packageurl")
    mergeServiceFiles()
}

tasks.named<Jar>("jar") {
    enabled = false
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<PluginUnderTestMetadata>("pluginUnderTestMetadata") {
    pluginClasspath.from(shaded)
}

gradlePlugin {
    website = "https://github.com/vulnlog/vulnlog"
    vcsUrl = "https://github.com/vulnlog/vulnlog.git"
    plugins {
        create("vulnlog") {
            id = "dev.vulnlog.plugin"
            displayName = "Vulnlog"
            description =
                """
                Supply chain security as code. Manage vulnerability records next to the
                source, validate Vulnlog YAML files, generate suppression files for
                downstream scanners (Trivy, Snyk, generic format), and produce
                self-contained HTML vulnerability reports.
                """.trimIndent()
            tags =
                listOf(
                    "vulnlog",
                    "vulnerability",
                    "security",
                    "supply-chain",
                    "supply-chain-security",
                    "cve",
                    "audit",
                    "suppression",
                    "sca",
                    "trivy",
                    "snyk",
                )
            implementationClass = "dev.vulnlog.gradle.VulnlogPlugin"
        }
    }
}
