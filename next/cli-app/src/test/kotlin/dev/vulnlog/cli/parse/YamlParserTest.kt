package dev.vulnlog.cli.parse

import dev.vulnlog.cli.model.ParseValidationVersion
import dev.vulnlog.cli.model.SchemaVersion
import dev.vulnlog.cli.model.Severity
import dev.vulnlog.cli.model.Verdict
import dev.vulnlog.cli.model.VexJustification
import dev.vulnlog.cli.model.VulnId
import dev.vulnlog.cli.result.ParseResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class YamlParserTest :
    FunSpec({

        val parser = YamlParser(createYamlMapper())

        context("schema version detection") {
            test("missing schemaVersion field returns an error") {
                val yaml =
                    """
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities: []
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Error>()
                    .error shouldContain "schemaVersion"
            }

            test("non-numeric schemaVersion returns an error") {
                val yaml =
                    """
                    schemaVersion: "abc"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities: []
                    """.trimIndent()

                parser.parse(yaml).shouldBeInstanceOf<ParseResult.Error>()
            }

            test("unsupported major version returns an error mentioning the version") {
                val yaml =
                    """
                    schemaVersion: "99"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities: []
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Error>()
                    .error shouldContain "99"
            }
        }

        context("v1 parsing") {
            test("minimal valid v1 file parses project fields") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities: []
                    """.trimIndent()

                val ok = parser.parse(yaml).shouldBeInstanceOf<ParseResult.Ok>()
                ok.validationVersion shouldBe ParseValidationVersion.V1
                ok.content.schemaVersion shouldBe SchemaVersion(1, 0)
                ok.content.project.organization shouldBe "acme"
                ok.content.project.name shouldBe "widget"
                ok.content.project.author shouldBe "alice"
            }

            test("v1.2 file parses with correct minor version") {
                val yaml =
                    """
                    schemaVersion: "1.2"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities: []
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.schemaVersion shouldBe SchemaVersion(1, 2)
            }

            test("release entries are parsed by id") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases:
                      - id: "v1.0"
                      - id: "v2.0"
                    vulnerabilities: []
                    """.trimIndent()

                val releases =
                    parser
                        .parse(yaml)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.releases
                releases shouldHaveSize 2
                releases[0].id.value shouldBe "v1.0"
                releases[1].id.value shouldBe "v2.0"
            }

            test("vulnerability with CVE id is parsed") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: CVE-2021-44228
                        releases: []
                        packages: []
                        reports:
                          - reporter: grype
                    """.trimIndent()

                val vulns =
                    parser
                        .parse(yaml)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities
                vulns shouldHaveSize 1
                vulns[0].id shouldBe VulnId.Cve("CVE-2021-44228")
            }

            test("vulnerability without verdict defaults to under_investigation") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: CVE-2021-1234
                        releases: []
                        packages: []
                        reports: []
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.UnderInvestigation
            }

            test("vulnerability with affected verdict and severity is parsed") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: CVE-2021-1234
                        releases: []
                        packages: []
                        reports: []
                        verdict: affected
                        severity: high
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.Affected(Severity.HIGH)
            }

            test("vulnerability with not_affected verdict and justification is parsed") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: CVE-2021-1234
                        releases: []
                        packages: []
                        reports: []
                        verdict: not_affected
                        justification: component_not_present
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe
                    Verdict.NotAffected(VexJustification.COMPONENT_NOT_PRESENT)
            }

            test("vulnerability with risk_acceptable verdict and severity is parsed") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: CVE-2021-1234
                        releases: []
                        packages: []
                        reports: []
                        verdict: risk_acceptable
                        severity: medium
                    """.trimIndent()

                parser
                    .parse(yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.RiskAcceptable(Severity.MEDIUM)
            }

            test("vulnerability with unrecognized id returns a parse error") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: UNKNOWN-2021-0001
                        releases: []
                        packages: []
                        reports: []
                    """.trimIndent()

                parser.parse(yaml).shouldBeInstanceOf<ParseResult.Error>()
            }

            test("vulnerability with maven package purl is parsed") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project:
                      organization: acme
                      name: widget
                      author: alice
                    releases: []
                    vulnerabilities:
                      - id: CVE-2021-44228
                        releases: []
                        packages:
                          - "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1"
                        reports: []
                    """.trimIndent()

                val vulns =
                    parser
                        .parse(yaml)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities
                vulns[0].packages shouldHaveSize 1
            }

            test("project field of wrong type returns a parse error") {
                val yaml =
                    """
                    schemaVersion: "1"
                    project: "not an object"
                    releases: []
                    vulnerabilities: []
                    """.trimIndent()

                parser.parse(yaml).shouldBeInstanceOf<ParseResult.Error>()
            }
        }
    })
