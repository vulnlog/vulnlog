// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.nio.file.Files

private fun vulnlogYaml(
    projectName: String = "Acme Web App",
    releaseId: String = "1.0.0",
    cveId: String = "CVE-2026-1234",
) = """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: $projectName
      author: Acme Corp Security Team

    releases:
      - id: $releaseId
        published_at: 2026-01-15

    vulnerabilities:

      - id: $cveId
        releases: [ $releaseId ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: >
          The vulnerable code path is not reachable in our application
          because we only use the safe subset of the API.
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

class ReportCommandTest :
    FunSpec({

        test("report generates output for single file") {
            val tempFile = Files.createTempFile("vulnlog", ".vl.yaml").toFile()
            val outputFile = Files.createTempFile("report", ".html").toFile()
            try {
                tempFile.writeText(vulnlogYaml())

                val result =
                    ReportCommand().test("${tempFile.absolutePath} -o ${outputFile.absolutePath}")

                result.statusCode shouldBe 0
                result.stdout shouldContain "Report written to:"
                val html = outputFile.readText()
                html shouldContain "<!DOCTYPE html>"
                html shouldContain "CVE-2026-1234"
            } finally {
                tempFile.delete()
                outputFile.delete()
            }
        }

        test("report merges multiple files with shared project") {
            val file1 = Files.createTempFile("vulnlog-1x", ".vl.yaml").toFile()
            val file2 = Files.createTempFile("vulnlog-2x", ".vl.yaml").toFile()
            val outputFile = Files.createTempFile("report", ".html").toFile()
            try {
                file1.writeText(vulnlogYaml(releaseId = "1.0.0", cveId = "CVE-2026-1234"))
                file2.writeText(vulnlogYaml(releaseId = "2.0.0", cveId = "CVE-2026-5678"))

                val result =
                    ReportCommand().test(
                        "${file1.absolutePath} ${file2.absolutePath} -o ${outputFile.absolutePath}",
                    )

                result.statusCode shouldBe 0
                val html = outputFile.readText()
                html shouldContain "CVE-2026-1234"
                html shouldContain "CVE-2026-5678"
            } finally {
                file1.delete()
                file2.delete()
                outputFile.delete()
            }
        }

        test("report merges same CVE from multiple files into one entry") {
            val file1 = Files.createTempFile("vulnlog-1x", ".vl.yaml").toFile()
            val file2 = Files.createTempFile("vulnlog-2x", ".vl.yaml").toFile()
            val outputFile = Files.createTempFile("report", ".html").toFile()
            try {
                file1.writeText(vulnlogYaml(releaseId = "1.0.0"))
                file2.writeText(vulnlogYaml(releaseId = "2.0.0"))

                val result =
                    ReportCommand().test(
                        "${file1.absolutePath} ${file2.absolutePath} -o ${outputFile.absolutePath}",
                    )

                result.statusCode shouldBe 0
                val html = outputFile.readText()
                html shouldContain "CVE-2026-1234"
                html shouldContain "1.0.0"
                html shouldContain "2.0.0"
            } finally {
                file1.delete()
                file2.delete()
                outputFile.delete()
            }
        }

        test("report fails when projects differ") {
            val file1 = Files.createTempFile("vulnlog-1x", ".vl.yaml").toFile()
            val file2 = Files.createTempFile("vulnlog-2x", ".vl.yaml").toFile()
            try {
                file1.writeText(vulnlogYaml(projectName = "Project A"))
                file2.writeText(vulnlogYaml(projectName = "Project B"))

                val result =
                    ReportCommand().test("${file1.absolutePath} ${file2.absolutePath}")

                result.statusCode shouldBe ExitCode.VALIDATION_ERROR.ordinal
                result.stderr shouldContain "same project metadata"
            } finally {
                file1.delete()
                file2.delete()
            }
        }

        test("report reads from stdin") {
            val originalStdin = System.`in`
            val outputFile = Files.createTempFile("report", ".html").toFile()
            try {
                System.setIn(ByteArrayInputStream(vulnlogYaml().toByteArray()))

                val result = ReportCommand().test("- -o ${outputFile.absolutePath}")

                result.statusCode shouldBe 0
                val html = outputFile.readText()
                html shouldContain "<!DOCTYPE html>"
                html shouldContain "CVE-2026-1234"
            } finally {
                System.setIn(originalStdin)
                outputFile.delete()
            }
        }

        test("report fails when no input provided") {
            val result = ReportCommand().test("")

            result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
            result.stderr shouldContain "No input provided"
        }

        test("report fails when file does not exist") {
            val result = ReportCommand().test("/nonexistent/vulnlog.vl.yaml")

            result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
            result.stderr shouldContain "does not exist"
        }

        test("report fails when file name does not match expected pattern") {
            val tempFile = Files.createTempFile("invalid-name", ".txt").toFile()
            try {
                tempFile.writeText(vulnlogYaml())

                val result = ReportCommand().test(tempFile.absolutePath)

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "File name must be"
            } finally {
                tempFile.delete()
            }
        }
    })
