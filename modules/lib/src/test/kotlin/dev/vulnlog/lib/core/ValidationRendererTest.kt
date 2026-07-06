// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.result.Rule
import dev.vulnlog.lib.result.Severity
import dev.vulnlog.lib.result.ValidationFinding
import dev.vulnlog.lib.result.ValidationResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class ValidationRendererTest :
    FunSpec({

        test("no findings renders nothing") {
            renderValidation("vulnlog.yaml", ValidationResult(emptyList())).shouldBeEmpty()
        }

        test("error finding renders one line in the severity grammar") {
            val result =
                ValidationResult(
                    listOf(
                        ValidationFinding(
                            severity = Severity.ERROR,
                            rule = Rule.DUPLICATE_RELEASE_ID,
                            path = "releases[v1.0]",
                            message = "Duplicate release ID 'v1.0'.",
                        ),
                    ),
                )
            renderValidation("vulnlog.yaml", result) shouldBe
                listOf("error: vulnlog.yaml: releases[v1.0]: Duplicate release ID 'v1.0'.")
        }

        test("warning finding renders with the warning prefix") {
            val result =
                ValidationResult(
                    listOf(
                        ValidationFinding(
                            severity = Severity.WARNING,
                            rule = Rule.ANALYZED_BEFORE_REPORTED,
                            path = "vulnerabilities[CVE-2021-1].analyzed_at",
                            message = "Analyzed date is before report date.",
                        ),
                    ),
                )
            renderValidation("vulnlog.yaml", result) shouldBe
                listOf(
                    "warning: vulnlog.yaml: vulnerabilities[CVE-2021-1].analyzed_at: " +
                        "Analyzed date is before report date.",
                )
        }

        test("info finding renders with the info prefix") {
            val result =
                ValidationResult(
                    listOf(
                        ValidationFinding(
                            severity = Severity.INFO,
                            rule = Rule.DUPLICATE_VULNERABILITY_ID,
                            path = "vulnerabilities[CVE-2021-1]",
                            message = "Some info.",
                        ),
                    ),
                )
            renderValidation("vulnlog.yaml", result) shouldBe
                listOf("info: vulnlog.yaml: vulnerabilities[CVE-2021-1]: Some info.")
        }

        test("errors appear before warnings, warnings before infos") {
            val result =
                ValidationResult(
                    listOf(
                        ValidationFinding(Severity.INFO, Rule.UNREFERENCED_RELEASE_ID, "path.info", "An info."),
                        ValidationFinding(
                            Severity.WARNING,
                            Rule.ANALYZED_BEFORE_REPORTED,
                            "path.warning",
                            "A warning.",
                        ),
                        ValidationFinding(Severity.ERROR, Rule.DUPLICATE_RELEASE_ID, "path.error", "An error."),
                    ),
                )
            renderValidation("vulnlog.yaml", result) shouldBe
                listOf(
                    "error: vulnlog.yaml: path.error: An error.",
                    "warning: vulnlog.yaml: path.warning: A warning.",
                    "info: vulnlog.yaml: path.info: An info.",
                )
        }
    })
