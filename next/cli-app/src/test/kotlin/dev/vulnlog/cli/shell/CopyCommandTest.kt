package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Files

private val SOURCE_YAML =
    """
    # ${'$'}schema: https://vulnlog.dev/schema/vulnlog-v1.json
    ---
    schemaVersion: "1"

    project:
      organization: Acme Corp
      name: Acme Web App
      author: Acme Corp Security Team

    releases:
      - id: 2.0.0
        published_at: 2026-01-15

    vulnerabilities:

      - id: CVE-2026-1234
        releases: [ 2.0.0 ]
        description: Remote code execution in example-lib
        packages: [ "pkg:npm/example-lib@2.3.0" ]
        reports:
          - reporter: trivy
        analysis: >
          The vulnerable code path is not reachable.
        verdict: not_affected
        justification: vulnerable_code_not_in_execute_path

      - id: CVE-2026-5678
        releases: [ 2.0.0 ]
        description: XSS in other-lib
        packages: [ "pkg:npm/other-lib@1.0.0" ]
        reports:
          - reporter: trivy
        verdict: not_affected
        justification: component_not_present
    """.trimIndent()

private val TARGET_YAML =
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
        published_at: 2025-06-01

    vulnerabilities:

      - id: CVE-2026-0001
        releases: [ 1.0.0 ]
        description: Existing entry
        packages: [ "pkg:npm/foo@1.0.0" ]
        reports:
          - reporter: trivy
        verdict: not_affected
        justification: component_not_present
    """.trimIndent()

class CopyCommandTest :
    FunSpec({

        test("copy single entry to single target") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            val targetFile = Files.createTempFile("target", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)
                targetFile.writeText(TARGET_YAML)

                val result =
                    CopyCommand().test(
                        "${sourceFile.absolutePath} ${targetFile.absolutePath} --vuln-id CVE-2026-1234",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "Copied 'CVE-2026-1234'"

                val targetContent = targetFile.readText()
                targetContent shouldContain "CVE-2026-1234"
                targetContent shouldContain "1.0.0"
                targetContent shouldNotContain "2.0.0"
            } finally {
                sourceFile.delete()
                targetFile.delete()
            }
        }

        test("copy single entry to multiple targets") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            val targetFile1 = Files.createTempFile("target1", ".vl.yaml").toFile()
            val targetFile2 = Files.createTempFile("target2", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)
                targetFile1.writeText(TARGET_YAML)
                targetFile2.writeText(TARGET_YAML)

                val result =
                    CopyCommand().test(
                        "${sourceFile.absolutePath} ${targetFile1.absolutePath} ${targetFile2.absolutePath} --vuln-id CVE-2026-1234",
                    )

                result.statusCode shouldBe 0
                targetFile1.readText() shouldContain "CVE-2026-1234"
                targetFile2.readText() shouldContain "CVE-2026-1234"
            } finally {
                sourceFile.delete()
                targetFile1.delete()
                targetFile2.delete()
            }
        }

        test("copy multiple vuln-ids") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            val targetFile = Files.createTempFile("target", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)
                targetFile.writeText(TARGET_YAML)

                val result =
                    CopyCommand().test(
                        "${sourceFile.absolutePath} ${targetFile.absolutePath} --vuln-id CVE-2026-1234 --vuln-id CVE-2026-5678",
                    )

                result.statusCode shouldBe 0
                val targetContent = targetFile.readText()
                targetContent shouldContain "CVE-2026-1234"
                targetContent shouldContain "CVE-2026-5678"
            } finally {
                sourceFile.delete()
                targetFile.delete()
            }
        }

        test("skip target that already contains the entry") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            val targetFile = Files.createTempFile("target", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)
                targetFile.writeText(TARGET_YAML.replace("CVE-2026-0001", "CVE-2026-1234"))

                val result =
                    CopyCommand().test(
                        "${sourceFile.absolutePath} ${targetFile.absolutePath} --vuln-id CVE-2026-1234",
                    )

                result.statusCode shouldBe 0
                result.stderr shouldContain "already exists"
            } finally {
                sourceFile.delete()
                targetFile.delete()
            }
        }

        test("error when vuln-id not found in source") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            val targetFile = Files.createTempFile("target", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)
                targetFile.writeText(TARGET_YAML)

                val result =
                    CopyCommand().test(
                        "${sourceFile.absolutePath} ${targetFile.absolutePath} --vuln-id CVE-9999-0000",
                    )

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "not found in source"
            } finally {
                sourceFile.delete()
                targetFile.delete()
            }
        }

        test("error when source file does not exist") {
            val targetFile = Files.createTempFile("target", ".vl.yaml").toFile()
            try {
                val result =
                    CopyCommand().test(
                        "/nonexistent/source.vl.yaml ${targetFile.absolutePath} --vuln-id CVE-2026-1234",
                    )

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "does not exist"
            } finally {
                targetFile.delete()
            }
        }

        test("error when target file does not exist") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)

                val result =
                    CopyCommand().test(
                        "${sourceFile.absolutePath} /nonexistent/target.vl.yaml --vuln-id CVE-2026-1234",
                    )

                result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                result.stderr shouldContain "does not exist"
            } finally {
                sourceFile.delete()
            }
        }

        test("copy adjusts releases to target latest published release") {
            val sourceFile = Files.createTempFile("source", ".vl.yaml").toFile()
            val targetFile = Files.createTempFile("target", ".vl.yaml").toFile()
            try {
                sourceFile.writeText(SOURCE_YAML)
                targetFile.writeText(TARGET_YAML)

                CopyCommand().test(
                    "${sourceFile.absolutePath} ${targetFile.absolutePath} --vuln-id CVE-2026-1234",
                )

                val targetContent = targetFile.readText()
                targetContent shouldContain "\"1.0.0\""
            } finally {
                sourceFile.delete()
                targetFile.delete()
            }
        }
    })
