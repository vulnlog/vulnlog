// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

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
        verdict: not affected
        justification: vulnerable code not in execute path

      - id: CVE-2026-5678
        releases: [ 2.0.0 ]
        description: XSS in other-lib
        packages: [ "pkg:npm/other-lib@1.0.0" ]
        reports:
          - reporter: trivy
        verdict: not affected
        justification: component not present
    """.trimIndent()

private val TARGET_YAML = vulnlogYaml(releaseId = "1.0.0", cveId = "CVE-2026-0001")

class CopyCommandTest :
    FunSpec({

        context("happy path") {

            test("copies a single entry to a single target") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                        val result =
                            CopyCommand().test(
                                "${source.absolutePath} ${target.absolutePath} --vuln-id CVE-2026-1234",
                            )

                        result.statusCode shouldBe 0
                        result.stdout shouldContain "Copied 'CVE-2026-1234'"

                        val content = target.readText()
                        content shouldContain "CVE-2026-1234"
                        content shouldContain "1.0.0"
                        content shouldNotContain "2.0.0"
                    }
                }
            }

            test("copies a single entry to multiple targets") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target1", content = TARGET_YAML) { target1 ->
                        withTempFile(prefix = "target2", content = TARGET_YAML) { target2 ->
                            val result =
                                CopyCommand().test(
                                    "${source.absolutePath} ${target1.absolutePath} ${target2.absolutePath} " +
                                        "--vuln-id CVE-2026-1234",
                                )

                            result.statusCode shouldBe 0
                            target1.readText() shouldContain "CVE-2026-1234"
                            target2.readText() shouldContain "CVE-2026-1234"
                        }
                    }
                }
            }

            test("copies multiple vuln-ids in one invocation") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                        val result =
                            CopyCommand().test(
                                "${source.absolutePath} ${target.absolutePath} " +
                                    "--vuln-id CVE-2026-1234 --vuln-id CVE-2026-5678",
                            )

                        result.statusCode shouldBe 0
                        val content = target.readText()
                        content shouldContain "CVE-2026-1234"
                        content shouldContain "CVE-2026-5678"
                    }
                }
            }

            test("rewrites copied entry's release to the target's latest published release") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                        CopyCommand().test(
                            "${source.absolutePath} ${target.absolutePath} --vuln-id CVE-2026-1234",
                        )

                        target.readText() shouldContain "\"1.0.0\""
                    }
                }
            }

            test("skips a target that already contains the entry") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    val targetWithEntry = TARGET_YAML.replace("CVE-2026-0001", "CVE-2026-1234")
                    withTempFile(prefix = "target", content = targetWithEntry) { target ->
                        val result =
                            CopyCommand().test(
                                "${source.absolutePath} ${target.absolutePath} --vuln-id CVE-2026-1234",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "already exists"
                    }
                }
            }
        }

        context("input validation") {

            test("fails when the source file does not exist") {
                withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                    val result =
                        CopyCommand().test(
                            "/nonexistent/source.vl.yaml ${target.absolutePath} --vuln-id CVE-2026-1234",
                        )

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "does not exist"
                }
            }

            test("fails when a target file does not exist") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    val result =
                        CopyCommand().test(
                            "${source.absolutePath} /nonexistent/target.vl.yaml --vuln-id CVE-2026-1234",
                        )

                    result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                    result.stderr shouldContain "does not exist"
                }
            }

            test("fails when the source path is a directory") {
                withTempDir(prefix = "source-dir") { sourceDir ->
                    withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                        val result =
                            CopyCommand().test(
                                "${sourceDir.toAbsolutePath()} ${target.absolutePath} --vuln-id CVE-2026-1234",
                            )

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                        result.stderr shouldContain "is a directory"
                    }
                }
            }

            test("fails when the source file name does not match the expected pattern") {
                withTempFile(prefix = "source", suffix = ".txt", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                        val result =
                            CopyCommand().test(
                                "${source.absolutePath} ${target.absolutePath} --vuln-id CVE-2026-1234",
                            )

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                        result.stderr shouldContain "File name must be"
                    }
                }
            }

            test("fails when a target file name does not match the expected pattern") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target", suffix = ".txt", content = TARGET_YAML) { target ->
                        val result =
                            CopyCommand().test(
                                "${source.absolutePath} ${target.absolutePath} --vuln-id CVE-2026-1234",
                            )

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                        result.stderr shouldContain "File name must be"
                    }
                }
            }
        }

        context("argument validation") {

            test("fails when the requested vuln-id is not in the source") {
                withTempFile(prefix = "source", content = SOURCE_YAML) { source ->
                    withTempFile(prefix = "target", content = TARGET_YAML) { target ->
                        val result =
                            CopyCommand().test(
                                "${source.absolutePath} ${target.absolutePath} --vuln-id CVE-9999-0000",
                            )

                        result.statusCode shouldBe ExitCode.GENERAL_ERROR.ordinal
                        result.stderr shouldContain "not found in source"
                    }
                }
            }
        }
    })
