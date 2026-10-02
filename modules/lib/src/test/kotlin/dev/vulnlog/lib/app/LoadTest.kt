// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.document.validation.ParsedVulnlogProject
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.finding.Rule
import dev.vulnlog.lib.finding.errors
import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.fixtures.vulnlogDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

private const val TEST_FILE_NAME = "test.vl.yaml"

private fun document(content: String) = InputDocument(content, TEST_FILE_NAME)

private val STRICT = ValidationConfig(strict = true)

private fun ValidationOutcome<*>.kind(): String =
    when (this) {
        is ValidationOutcome.Ok -> "ok"
        is InputRejected.Unparsable -> "unparsable"
        is InputRejected.Invalid -> "invalid"
    }

class LoadTest :
    FunSpec({

        context("parseDocument") {

            test("a clean document yields its DTO without findings and keeps the document") {
                val input = document(ValidationDocuments.CLEAN)

                val outcome = parseDocument(input)

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ParsedVulnlogProject>>()
                ok.findings.shouldBeEmpty()
                ok.project.inputDocument shouldBe input
                val dto = ok.project.validatedDto.shouldBeInstanceOf<VulnlogFileV1Dto>()
                dto.vulnerabilities shouldHaveSize 1
            }

            test("a document whose domain rules do not hold still parses, so it can be formatted") {
                val outcome = parseDocument(document(ValidationDocuments.DANGLING_RELEASE))

                outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ParsedVulnlogProject>>()
            }

            test("malformed YAML stops the run with one problem and no findings") {
                val outcome = parseDocument(document(ValidationDocuments.MALFORMED_YAML))

                val stopped = outcome.shouldBeInstanceOf<InputRejected.Unparsable>()
                stopped.problems shouldHaveSize 1
                stopped.findings.shouldBeEmpty()
            }

            test("an unsupported schema version, an unknown property or no data stops the run, naming the cause") {
                val contents =
                    listOf(
                        ValidationDocuments.UNSUPPORTED_SCHEMA_VERSION,
                        ValidationDocuments.UNKNOWN_PROPERTY,
                        "",
                        "  \n",
                        "---\n",
                        "# only a comment\n",
                    )

                val outcomes = contents.map { parseDocument(document(it)) }

                val problems = outcomes.map { it.shouldBeInstanceOf<InputRejected.Unparsable>().problems }
                problems.map { it.single().message } shouldBe
                    listOf(
                        "Unsupported schema version '99'. Try updating vulnlog.",
                        "Unknown property 'bogus'. Try updating vulnlog.",
                    ) + List(4) { "Empty YAML document" }
            }
        }

        context("validateDocument") {

            test("a clean document yields the domain model without findings") {
                val outcome = validateDocument(document(ValidationDocuments.CLEAN))

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
                ok.findings.shouldBeEmpty()
                ok.project.vulnlogProjectFile.vulnerabilities shouldHaveSize 1
            }

            test("a stop in a parse stage is passed on") {
                val outcome = validateDocument(document(ValidationDocuments.MALFORMED_YAML))

                outcome.shouldBeInstanceOf<InputRejected.Unparsable>()
            }

            test("a value without a domain representation stops the run, located by path and position") {
                val outcome = validateDocument(document(ValidationDocuments.UNMAPPABLE_VULN_ID))

                val problem = outcome.shouldBeInstanceOf<InputRejected.Unparsable>().problems.single()
                problem.path shouldBe "vulnerabilities[UNKNOWN-2026-1234].id"
                problem.location shouldNotBe null
            }

            test("a stop in the domain stage keeps the warnings of the DTO stage") {
                val content =
                    vulnlogDocument(
                        vulnId = "UNKNOWN-2026-1234",
                        verdictBlock = "    verdict: risk acceptable\n    severity: low",
                    )

                val outcome = validateDocument(document(content))

                val stopped = outcome.shouldBeInstanceOf<InputRejected.Unparsable>()
                stopped.findings.map { it.rule } shouldBe listOf(Rule.DEPRECATED_VERDICT)
            }
        }

        context("findings") {

            test("an info never stops the run, in strict mode neither") {
                val configs = listOf(ValidationConfig(), STRICT)

                val outcomes = configs.map { validateDocument(document(ValidationDocuments.UNREFERENCED_RELEASE), it) }

                outcomes.map { it.kind() to it.findings.single().rule } shouldBe
                    List(2) { "ok" to Rule.UNREFERENCED_RELEASE_ID }
            }

            test("a warning of the DTO or the domain rules stops the run only in strict mode") {
                val dtoWarning = document(ValidationDocuments.DEPRECATED_VERDICT)
                val domainWarning = document(ValidationDocuments.ANALYZED_BEFORE_REPORTED)

                val outcomes =
                    listOf(
                        parseDocument(dtoWarning),
                        parseDocument(dtoWarning, STRICT),
                        validateDocument(domainWarning),
                        validateDocument(domainWarning, STRICT),
                    )

                outcomes.map { it.kind() to it.findings.single().rule } shouldBe
                    listOf(
                        "ok" to Rule.DEPRECATED_VERDICT,
                        "invalid" to Rule.DEPRECATED_VERDICT,
                        "ok" to Rule.ANALYZED_BEFORE_REPORTED,
                        "invalid" to Rule.ANALYZED_BEFORE_REPORTED,
                    )
            }

            test("an error stops the run, carrying the finding") {
                val outcome = validateDocument(document(ValidationDocuments.DANGLING_RELEASE))

                val invalid = outcome.shouldBeInstanceOf<InputRejected.Invalid>()
                val error = invalid.findings.errors.single()
                error.severity to error.rule shouldBe (FindingSeverity.ERROR to Rule.DANGLING_RELEASE_REFERENCE)
            }
        }
    })
