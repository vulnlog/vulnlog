// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Verdict
import dev.vulnlog.lib.model.VexJustification
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.VulnlogFileRaw
import dev.vulnlog.lib.model.validation.FailureLocation
import dev.vulnlog.lib.result.ParseResult
import dev.vulnlog.lib.result.YamlParseDtoResult
import dev.vulnlog.lib.result.YamlParseResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf

private val minimalV1 =
    VulnlogFileRaw(
        """
        schemaVersion: "1"
        project:
          organization: acme
          name: widget
          author: alice
        releases: []
        vulnerabilities: []
        """.trimIndent(),
    )

private fun validYamlOf(raw: VulnlogFileRaw): YamlParseResult.Valid =
    parseToYaml(raw).shouldBeInstanceOf<YamlParseResult.Valid>()

class VulnlogParserTest :
    FunSpec({

        val mapper = createYamlMapper()

        context("schema version detection") {
            test("missing schemaVersion field returns an error") {
                val yaml =
                    VulnlogFileRaw(
                        """
                        project:
                          organization: acme
                          name: widget
                          author: alice
                        releases: []
                        vulnerabilities: []
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Error>()
                    .failures
                    .single()
                    .message shouldContain "schemaVersion"
            }

            test("non-numeric schemaVersion returns an error") {
                val yaml =
                    VulnlogFileRaw(
                        """
                        schemaVersion: "abc"
                        project:
                          organization: acme
                          name: widget
                          author: alice
                        releases: []
                        vulnerabilities: []
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml).shouldBeInstanceOf<ParseResult.Error>()
            }

            test("unsupported major version returns an error mentioning the version") {
                val yaml =
                    VulnlogFileRaw(
                        """
                        schemaVersion: "99"
                        project:
                          organization: acme
                          name: widget
                          author: alice
                        releases: []
                        vulnerabilities: []
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Error>()
                    .failures
                    .single()
                    .message shouldContain "99"
            }
        }

        context("v1 parsing") {
            test("minimal valid v1 file parses project fields") {
                val ok = parseVulnlogFile(mapper, minimalV1).shouldBeInstanceOf<ParseResult.Ok>()
                ok.validationVersion shouldBe ParseValidationVersion.V1
                ok.content.schemaVersion shouldBe SchemaVersion(1, 0)
                ok.content.project.organization shouldBe "acme"
                ok.content.project.name shouldBe "widget"
                ok.content.project.author shouldBe "alice"
            }

            test("v1.2 file parses with correct minor version") {
                val yaml =
                    VulnlogFileRaw(
                        """
                        schemaVersion: "1.2"
                        project:
                          organization: acme
                          name: widget
                          author: alice
                        releases: []
                        vulnerabilities: []
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.schemaVersion shouldBe SchemaVersion(1, 2)
            }

            test("release entries are parsed by id") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                val releases =
                    parseVulnlogFile(mapper, yaml)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.releases
                releases shouldHaveSize 2
                releases[0].id.value shouldBe "v1.0"
                releases[1].id.value shouldBe "v2.0"
            }

            test("vulnerability with CVE id is parsed") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                val vulns =
                    parseVulnlogFile(mapper, yaml)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities
                vulns shouldHaveSize 1
                vulns[0].id shouldBe VulnId.Cve("CVE-2021-44228")
            }

            test("vulnerability without verdict defaults to under_investigation") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.UnderInvestigation
            }

            test("vulnerability with affected verdict and severity is parsed") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.Affected(Severity.HIGH)
            }

            test("vulnerability with not affected verdict and justification is parsed") {
                val yaml =
                    VulnlogFileRaw(
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
                            verdict: not affected
                            justification: component not present
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe
                    Verdict.NotAffected(VexJustification.COMPONENT_NOT_PRESENT)
            }

            test("vulnerability with risk acceptable verdict and severity is parsed") {
                val yaml =
                    VulnlogFileRaw(
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
                            verdict: risk acceptable
                            severity: medium
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Ok>()
                    .content.vulnerabilities[0]
                    .verdict shouldBe Verdict.RiskAcceptable(Severity.MEDIUM)
            }

            test("vulnerability with unrecognized id returns a parse error") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml).shouldBeInstanceOf<ParseResult.Error>()
            }

            test("vulnerability with maven package purl is parsed") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                val vulns =
                    parseVulnlogFile(mapper, yaml)
                        .shouldBeInstanceOf<ParseResult.Ok>()
                        .content.vulnerabilities
                vulns[0].packages shouldHaveSize 1
            }

            test("project field of wrong type returns a parse error") {
                val yaml =
                    VulnlogFileRaw(
                        """
                        schemaVersion: "1"
                        project: "not an object"
                        releases: []
                        vulnerabilities: []
                        """.trimIndent(),
                    )

                parseVulnlogFile(mapper, yaml).shouldBeInstanceOf<ParseResult.Error>()
            }
        }

        context("YAML syntax step") {
            test("well-formed YAML returns the root node and schema version") {
                val valid = validYamlOf(minimalV1)
                valid.schemaVersion shouldBe SchemaVersion(1, 0)
            }

            test("syntax error returns the 1-based failure location") {
                val yaml = VulnlogFileRaw("schemaVersion: [unclosed")

                val invalid = parseToYaml(yaml).shouldBeInstanceOf<YamlParseResult.Invalid>()
                invalid.location shouldNotBe null
                invalid.location!!.line shouldBe 1
            }

            test("empty input is missing the schemaVersion") {
                val invalid = parseToYaml(VulnlogFileRaw("")).shouldBeInstanceOf<YamlParseResult.Invalid>()
                invalid.errorMessage shouldBe "Missing or invalid schemaVersion"
                invalid.location shouldBe null
            }

            test("scalar root is missing the schemaVersion") {
                parseToYaml(VulnlogFileRaw("just text"))
                    .shouldBeInstanceOf<YamlParseResult.Invalid>()
                    .errorMessage shouldBe "Missing or invalid schemaVersion"
            }

            test("unquoted numeric schemaVersion is accepted") {
                val yaml = VulnlogFileRaw(minimalV1.content.replace("schemaVersion: \"1\"", "schemaVersion: 1"))

                validYamlOf(yaml).schemaVersion shouldBe SchemaVersion(1, 0)
            }

            test("duplicate keys are tolerated") {
                val yaml = VulnlogFileRaw(minimalV1.content + "\nreleases: []")

                parseToYaml(yaml).shouldBeInstanceOf<YamlParseResult.Valid>()
            }

            test("invalid schemaVersion value points at its node") {
                val yaml = VulnlogFileRaw(minimalV1.content.replace("schemaVersion: \"1\"", "schemaVersion: \"abc\""))

                val invalid = parseToYaml(yaml).shouldBeInstanceOf<YamlParseResult.Invalid>()
                invalid.errorMessage shouldBe "Missing or invalid schemaVersion"
                invalid.location shouldNotBe null
                invalid.location!!.line shouldBe 1
            }
        }

        context("DTO step") {
            test("unsupported major version returns invalid") {
                val yaml = VulnlogFileRaw(minimalV1.content.replace("schemaVersion: \"1\"", "schemaVersion: \"99\""))

                val invalid =
                    parseToVulnlogDto(mapper, validYamlOf(yaml))
                        .shouldBeInstanceOf<YamlParseDtoResult.Invalid>()
                invalid.message shouldContain "99"
                invalid.message shouldContain "Try updating vulnlog"
            }

            test("unknown property is rejected with its name and location") {
                val yaml = VulnlogFileRaw(minimalV1.content + "\nbogus: true")

                val invalid =
                    parseToVulnlogDto(mapper, validYamlOf(yaml))
                        .shouldBeInstanceOf<YamlParseDtoResult.Invalid>()
                invalid.message shouldContain "bogus"
                invalid.location shouldBe FailureLocation(8, 8)
            }

            test("wrong field type returns a YAML parse error with location") {
                val yaml =
                    VulnlogFileRaw(
                        """
                        schemaVersion: "1"
                        project: "not an object"
                        releases: []
                        vulnerabilities: []
                        """.trimIndent(),
                    )

                val invalid =
                    parseToVulnlogDto(mapper, validYamlOf(yaml))
                        .shouldBeInstanceOf<YamlParseDtoResult.Invalid>()
                invalid.message shouldStartWith "YAML parse error:"
                invalid.location shouldNotBe null
                invalid.location!!.line shouldBe 2
            }

            test("valid input maps to the v1 DTO") {
                val valid =
                    parseToVulnlogDto(mapper, validYamlOf(minimalV1))
                        .shouldBeInstanceOf<YamlParseDtoResult.Valid>()
                valid.validationVersion shouldBe ParseValidationVersion.V1
                valid.schemaVersion shouldBe SchemaVersion(1, 0)
            }
        }

        context("domain step") {
            test("domain mapping failure returns a parser error") {
                val yaml =
                    VulnlogFileRaw(
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
                        """.trimIndent(),
                    )

                val failure =
                    parseVulnlogFile(mapper, yaml)
                        .shouldBeInstanceOf<ParseResult.Error>()
                        .failures
                        .single()
                failure.path shouldBe "vulnerabilities[UNKNOWN-2021-0001].id"
                failure.message shouldContain "UNKNOWN-2021-0001"
            }

            test("success carries every parse artifact") {
                val ok = parseVulnlogFile(mapper, minimalV1).shouldBeInstanceOf<ParseResult.Ok>()

                ok.rawContent shouldBe minimalV1
                ok.dto.schemaVersion shouldBe "1"
                scalarValueOf(ok.rootNode, "schemaVersion") shouldBe "1"
            }
        }

        context("full pipeline") {
            test("syntax failure carries a location") {
                parseVulnlogFile(mapper, VulnlogFileRaw("schemaVersion: [unclosed"))
                    .shouldBeInstanceOf<ParseResult.Error>()
                    .failures
                    .single()
                    .location shouldNotBe null
            }

            test("DTO failure points at the offending node") {
                val yaml = VulnlogFileRaw(minimalV1.content + "\nbogus: true")

                parseVulnlogFile(mapper, yaml)
                    .shouldBeInstanceOf<ParseResult.Error>()
                    .failures
                    .single()
                    .location shouldBe FailureLocation(8, 8)
            }

            test("domain mapping failures point at the offending entry") {
                val yaml =
                    VulnlogFileRaw(
                        minimalV1.content.replace(
                            "vulnerabilities: []",
                            """
                            vulnerabilities:
                              - id: UNKNOWN-2021-0001
                                releases: []
                                packages: []
                                reports: []
                            """.trimIndent(),
                        ),
                    )

                val failure =
                    parseVulnlogFile(mapper, yaml)
                        .shouldBeInstanceOf<ParseResult.Error>()
                        .failures
                        .single()
                failure.path shouldBe "vulnerabilities[UNKNOWN-2021-0001].id"
                failure.location shouldBe FailureLocation(8, 9)
            }

            test("every domain mapping problem is reported at once") {
                val yaml =
                    VulnlogFileRaw(
                        minimalV1.content.replace(
                            "vulnerabilities: []",
                            """
                            vulnerabilities:
                              - id: UNKNOWN-2021-0001
                                releases: []
                                packages: []
                                reports: []
                              - id: CVE-2021-1234
                                releases: []
                                packages: []
                                reports: []
                                verdict: bogus
                            """.trimIndent(),
                        ),
                    )

                val failures =
                    parseVulnlogFile(mapper, yaml)
                        .shouldBeInstanceOf<ParseResult.Error>()
                        .failures
                failures.map { it.path } shouldBe
                    listOf(
                        "vulnerabilities[UNKNOWN-2021-0001].id",
                        "vulnerabilities[CVE-2021-1234].verdict",
                    )
                failures.map { it.location?.line } shouldBe listOf(8, 16)
            }

            test("unquoted numeric schemaVersion parses end-to-end") {
                val yaml = VulnlogFileRaw(minimalV1.content.replace("schemaVersion: \"1\"", "schemaVersion: 1"))

                parseVulnlogFile(mapper, yaml).shouldBeInstanceOf<ParseResult.Ok>()
            }
        }
    })
