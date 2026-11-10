// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.fixtures.openVexDocument
import dev.vulnlog.lib.fixtures.openVexScopedDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

private fun documentOf(
    yaml: String,
    flags: String,
): String =
    withTempFile(content = yaml) { input ->
        CycloneDxVexCommand().test("${input.absolutePath} $flags -o -").stdout
    }

private fun seedDocument(
    yaml: String,
    flags: String,
    target: Path,
): String =
    withTempFile(content = yaml) { input ->
        CycloneDxVexCommand().test("${input.absolutePath} $flags -o ${target.toAbsolutePath()}")
        target.toFile().readText()
    }

class CycloneDxVexCommandTest :
    FunSpec({

        context("happy path") {

            test("writes the document for the release to stdout") {
                withTempFile(content = openVexDocument()) { input ->
                    val result = CycloneDxVexCommand().test("${input.absolutePath} --release 1.0.0 -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "\"bomFormat\": \"CycloneDX\""
                    result.stdout shouldContain "\"specVersion\": \"1.7\""
                    result.stdout shouldContain "\"purl\": \"pkg:maven/com.acme/acme-web-app@1.0.0\""
                    result.stdout shouldContain "\"ref\": \"pkg:npm/example-lib@2.3.0\""
                    result.stdout shouldContain "\"state\": \"not_affected\""
                    result.stderr shouldNotContain "warning:"
                }
            }

            test("-o writes the document to the given path") {
                withTempFile(content = openVexDocument()) { input ->
                    withTempDir(prefix = "cyclonedx-out") { outputDir ->
                        val target = outputDir.resolve("vex.cdx.json")

                        val result =
                            CycloneDxVexCommand().test(
                                "${input.absolutePath} --release 1.0.0 -o ${target.toAbsolutePath()}",
                            )

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Wrote: ${target.toAbsolutePath()}"
                        target.toFile().readText() shouldContain "\"version\": 1"
                    }
                }
            }
        }

        context("--release") {

            test("is required, because a document covers one release") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = CycloneDxVexCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldNotBe 0
                    result.stderr shouldContain "--release"
                    result.stdout shouldBe ""
                }
            }

            test("names an unpublished release to cover it") {
                val document = documentOf(openVexScopedDocument(), "--release 1.2.0")

                document shouldContain "\"purl\": \"pkg:docker/acme/web-app@1.2.0\""
                document shouldContain "\"id\": \"CVE-2026-3333\""
                document shouldContain "\"state\": \"in_triage\""
            }

            test("states every entry as it stands on the named release") {
                val document = documentOf(openVexScopedDocument(), "--release 1.1.0")

                document shouldContain "\"purl\": \"pkg:docker/acme/web-app@1.1.0\""
                document shouldNotContain "pkg:docker/acme/web-app@1.0.0"
                document shouldContain "\"state\": \"resolved\""
                document shouldNotContain "CVE-2026-3333"
            }

            test("an unknown release and an unknown tag are rejected together") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = CycloneDxVexCommand().test("${input.absolutePath} --release 9.9.9 --tag binary -o -")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Release not found: 9.9.9"
                    result.stderr shouldContain "Tag not found: binary"
                }
            }
        }

        context("--tag") {

            test("keeps only the vulnerabilities and release purls carrying the tag") {
                val document = documentOf(openVexScopedDocument(), "--release 1.0.0 --tag container")

                document shouldContain "\"purl\": \"pkg:docker/acme/web-app@1.0.0\""
                document shouldNotContain "pkg:maven/com.acme/acme-lib@1.0.0"
            }
        }

        context("--baseline") {

            test("keeps the serial number and counts the version up when the content changed") {
                withTempDir(prefix = "cyclonedx-baseline") { dir ->
                    val baseline = dir.resolve("vex.cdx.json")
                    val first = seedDocument(openVexScopedDocument(), "--release 1.0.0", baseline)
                    val serialNumber = Regex("\"serialNumber\": \"(urn:uuid:[^\"]+)\"").find(first)!!.groupValues[1]

                    // The narrower scope changes the content, so a revision is due.
                    val document =
                        documentOf(
                            openVexScopedDocument(),
                            "--release 1.0.0 --tag container --baseline ${baseline.toAbsolutePath()}",
                        )

                    document shouldContain "\"serialNumber\": \"$serialNumber\""
                    document shouldContain "\"version\": 2"
                    document shouldContain "\"name\": \"Vulnlog CLI\""
                }
            }

            test("writing back to an unchanged baseline reports it and leaves the bytes alone") {
                withTempDir(prefix = "cyclonedx-baseline") { dir ->
                    val baseline = dir.resolve("vex.cdx.json")
                    val first = seedDocument(openVexScopedDocument(), "--release 1.0.0", baseline)

                    val result =
                        withTempFile(content = openVexScopedDocument()) { input ->
                            CycloneDxVexCommand().test(
                                "${input.absolutePath} --release 1.0.0 --baseline ${baseline.toAbsolutePath()} " +
                                    "-o ${baseline.toAbsolutePath()}",
                            )
                        }

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "Unchanged: ${baseline.toAbsolutePath()}"
                    baseline.toFile().readText() shouldBe first
                }
            }

            test("a missing baseline is rejected") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result =
                        CycloneDxVexCommand().test(
                            "${input.absolutePath} --release 1.0.0 --baseline /does/not/exist.json -o -",
                        )

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "error: baseline '/does/not/exist.json' does not exist"
                    result.stderr shouldContain "hint: omit --baseline to issue a new document"
                }
            }

            test("a baseline that is not a CycloneDX document is rejected") {
                withTempDir(prefix = "cyclonedx-baseline") { dir ->
                    val foreign = dir.resolve("other.json")
                    foreign.toFile().writeText("""{"@context": "https://openvex.dev/ns/v0.2.0"}""")

                    val result =
                        withTempFile(content = openVexScopedDocument()) { input ->
                            CycloneDxVexCommand().test(
                                "${input.absolutePath} --release 1.0.0 --baseline ${foreign.toAbsolutePath()} -o -",
                            )
                        }

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain
                        "error: baseline '${foreign.toAbsolutePath()}' is not a CycloneDX document"
                    result.stderr shouldContain "hint: omit --baseline to issue a new document"
                    result.stdout shouldBe ""
                }
            }
        }

        context("nothing to write") {

            test("fails when the release declares no purls") {
                withTempFile(content = openVexDocument()) { input ->
                    val result = CycloneDxVexCommand().test("${input.absolutePath} --release 1.0.1 -o -")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: no vulnerability applies"
                    result.stderr shouldContain "hint: declare 'purls' on release '1.0.1'"
                    result.stdout shouldBe ""
                }
            }

            test("fails when the tag leaves no release purl") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = CycloneDxVexCommand().test("${input.absolutePath} --release 1.1.0 --tag library -o -")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "pass another --tag or omit it"
                }
            }
        }

        context("invalid input") {

            test("a blank project author is rejected as a finding, not as a crash") {
                val yaml = openVexDocument().replace("author: Acme Corp Security Team", "author: \" \"")

                withTempFile(content = yaml) { input ->
                    val result = CycloneDxVexCommand().test("${input.absolutePath} --release 1.0.0 -o -")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "Project 'author' must not be blank."
                    result.stdout shouldBe ""
                }
            }
        }
    })
