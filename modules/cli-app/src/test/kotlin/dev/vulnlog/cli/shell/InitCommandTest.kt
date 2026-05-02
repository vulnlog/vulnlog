// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

private const val REQUIRED_OPTIONS = "--organization acme --name widget --author alice"

class InitCommandTest :
    FunSpec({

        context("happy path") {

            test("writes YAML to stdout when no output is specified") {
                val result = InitCommand().test(REQUIRED_OPTIONS)

                result.statusCode shouldBe 0
                result.stdout shouldContain "schemaVersion:"
                result.stdout shouldContain "organization:"
                result.stdout shouldContain "acme"
            }

            test("writes YAML to stdout with explicit -o -") {
                val result = InitCommand().test("$REQUIRED_OPTIONS -o -")

                result.statusCode shouldBe 0
                result.stdout shouldContain "schemaVersion:"
                result.stdout shouldContain "acme"
            }

            test("does not print a status message when writing to stdout") {
                val result = InitCommand().test(REQUIRED_OPTIONS)

                result.statusCode shouldBe 0
                result.stdout shouldNotContain "Vulnlog file created at:"
            }

            test("writes YAML to a file when -o path is specified") {
                withTempFile(prefix = "vulnlog-init", suffix = ".yaml") { output ->
                    val result = InitCommand().test("$REQUIRED_OPTIONS -o ${output.absolutePath}")

                    result.statusCode shouldBe 0
                    result.stdout shouldContain "Vulnlog file created at:"
                    output.readText() shouldContain "acme"
                }
            }
        }

        context("argument validation") {

            test("fails when no options are provided") {
                val result = InitCommand().test("")

                result.statusCode shouldBe 1
                result.stderr shouldContain "missing"
            }

            test("fails when --organization is missing") {
                val result = InitCommand().test("--name widget --author alice")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--organization"
            }

            test("fails when --name is missing") {
                val result = InitCommand().test("--organization acme --author alice")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--name"
            }

            test("fails when --author is missing") {
                val result = InitCommand().test("--organization acme --name widget")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--author"
            }
        }
    })
