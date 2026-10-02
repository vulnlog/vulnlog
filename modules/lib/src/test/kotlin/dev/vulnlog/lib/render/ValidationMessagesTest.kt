// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.finding.FailureLocation
import dev.vulnlog.lib.finding.FindingSeverity.ERROR
import dev.vulnlog.lib.finding.FindingSeverity.INFO
import dev.vulnlog.lib.finding.FindingSeverity.WARNING
import dev.vulnlog.lib.finding.ParseFailure
import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.finding
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidationMessagesTest :
    FunSpec({

        context("renderFindings") {

            test("renders one line per finding, followed by a summary") {
                val findings = listOf(finding(ERROR), finding(WARNING))

                val rendered = renderFindings("vulnlog.yaml", findings)

                rendered shouldBe
                    """
                    error: vulnlog.yaml: fixture path: fixture message
                    warning: vulnlog.yaml: fixture path: fixture message
                    1 error, 1 warning
                    """.trimIndent()
            }

            test("keeps only the findings of the reported severities") {
                val findings = listOf(finding(ERROR), finding(WARNING))

                val rendered = renderFindings("vulnlog.yaml", findings, setOf(ERROR))

                rendered shouldBe
                    """
                    error: vulnlog.yaml: fixture path: fixture message
                    1 error
                    """.trimIndent()
            }

            test("renders nothing when no finding has a reported severity") {
                val findingLists = listOf(emptyList(), listOf(finding(INFO)))

                val rendered = findingLists.map { renderFindings("vulnlog.yaml", it, setOf(ERROR)) }

                rendered shouldBe listOf("", "")
            }
        }

        test("renderProblem names the position and the path of a problem, as far as they are known") {
            val problems =
                listOf(
                    ParseFailure(
                        "Invalid verdict: maybe",
                        "vulnerabilities[CVE-2026-1].verdict",
                        FailureLocation(7, 14),
                    ),
                    ParseFailure("Missing schemaVersion", "schemaVersion"),
                    ParseFailure("Empty YAML document"),
                )

            val rendered = problems.map { renderProblem("vulnlog.yaml", it) }

            rendered shouldBe
                listOf(
                    "error: vulnlog.yaml: 7:14: vulnerabilities[CVE-2026-1].verdict: Invalid verdict: maybe",
                    "error: vulnlog.yaml: schemaVersion: Missing schemaVersion",
                    "error: vulnlog.yaml: Empty YAML document",
                )
        }

        test("renderValidationSummary counts every finding, including those the output holds back") {
            val findingLists =
                listOf(emptyList(), listOf(finding(ERROR), finding(WARNING), finding(WARNING)), listOf(finding(INFO)))

            val rendered = findingLists.map { renderValidationSummary("vulnlog.yaml", it) }

            rendered shouldBe
                listOf(
                    "validated vulnlog.yaml: no findings",
                    "validated vulnlog.yaml: 1 error, 2 warnings",
                    "validated vulnlog.yaml: 1 info",
                )
        }

        test("renderParsedProject states the schema version and the entry counts") {
            val files =
                listOf(
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0"), releaseEntry("v2.0")),
                        tags = listOf(tagEntry("backend")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"))),
                    ),
                    vulnlogFile(),
                )

            val rendered = files.map { renderParsedProject("vulnlog.yaml", it) }

            rendered shouldBe
                listOf(
                    "parsed vulnlog.yaml: schema version 1, releases: 2, tags: 1, vulnerabilities: 1",
                    "parsed vulnlog.yaml: schema version 1, releases: 0, tags: 0, vulnerabilities: 0",
                )
        }
    })
