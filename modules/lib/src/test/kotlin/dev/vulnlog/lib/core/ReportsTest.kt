// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ReporterType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReportsTest :
    FunSpec({

        context("parseReporter") {
            test("parses dependency-check") {
                parseReporter("dependency-check") shouldBe ReporterType.DEPENDENCY_CHECK
            }

            test("parses github-advisory") {
                parseReporter("github-advisory") shouldBe ReporterType.GITHUB_SECURITY_ADVISORY
            }

            test("parses grype") {
                parseReporter("grype") shouldBe ReporterType.GRYPE
            }

            test("parses npm-audit") {
                parseReporter("npm-audit") shouldBe ReporterType.NPM_AUDIT
            }

            test("parses other") {
                parseReporter("other") shouldBe ReporterType.OTHER
            }

            test("parses cargo-audit") {
                parseReporter("cargo-audit") shouldBe ReporterType.CARGO_AUDIT
            }

            test("parses semgrep") {
                parseReporter("semgrep") shouldBe ReporterType.SEMGREP
            }

            test("parses snyk") {
                parseReporter("snyk") shouldBe ReporterType.SNYK
            }

            test("parses trivy") {
                parseReporter("trivy") shouldBe ReporterType.TRIVY
            }

            test("throws on unknown reporter") {
                shouldThrow<IllegalArgumentException> {
                    parseReporter("unknown_tool")
                }
            }
        }

        context("canonical") {
            test("DEPENDENCY_CHECK canonical is dependency-check") {
                ReporterType.DEPENDENCY_CHECK.canonical() shouldBe "dependency-check"
            }

            test("GITHUB_SECURITY_ADVISORY canonical is github-advisory") {
                ReporterType.GITHUB_SECURITY_ADVISORY.canonical() shouldBe "github-advisory"
            }

            test("GRYPE canonical is grype") {
                ReporterType.GRYPE.canonical() shouldBe "grype"
            }

            test("NPM_AUDIT canonical is npm-audit") {
                ReporterType.NPM_AUDIT.canonical() shouldBe "npm-audit"
            }

            test("OTHER canonical is other") {
                ReporterType.OTHER.canonical() shouldBe "other"
            }

            test("cargo-audit canonical is cargo-audit") {
                ReporterType.CARGO_AUDIT.canonical() shouldBe "cargo-audit"
            }

            test("SEMGREP canonical is semgrep") {
                ReporterType.SEMGREP.canonical() shouldBe "semgrep"
            }

            test("SNYK canonical is snyk") {
                ReporterType.SNYK.canonical() shouldBe "snyk"
            }

            test("TRIVY canonical is trivy") {
                ReporterType.TRIVY.canonical() shouldBe "trivy"
            }

            test("parseReporter and canonical are inverse") {
                val allCanonicals =
                    listOf(
                        "dependency-check",
                        "github-advisory",
                        "grype",
                        "npm-audit",
                        "other",
                        "cargo-audit",
                        "semgrep",
                        "snyk",
                        "trivy",
                    )
                allCanonicals.forEach { canonical ->
                    parseReporter(canonical).canonical() shouldBe canonical
                }
            }
        }
    })
