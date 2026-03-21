plugins {
    id("vulnlog.app-convention")
}

description = "Next generation Vulnlog CLI application"

group = "dev.vulnlog"

application {
    mainClass.set("dev.vulnlog.cli.shell.MainKt")
    applicationName = "vulnlog"
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.jacksonYaml)

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
