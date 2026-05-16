// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import com.github.ajalt.clikt.testing.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.time.LocalDate

class AddVulnerabilitiesCommandTest :
    FunSpec({

        context("STDOUT output") {

            test("prints a list-item YAML entry for a minimal invocation") {
                val result = AddVulnerabilitiesCommand().test("--vuln-id CVE-2026-1234")

                result.statusCode shouldBe 0
                result.stdout shouldContain "  - id: \"CVE-2026-1234\""
                result.stdout shouldContain "releases: []"
                result.stdout shouldContain "packages: []"
                result.stdout shouldNotContain "verdict"
            }

            test("emits multiple releases, packages and tags") {
                val result =
                    AddVulnerabilitiesCommand().test(
                        "--vuln-id CVE-2026-1234 " +
                            "--release 1.0.0 --release 1.1.0 " +
                            "--package pkg:npm/example-lib@2.3.0 " +
                            "--package pkg:npm/other-lib@1.0.0 " +
                            "--tag frontend --tag backend",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "\"1.0.0\""
                result.stdout shouldContain "\"1.1.0\""
                result.stdout shouldContain "pkg:npm/example-lib@2.3.0"
                result.stdout shouldContain "pkg:npm/other-lib@1.0.0"
                result.stdout shouldContain "frontend"
                result.stdout shouldContain "backend"
            }

            test("--reporter adds a reports entry dated today") {
                val result =
                    AddVulnerabilitiesCommand().test(
                        "--vuln-id CVE-2026-1234 --reporter trivy",
                    )

                result.statusCode shouldBe 0
                result.stdout shouldContain "reporter: \"trivy\""
                result.stdout shouldContain "at: \"${LocalDate.now()}\""
            }
        }

        context("argument validation") {

            test("fails when --vuln-id is missing") {
                val result = AddVulnerabilitiesCommand().test("")

                result.statusCode shouldBe 1
                result.stderr shouldContain "--vuln-id"
            }
        }
    })
