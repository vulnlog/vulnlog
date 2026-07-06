// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.result.Rule
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationFinding
import dev.vulnlog.lib.result.ValidationResult
import dev.vulnlog.lib.result.ValidationResults
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.nio.file.Path

private fun finding(severity: Severity) =
    ValidationFinding(
        severity = severity,
        rule = Rule.DANGLING_TAG_REFERENCE,
        path = "vulnerabilities[CVE-2026-1234].tags",
        message = "tag not defined",
    )

private fun resultsFor(vararg fileToFindings: Pair<String, List<ValidationFinding>>) =
    ValidationResults(
        fileToFindings.associate { (name, findings) ->
            FileInputOption.File(Path.of(name)) to ValidationResult(findings)
        },
    )

class ValidateFileTest :
    FunSpec({

        context("renderValidationSummary") {

            test("renders no findings for a clean file") {
                renderValidationSummary(resultsFor("clean.vl.yaml" to emptyList())) shouldBe
                    listOf("validated clean.vl.yaml: no findings")
            }

            test("renders the counts per severity, omitting empty severities") {
                val results =
                    resultsFor(
                        "app.vl.yaml" to
                            listOf(
                                finding(Severity.ERROR),
                                finding(Severity.WARNING),
                                finding(Severity.WARNING),
                            ),
                    )

                renderValidationSummary(results) shouldBe
                    listOf("validated app.vl.yaml: 1 error, 2 warnings")
            }

            test("renders one sorted line per file") {
                val results =
                    resultsFor(
                        "b.vl.yaml" to emptyList(),
                        "a.vl.yaml" to listOf(finding(Severity.INFO)),
                    )

                renderValidationSummary(results) shouldBe
                    listOf(
                        "validated a.vl.yaml: 1 info",
                        "validated b.vl.yaml: no findings",
                    )
            }
        }
    })
