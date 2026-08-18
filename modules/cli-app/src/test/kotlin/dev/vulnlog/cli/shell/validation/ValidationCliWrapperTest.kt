// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.validation

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.testing.test
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.withTempFile
import dev.vulnlog.lib.core.validation.ValidationConfig
import dev.vulnlog.lib.fixtures.ValidationDocuments
import dev.vulnlog.lib.model.finding.ALL_SEVERITIES
import dev.vulnlog.lib.shell.FileInputOption
import dev.vulnlog.lib.shell.ValidationRequest
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import java.io.File

/**
 * Drives one validation stage and echoes what it produced, so the assertions can look at the exit
 * code and the messages the wrapper wrote rather than at a whole command's behaviour.
 */
private class WrapperCommand(
    private val file: File,
    private val request: ValidationRequest,
    private val stopAfterParse: Boolean,
) : CliktCommand(name = "wrapper") {
    override fun run() {
        val input = FileInputOption.File(file.toPath())
        if (stopAfterParse) {
            parseInputOrFail(input, request)
            echo("parsed ok", err = true)
        } else {
            val valid = validateInputOrFail(input, request)
            echo("validated ${valid.project.vulnlogProjectFile.vulnerabilities.size}", err = true)
        }
    }
}

private fun parse(
    file: File,
    request: ValidationRequest = ValidationRequest(),
) = WrapperCommand(file, request, stopAfterParse = true).test("")

private fun validate(
    file: File,
    request: ValidationRequest = ValidationRequest(),
) = WrapperCommand(file, request, stopAfterParse = false).test("")

class ValidationCliWrapperTest :
    FunSpec({

        context("parseFile") {

            test("accepts a document that reaches the DTO stage") {
                withTempFile(content = ValidationDocuments.CLEAN) { file ->
                    val result = parse(file)

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "parsed ok"
                }
            }

            test("accepts a document whose domain rules do not hold") {
                withTempFile(content = ValidationDocuments.DANGLING_RELEASE) { file ->
                    val result = parse(file)

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "parsed ok"
                }
            }

            test("rejects malformed YAML with a validation error") {
                withTempFile(content = ValidationDocuments.MALFORMED_YAML) { file ->
                    val result = parse(file)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: ${file.name}: "
                }
            }

            test("rejects an unsupported schema version and points at it") {
                withTempFile(content = ValidationDocuments.UNSUPPORTED_SCHEMA_VERSION) { file ->
                    val result = parse(file)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "Unsupported schema version '99'"
                }
            }

            test("rejects an unknown property and names it") {
                withTempFile(content = ValidationDocuments.UNKNOWN_PROPERTY) { file ->
                    val result = parse(file)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "Unknown property 'bogus'"
                }
            }
        }

        context("validateFile") {

            test("accepts a clean document") {
                withTempFile(content = ValidationDocuments.CLEAN) { file ->
                    val result = validate(file)

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "validated 1"
                }
            }

            test("rejects a document that cannot be mapped onto the domain model") {
                withTempFile(content = ValidationDocuments.UNMAPPABLE_VULN_ID) { file ->
                    val result = validate(file)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "error: ${file.name}: "
                }
            }

            test("rejects a document that breaks a domain rule") {
                withTempFile(content = ValidationDocuments.DANGLING_RELEASE) { file ->
                    val result = validate(file)

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "References undefined release '9.9.9'"
                }
            }

            test("points a failing user at the help channel") {
                withTempFile(content = ValidationDocuments.DANGLING_RELEASE) { file ->
                    val result = validate(file)

                    result.stderr shouldContain "ask for help at"
                }
            }
        }

        context("warnings") {

            test("a DTO warning does not stop the command") {
                withTempFile(content = ValidationDocuments.DEPRECATED_VERDICT) { file ->
                    val result = validate(file)

                    result.statusCode shouldBe 0
                }
            }

            test("a domain warning does not stop the command") {
                withTempFile(content = ValidationDocuments.ANALYZED_BEFORE_REPORTED) { file ->
                    val result = validate(file)

                    result.statusCode shouldBe 0
                }
            }

            test("a domain warning stops the command in strict mode") {
                withTempFile(content = ValidationDocuments.ANALYZED_BEFORE_REPORTED) { file ->
                    val result = validate(file, ValidationRequest(ValidationConfig(strict = true), ALL_SEVERITIES))

                    result.statusCode shouldBe ExitCode.VALIDATION_ERROR.code
                    result.stderr shouldContain "warning: ${file.name}: "
                }
            }
        }

        context("reported severities") {

            test("the default config reports errors only") {
                withTempFile(content = ValidationDocuments.UNREFERENCED_RELEASE) { file ->
                    val result = validate(file)

                    result.statusCode shouldBe 0
                    result.stderr shouldNotContain "info: "
                }
            }

            test("the reporting config also reports infos") {
                withTempFile(content = ValidationDocuments.UNREFERENCED_RELEASE) { file ->
                    val result = validate(file, ValidationRequest(reportedSeverities = ALL_SEVERITIES))

                    result.statusCode shouldBe 0
                    result.stderr shouldContain "info: ${file.name}: "
                    result.stderr shouldContain "Unreferenced release ID"
                }
            }

            test("the reporting config also reports warnings") {
                withTempFile(content = ValidationDocuments.ANALYZED_BEFORE_REPORTED) { file ->
                    val result = validate(file, ValidationRequest(reportedSeverities = ALL_SEVERITIES))

                    result.stderr shouldContain "warning: ${file.name}: "
                }
            }
        }
    })
