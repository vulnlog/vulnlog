// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.validation

import dev.vulnlog.lib.core.validation.ValidationConfig
import dev.vulnlog.lib.core.validation.ValidationOutcome
import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.model.finding.Rule
import dev.vulnlog.lib.parse.validation.ParsedVulnlogProject
import dev.vulnlog.lib.parse.validation.ValidVulnlogProject
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.ValidationRequest
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

private fun wrapperTask(): DefaultTask =
    ProjectBuilder
        .builder()
        .build()
        .tasks
        .register("wrapper", DefaultTask::class.java)
        .get()

private fun documentFile(content: String): FileInputOption.File {
    val file: Path = Files.createTempFile("vulnlog", ".vl.yaml")
    file.toFile().deleteOnExit()
    file.writeText(content)
    return FileInputOption.File(file)
}

private fun parse(
    content: String,
    request: ValidationRequest = ValidationRequest(),
): ParsedVulnlogProject = wrapperTask().parseInputOrFail(documentFile(content), request).project

private fun validate(
    content: String,
    request: ValidationRequest = ValidationRequest(),
): ValidVulnlogProject = validateOutcome(content, request).project

private fun validateOutcome(
    content: String,
    request: ValidationRequest = ValidationRequest(),
): ValidationOutcome.Ok<ValidVulnlogProject> = wrapperTask().validateInputOrFail(documentFile(content), request)

class ValidationGradleWrapperTest :
    FunSpec({

        context("parseFile") {

            test("accepts a document that reaches the DTO stage") {
                val document = ValidationDocuments.CLEAN

                val parsed = parse(document)

                parsed.inputDocument.content shouldBe document
            }

            test("accepts a document whose domain rules do not hold") {
                val parsed = parse(ValidationDocuments.DANGLING_RELEASE)

                parsed.validatedDto shouldNotBe null
            }

            test("fails the build on malformed YAML") {
                val error = shouldThrow<GradleException> { parse(ValidationDocuments.MALFORMED_YAML) }

                error.message shouldBe "Vulnlog validation failed."
            }

            test("fails the build on an unsupported schema version") {
                val error = shouldThrow<GradleException> { parse(ValidationDocuments.UNSUPPORTED_SCHEMA_VERSION) }

                error.message shouldBe "Vulnlog validation failed."
            }

            test("fails the build on an unknown property") {
                val error = shouldThrow<GradleException> { parse(ValidationDocuments.UNKNOWN_PROPERTY) }

                error.message shouldBe "Vulnlog validation failed."
            }
        }

        context("validateFile") {

            test("accepts a clean document and carries the domain model") {
                val valid = validate(ValidationDocuments.CLEAN)

                valid.vulnlogProjectFile.vulnerabilities shouldHaveSize 1
            }

            test("fails the build when the document cannot be mapped onto the domain model") {
                val error = shouldThrow<GradleException> { validate(ValidationDocuments.UNMAPPABLE_VULN_ID) }

                error.message shouldBe "Vulnlog validation failed."
            }

            test("fails the build when a domain rule is broken") {
                val error = shouldThrow<GradleException> { validate(ValidationDocuments.DANGLING_RELEASE) }

                error.message shouldBe "Vulnlog validation failed."
            }
        }

        context("warnings") {

            test("a DTO warning does not fail the build") {
                shouldNotThrowAny { validate(ValidationDocuments.DEPRECATED_VERDICT) }
            }

            test("a domain warning does not fail the build") {
                shouldNotThrowAny { validate(ValidationDocuments.ANALYZED_BEFORE_REPORTED) }
            }

            test("a domain warning fails the build in strict mode") {
                val strict = ValidationRequest(ValidationConfig(strict = true))

                val error =
                    shouldThrow<GradleException> { validate(ValidationDocuments.ANALYZED_BEFORE_REPORTED, strict) }

                error.message shouldBe "Vulnlog validation failed."
            }

            test("a warning is carried through to the caller") {
                val outcome = validateOutcome(ValidationDocuments.ANALYZED_BEFORE_REPORTED)

                outcome.findings shouldHaveSize 1
            }
        }

        context("informational findings") {

            test("are carried through without failing the build") {
                val outcome = validateOutcome(ValidationDocuments.UNREFERENCED_RELEASE)

                outcome.findings
                    .single()
                    .rule shouldBe Rule.UNREFERENCED_RELEASE_ID
            }
        }
    })
