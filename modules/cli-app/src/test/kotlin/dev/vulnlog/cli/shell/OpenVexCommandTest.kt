// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.fixtures.openVexDocument
import dev.vulnlog.lib.fixtures.openVexScopedDocument
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.nio.file.Path

/** Runs the command against [yaml] and returns the document it wrote to stdout. */
private fun documentOf(
    yaml: String,
    flags: String = "",
): String =
    withTempFile(content = yaml) { input ->
        OpenVexCommand().test("${input.absolutePath} $flags -o -").stdout
    }

/** Writes an OpenVEX document for [yaml] to [target] and returns its text. */
private fun seedDocument(
    yaml: String,
    target: Path,
): String =
    withTempFile(content = yaml) { input ->
        OpenVexCommand().test("${input.absolutePath} -o ${target.toAbsolutePath()}")
        target.toFile().readText()
    }

class OpenVexCommandTest :
    FunSpec({

        context("happy path") {

            test("writes the document to stdout and warns about the release without purls") {
                withTempFile(content = openVexDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "\"@context\": \"https://openvex.dev/ns/v0.2.0\""
                    result.stdout shouldContain "\"@id\": \"pkg:maven/com.acme/acme-web-app@1.0.0\""
                    result.stdout shouldContain "\"status\": \"not_affected\""
                    result.stderr shouldContain "warning: releases without purls are not part of the document: '1.0.1'"
                }
            }

            test("-o writes the document to the given path") {
                withTempFile(content = openVexDocument()) { input ->
                    withTempDir(prefix = "openvex-out") { outputDir ->
                        val target = outputDir.resolve("vex.json")

                        val result = OpenVexCommand().test("${input.absolutePath} -o ${target.toAbsolutePath()}")

                        result.statusCode shouldBe 0
                        result.stderr shouldContain "Wrote: ${target.toAbsolutePath()}"
                        target.toFile().readText() shouldContain "\"version\": 1"
                    }
                }
            }
        }

        context("--as-of") {

            test("covers the named release and every earlier one") {
                val document = documentOf(openVexScopedDocument(), "--as-of 1.1.0")

                document shouldContain "\"@id\": \"pkg:docker/acme/web-app@1.0.0\""
                document shouldContain "\"@id\": \"pkg:docker/acme/web-app@1.1.0\""
                document shouldContain "\"status\": \"fixed\""
            }

            test("a later release drops out, and its fix still names the action") {
                val document = documentOf(openVexScopedDocument(), "--as-of 1.0.0")

                document shouldNotContain "pkg:docker/acme/web-app@1.1.0"
                document shouldNotContain "\"status\": \"fixed\""
                document shouldContain "\"action_statement\": \"Update to release 1.1.0."
            }

            test("an unknown release is rejected") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} --as-of 9.9.9 -o -")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Release not found: 9.9.9"
                }
            }
        }

        context("--tag") {

            test("keeps only the purls carrying the tag") {
                val document = documentOf(openVexScopedDocument(), "--tag container")

                document shouldContain "\"@id\": \"pkg:docker/acme/web-app@1.0.0\""
                document shouldNotContain "pkg:maven/com.acme/acme-lib@1.0.0"
            }

            test("names the releases the tag left without purls") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} --tag library -o -")

                    result.statusCode shouldBe 0
                    result.stderr shouldContain
                        "warning: releases without purls in scope are not part of the document: '1.1.0', '1.2.0'"
                }
            }

            test("an unknown tag is rejected") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} --tag binary -o -")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "Tag not found: binary"
                }
            }
        }

        context("--baseline") {

            test("keeps the identifier and counts the version up when the content changed") {
                withTempDir(prefix = "openvex-baseline") { dir ->
                    val baseline = dir.resolve("vex.json")
                    val first = seedDocument(openVexScopedDocument(), baseline)
                    val id = Regex("\"@id\": \"(https://vulnlog[^\"]+)\"").find(first)!!.groupValues[1]

                    // A narrower scope drops the library purl, so this run really does say something new.
                    val document =
                        documentOf(openVexScopedDocument(), "--tag container --baseline ${baseline.toAbsolutePath()}")

                    document shouldContain "\"@id\": \"$id\""
                    document shouldContain "\"version\": 2"
                    document shouldContain "\"last_updated\":"
                    document shouldNotContain "pkg:maven/com.acme/acme-lib@1.0.0"
                }
            }

            test("writing back to an unchanged baseline reports it and leaves the bytes alone") {
                withTempDir(prefix = "openvex-baseline") { dir ->
                    val baseline = dir.resolve("vex.json")
                    val first = seedDocument(openVexScopedDocument(), baseline)

                    val result =
                        withTempFile(content = openVexScopedDocument()) { input ->
                            OpenVexCommand().test(
                                "${input.absolutePath} --baseline ${baseline.toAbsolutePath()} " +
                                    "-o ${baseline.toAbsolutePath()}",
                            )
                        }

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "Unchanged: ${baseline.toAbsolutePath()}"
                    baseline.toFile().readText() shouldBe first
                }
            }

            test("an unchanged baseline written elsewhere keeps its bytes") {
                withTempDir(prefix = "openvex-baseline") { dir ->
                    val baseline = dir.resolve("vex.json")
                    val first = seedDocument(openVexScopedDocument(), baseline)

                    val document = documentOf(openVexScopedDocument(), "--baseline ${baseline.toAbsolutePath()}")

                    document shouldBe first
                }
            }

            test("a missing baseline is rejected") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} --baseline /does/not/exist.json -o -")

                    result.statusCode shouldBe ExitCode.INVALID_FLAG_VALUE.code
                    result.stderr shouldContain "error: baseline '/does/not/exist.json' does not exist"
                    result.stderr shouldContain "hint: omit --baseline to issue a new document"
                }
            }

            test("a baseline that is not an OpenVEX document issues a new one") {
                withTempDir(prefix = "openvex-baseline") { dir ->
                    val foreign = dir.resolve("other.json")
                    foreign.toFile().writeText("""{"bomFormat": "CycloneDX"}""")

                    val result =
                        withTempFile(content = openVexScopedDocument()) { input ->
                            OpenVexCommand().test("${input.absolutePath} --baseline ${foreign.toAbsolutePath()} -o -")
                        }

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "is not an OpenVEX document, issuing a new one"
                    result.stdout shouldContain "\"version\": 1"
                }
            }
        }

        context("nothing to write") {

            test("fails when no release declares purls") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: no statement applies"
                    result.stderr shouldContain "declare 'purls' on the releases"
                }
            }

            test("fails when the scope leaves no statement") {
                withTempFile(content = openVexScopedDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} --tag legacy -o -")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: no statement applies"
                    result.stderr shouldContain "no release purl in scope carries one of the requested tags"
                }
            }
        }
    })
