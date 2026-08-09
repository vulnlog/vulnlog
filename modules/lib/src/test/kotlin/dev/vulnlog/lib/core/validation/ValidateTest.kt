// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.finding.Rule
import dev.vulnlog.lib.model.finding.errors
import dev.vulnlog.lib.parse.dto.VulnlogFileV1Dto
import dev.vulnlog.lib.parse.validation.ParsedVulnlogProject
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.InputDocument
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

private const val TEST_FILE_NAME = "test.vl.yaml"

private fun document(content: String) = InputDocument(content, TEST_FILE_NAME)

private val STRICT = ValidationConfig(strict = true)

class ValidateTest :
    FunSpec({

        context("parseDocument") {

            test("a clean document yields the parsed project without findings") {
                val input = document(ValidationDocuments.CLEAN)

                val outcome = parseDocument(input)

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ParsedVulnlogProject>>()
                ok.findings.shouldBeEmpty()
                ok.project.inputDocument shouldBe input
            }

            test("the document is carried through unchanged") {
                val outcome = parseDocument(document(ValidationDocuments.CLEAN))

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ParsedVulnlogProject>>()
                ok.project.validatedDto
                    .shouldBeInstanceOf<VulnlogFileV1Dto>()
                    .vulnerabilities shouldHaveSize 1
            }

            test("a document whose domain rules do not hold still parses") {
                val outcome = parseDocument(document(ValidationDocuments.DANGLING_RELEASE))

                outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ParsedVulnlogProject>>()
            }

            test("malformed YAML stops the run with a problem") {
                val outcome = parseDocument(document(ValidationDocuments.MALFORMED_YAML))

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Unreadable>()
                stopped.problems shouldHaveSize 1
                stopped.findings.shouldBeEmpty()
            }

            test("an unsupported schema version stops the run and names the version") {
                val outcome = parseDocument(document(ValidationDocuments.UNSUPPORTED_SCHEMA_VERSION))

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Unreadable>()
                stopped.problems.single().message shouldContain "Unsupported schema version '99'"
            }

            test("an unknown property stops the run and names the property") {
                val outcome = parseDocument(document(ValidationDocuments.UNKNOWN_PROPERTY))

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Unreadable>()
                stopped.problems.single().message shouldContain "Unknown property 'bogus'"
            }
        }

        context("validateDocument") {

            test("a clean document yields the domain model without findings") {
                val outcome = validateDocument(document(ValidationDocuments.CLEAN))

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
                ok.findings.shouldBeEmpty()
                ok.project.vulnlogProjectFile.vulnerabilities shouldHaveSize 1
            }

            test("a value without a domain representation stops the run") {
                val outcome = validateDocument(document(ValidationDocuments.UNMAPPABLE_VULN_ID))

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Unreadable>()
                stopped.problems
                    .single()
                    .path shouldBe "vulnerabilities[UNKNOWN-2026-1234].id"
            }

            test("a located problem carries its source position") {
                val outcome = validateDocument(document(ValidationDocuments.UNMAPPABLE_VULN_ID))

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Unreadable>()
                stopped.problems.single().location shouldNotBe null
            }
        }

        context("informational findings") {

            test("do not stop the run and are carried to the caller") {
                val outcome = validateDocument(document(ValidationDocuments.UNREFERENCED_RELEASE))

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
                with(ok.findings.single()) {
                    severity shouldBe FindingSeverity.INFO
                    rule shouldBe Rule.UNREFERENCED_RELEASE_ID
                }
            }

            test("do not stop the run in strict mode either") {
                val outcome = validateDocument(document(ValidationDocuments.UNREFERENCED_RELEASE), STRICT)

                outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
            }
        }

        context("warnings") {

            test("a domain warning does not stop the run") {
                val outcome = validateDocument(document(ValidationDocuments.ANALYZED_BEFORE_REPORTED))

                val ok = outcome.shouldBeInstanceOf<ValidationOutcome.Ok<ValidVulnlogProject>>()
                with(ok.findings.single()) {
                    severity shouldBe FindingSeverity.WARNING
                    rule shouldBe Rule.ANALYZED_BEFORE_REPORTED
                }
            }

            test("a domain warning stops the run in strict mode") {
                val outcome = validateDocument(document(ValidationDocuments.ANALYZED_BEFORE_REPORTED), STRICT)

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Rejected>()
                stopped.findings shouldHaveSize 1
            }
        }

        context("errors") {

            test("a broken domain rule stops the run, carrying the finding") {
                val outcome = validateDocument(document(ValidationDocuments.DANGLING_RELEASE))

                val stopped = outcome.shouldBeInstanceOf<ValidationOutcome.Stopped.Rejected>()
                with(stopped.findings.errors.single()) {
                    severity shouldBe FindingSeverity.ERROR
                    rule shouldBe Rule.DANGLING_RELEASE_REFERENCE
                }
            }
        }
    })
