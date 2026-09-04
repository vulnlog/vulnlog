// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import dev.vulnlog.lib.fixtures.openVexDocument
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

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
                    result.stderr shouldContain
                        "warning: releases without purls are not part of the document: '1.0.1'"
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

        context("nothing to write") {

            test("fails when no release declares purls") {
                withTempFile(content = vulnlogDocument()) { input ->
                    val result = OpenVexCommand().test("${input.absolutePath} -o -")

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: no statement applies"
                    result.stderr shouldContain "declare 'purls' on the releases"
                }
            }
        }
    })
