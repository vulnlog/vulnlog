package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.nio.file.Files

private val VALID_VULNLOG_YAML =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 1.0.0
        published_at: 2026-01-15

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 1.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: >
          The vulnerable code path is not reachable in our application
          because we only use the safe subset of the API.
        verdict: not_affected
        justification: vulnerable_code_not_in_execute_path
    """.trimIndent()

class SuppressCommandTest :
    FunSpec({

        test("suppress writes to file with valid file path") {
            val tempFile = Files.createTempFile("vulnlog", ".vl.yaml").toFile()
            val outputDir = Files.createTempDirectory("vulnlog-suppress-output")
            try {
                tempFile.writeText(VALID_VULNLOG_YAML)

                val result = SuppressCommand().test("${tempFile.absolutePath} -o ${outputDir.toAbsolutePath()}")

                result.statusCode shouldBe 0
                result.stdout shouldContain "Suppression file created at:"
            } finally {
                outputDir.toFile().deleteRecursively()
                tempFile.delete()
            }
        }

        test("suppress writes to stdout with -o -") {
            val tempFile = Files.createTempFile("vulnlog", ".vl.yaml").toFile()
            try {
                tempFile.writeText(VALID_VULNLOG_YAML)

                val result = SuppressCommand().test("${tempFile.absolutePath} -o -")

                result.statusCode shouldBe 0
                result.stdout shouldContain "CVE-2026-1234"
            } finally {
                tempFile.delete()
            }
        }

        test("suppress reads from stdin and writes to stdout") {
            val originalStdin = System.`in`
            try {
                System.setIn(ByteArrayInputStream(VALID_VULNLOG_YAML.toByteArray()))

                val result = SuppressCommand().test("- -o -")

                result.statusCode shouldBe 0
                result.stdout shouldContain "CVE-2026-1234"
            } finally {
                System.setIn(originalStdin)
            }
        }

        test("suppress reads from stdin and writes to directory") {
            val originalStdin = System.`in`
            val outputDir = Files.createTempDirectory("vulnlog-suppress-output")
            try {
                System.setIn(ByteArrayInputStream(VALID_VULNLOG_YAML.toByteArray()))

                val result = SuppressCommand().test("- -o ${outputDir.toAbsolutePath()}")

                result.statusCode shouldBe 0
                result.stdout shouldContain "Suppression file created at:"
            } finally {
                System.setIn(originalStdin)
                outputDir.toFile().deleteRecursively()
            }
        }

        test("suppress fails when file does not exist") {
            val result = SuppressCommand().test("/nonexistent/vulnlog.vl.yaml")

            result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
            result.stderr shouldContain "does not exist"
        }

        test("suppress fails when file name does not match expected pattern") {
            val tempFile = Files.createTempFile("invalid-name", ".txt").toFile()
            try {
                tempFile.writeText(VALID_VULNLOG_YAML)

                val result = SuppressCommand().test(tempFile.absolutePath)

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "file name must be"
            } finally {
                tempFile.delete()
            }
        }

        test("suppress fails with invalid vulnlog YAML from stdin") {
            val originalStdin = System.`in`
            try {
                val invalidVulnlog = "---\nproject:\n  organization: Acme\n  name: Test\n  author: Bob\n"
                System.setIn(ByteArrayInputStream(invalidVulnlog.toByteArray()))

                val result = SuppressCommand().test("-")

                result.statusCode shouldBe ExitCode.VALIDATION_ERROR.ordinal
                result.stderr shouldContain "Parsing of <stdin> failed"
            } finally {
                System.setIn(originalStdin)
            }
        }
    })
