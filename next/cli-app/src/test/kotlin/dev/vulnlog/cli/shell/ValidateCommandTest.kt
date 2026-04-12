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
        verdict: not affected
        justification: vulnerable code not in execute path
    """.trimIndent()

private val INVALID_VULNLOG_YAML =
    """
    ---
    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team
    """.trimIndent()

class ValidateCommandTest :
    FunSpec({

        test("validate succeeds with valid vulnlog file") {
            val tempFile = Files.createTempFile("vulnlog", ".vl.yaml").toFile()
            try {
                tempFile.writeText(VALID_VULNLOG_YAML)

                val result = ValidateCommand().test(tempFile.absolutePath)

                result.statusCode shouldBe 0
                result.stdout shouldContain "Validation OK"
            } finally {
                tempFile.delete()
            }
        }

        test("validate reads from stdin when - is passed") {
            val originalStdin = System.`in`
            try {
                System.setIn(ByteArrayInputStream(VALID_VULNLOG_YAML.toByteArray()))

                val result = ValidateCommand().test("-")

                result.statusCode shouldBe 0
                result.stdout shouldContain "Validation OK"
            } finally {
                System.setIn(originalStdin)
            }
        }

        test("validate fails with no arguments") {
            val result = ValidateCommand().test("")

            result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
            result.stderr shouldContain "No input provided"
        }

        test("validate fails when file does not exist") {
            val result = ValidateCommand().test("/nonexistent/vulnlog.vl.yaml")

            result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
            result.stderr shouldContain "does not exist"
        }

        test("validate fails when file name does not match expected pattern") {
            val tempFile = Files.createTempFile("invalid-name", ".txt").toFile()
            try {
                tempFile.writeText(VALID_VULNLOG_YAML)

                val result = ValidateCommand().test(tempFile.absolutePath)

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "File name must be"
            } finally {
                tempFile.delete()
            }
        }

        test("validate reports parsing errors for invalid vulnlog YAML") {
            val tempFile = Files.createTempFile("vulnlog", ".vl.yaml").toFile()
            try {
                tempFile.writeText(INVALID_VULNLOG_YAML)

                val result = ValidateCommand().test(tempFile.absolutePath)

                result.statusCode shouldBe ExitCode.VALIDATION_ERROR.ordinal
                result.stderr shouldContain "Parsing"
                result.stderr shouldContain "failed"
            } finally {
                tempFile.delete()
            }
        }

        test("validate reports parsing errors from stdin for invalid vulnlog YAML") {
            val originalStdin = System.`in`
            try {
                System.setIn(ByteArrayInputStream(INVALID_VULNLOG_YAML.toByteArray()))

                val result = ValidateCommand().test("-")

                result.statusCode shouldBe ExitCode.VALIDATION_ERROR.ordinal
                result.stderr shouldContain "Parsing of <stdin> failed"
            } finally {
                System.setIn(originalStdin)
            }
        }
    })
