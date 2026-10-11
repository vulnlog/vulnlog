import org.cyclonedx.model.Component

plugins {
    id("vulnlog.app-convention")
    id("vulnlog.sbom-convention")
}

description = "Vulnlog command-line interface"

group = "dev.vulnlog"

application {
    mainClass.set("dev.vulnlog.cli.shell.MainKt")
    applicationName = "vulnlog"
}

dependencies {
    implementation(project(":lib"))
    implementation(libs.clikt)

    testImplementation(testFixtures(project(":lib")))
    testImplementation(libs.kotestAssertionsCoreJvm)
    testImplementation(libs.kotestRunnerJunit5Jvm)
}

val generateBuildInfo by tasks.registering {
    val versionValue = project.version.toString()
    val outputDir = layout.buildDirectory.dir("generated/source/buildInfo")

    inputs.property("version", versionValue)
    outputs.dir(outputDir)

    doLast {
        val buildInfoFile = outputDir.get().file("dev/vulnlog/cli/BuildInfo.kt").asFile
        buildInfoFile.parentFile.mkdirs()
        buildInfoFile.writeText(
            """
            package dev.vulnlog.cli

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

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

sbom {
    rootPurl = "pkg:generic/vulnlog"
    vexDocument = layout.settingsDirectory.file(".vex/vulnlog-cli.openvex.json")
    containerPurl = "pkg:oci/vulnlog?repository_url=ghcr.io/vulnlog/vulnlog"
    containerVexDocument = layout.settingsDirectory.file(".vex/vulnlog-container.openvex.json")
}

tasks.cyclonedxDirectBom {
    projectType = Component.Type.APPLICATION
    componentName = "vulnlog"
    jsonOutput = layout.buildDirectory.file("sbom/vulnlog-$version.cdx.json")
}
