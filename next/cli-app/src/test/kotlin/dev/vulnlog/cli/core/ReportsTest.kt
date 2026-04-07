package dev.vulnlog.cli.core

import dev.vulnlog.cli.model.ReporterType
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ReportsTest :
    FunSpec({

        context("parseReporter") {
            test("parses dependency_check") {
                parseReporter("dependency_check") shouldBe ReporterType.DEPENDENCY_CHECK
            }

            test("parses github_advisory") {
                parseReporter("github_advisory") shouldBe ReporterType.GITHUB_SECURITY_ADVISORY
            }

            test("parses grype") {
                parseReporter("grype") shouldBe ReporterType.GRYPE
            }

            test("parses npm_audit") {
                parseReporter("npm_audit") shouldBe ReporterType.NPM_AUDIT
            }

            test("parses other") {
                parseReporter("other") shouldBe ReporterType.OTHER
            }

            test("parses rust_audit") {
                parseReporter("rust_audit") shouldBe ReporterType.RUST_AUDIT
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
            test("DEPENDENCY_CHECK canonical is dependency_check") {
                ReporterType.DEPENDENCY_CHECK.canonical() shouldBe "dependency_check"
            }

            test("GITHUB_SECURITY_ADVISORY canonical is github_advisory") {
                ReporterType.GITHUB_SECURITY_ADVISORY.canonical() shouldBe "github_advisory"
            }

            test("GRYPE canonical is grype") {
                ReporterType.GRYPE.canonical() shouldBe "grype"
            }

            test("NPM_AUDIT canonical is npm_audit") {
                ReporterType.NPM_AUDIT.canonical() shouldBe "npm_audit"
            }

            test("OTHER canonical is other") {
                ReporterType.OTHER.canonical() shouldBe "other"
            }

            test("RUST_AUDIT canonical is rust_audit") {
                ReporterType.RUST_AUDIT.canonical() shouldBe "rust_audit"
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
                        "dependency_check",
                        "github_advisory",
                        "grype",
                        "npm_audit",
                        "other",
                        "rust_audit",
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
