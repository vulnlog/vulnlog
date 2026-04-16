// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import kotlin.io.path.createTempDirectory

private val BUILD_FILE =
    """
    plugins {
        id("dev.vulnlog.plugin")
    }
    """.trimIndent()

private fun projectDir(): File {
    val dir = createTempDirectory("vulnlog-init-test").toFile()
    dir.resolve("build.gradle.kts").writeText(BUILD_FILE)
    dir.resolve("settings.gradle.kts").writeText("")
    return dir
}

private fun runner(
    projectDir: File,
    vararg args: String,
): GradleRunner =
    GradleRunner
        .create()
        .withProjectDir(projectDir)
        .withPluginClasspath()
        .withArguments(*args)

class VulnlogInitTaskTest :
    FunSpec({

        test("creates a vulnlog file with configured values") {
            val dir = projectDir()

            val result =
                runner(
                    dir,
                    "vulnlogInit",
                    "-Pvulnlog.organization=Acme Corp",
                    "-Pvulnlog.name=Widget",
                    "-Pvulnlog.author=Alice",
                    "-Pvulnlog.output=vulnlog.yaml",
                ).build()

            result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.SUCCESS
            val content = dir.resolve("vulnlog.yaml").readText()
            content shouldContain "Acme Corp"
            content shouldContain "Widget"
            content shouldContain "Alice"
            content shouldContain "schemaVersion:"
        }

        test("fails when required properties are missing") {
            val dir = projectDir()

            val result =
                runner(
                    dir,
                    "vulnlogInit",
                    "-Pvulnlog.name=Widget",
                    "-Pvulnlog.author=Alice",
                    "-Pvulnlog.output=vulnlog.yaml",
                ).buildAndFail()

            result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.FAILED
        }

        test("fails when output is not specified") {
            val dir = projectDir()

            val result =
                runner(
                    dir,
                    "vulnlogInit",
                    "-Pvulnlog.organization=Acme Corp",
                    "-Pvulnlog.name=Widget",
                    "-Pvulnlog.author=Alice",
                ).buildAndFail()

            result.task(":vulnlogInit")?.outcome shouldBe TaskOutcome.FAILED
        }
    })
