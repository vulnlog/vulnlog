// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

import dev.vulnlog.lib.fixtures.cve
import dev.vulnlog.lib.fixtures.finding
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnerability
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ValidationRendererTest :
    FunSpec({

        context("renderValidationFindings") {

            test("renders one line per finding followed by a summary") {
                val findings = listOf(finding(FindingSeverity.ERROR), finding(FindingSeverity.WARNING))

                val rendered = renderFindings("vulnlog.yaml", findings)

                rendered shouldBe
                    """
                    error: vulnlog.yaml: fixture path: fixture message
                    warning: vulnlog.yaml: fixture path: fixture message
                    1 error, 1 warning
                    """.trimIndent()
            }

            test("keeps only the findings of the requested severities") {
                val findings = listOf(finding(FindingSeverity.ERROR), finding(FindingSeverity.WARNING))

                val rendered = renderFindings("vulnlog.yaml", findings, setOf(FindingSeverity.ERROR))

                rendered shouldBe
                    """
                    error: vulnlog.yaml: fixture path: fixture message
                    1 error
                    """.trimIndent()
            }

            test("renders nothing when no finding has a requested severity") {
                val findings = listOf(finding(FindingSeverity.INFO))

                val rendered = renderFindings("vulnlog.yaml", findings, setOf(FindingSeverity.ERROR))

                rendered shouldBe ""
            }

            test("renders nothing when there are no findings") {
                val rendered = renderFindings("vulnlog.yaml", emptyList())

                rendered shouldBe ""
            }
        }

        context("renderValidationSummary") {

            test("reports 'no findings' when there are none") {
                val rendered = renderValidationSummary("vulnlog.yaml", emptyList())

                rendered shouldBe "validated vulnlog.yaml: no findings"
            }

            test("reports the counts per severity") {
                val findings =
                    listOf(
                        finding(FindingSeverity.ERROR),
                        finding(FindingSeverity.WARNING),
                        finding(FindingSeverity.WARNING),
                    )

                val rendered = renderValidationSummary("vulnlog.yaml", findings)

                rendered shouldBe "validated vulnlog.yaml: 1 error, 2 warnings"
            }

            test("counts findings the output holds back") {
                val findings = listOf(finding(FindingSeverity.INFO))

                val rendered = renderValidationSummary("vulnlog.yaml", findings)

                rendered shouldBe "validated vulnlog.yaml: 1 info"
            }
        }

        context("renderParsedProject") {

            test("states the schema version and the entry counts") {
                val file =
                    vulnlogFile(
                        releases = listOf(releaseEntry("v1.0"), releaseEntry("v2.0")),
                        tags = listOf(tagEntry("backend")),
                        vulnerabilities = listOf(vulnerability(cve("CVE-2021-1"))),
                    )

                val rendered = renderParsedProject("vulnlog.yaml", file)

                rendered shouldBe
                    "parsed vulnlog.yaml: schema version 1, releases: 2, tags: 1, vulnerabilities: 1"
            }

            test("counts an empty file as zero of everything") {
                val rendered = renderParsedProject("vulnlog.yaml", vulnlogFile())

                rendered shouldBe
                    "parsed vulnlog.yaml: schema version 1, releases: 0, tags: 0, vulnerabilities: 0"
            }
        }
    })
