package dev.vulnlog.cli.core

import dev.vulnlog.cli.result.Rule
import dev.vulnlog.cli.result.Severity
import dev.vulnlog.cli.result.ValidationFinding
import dev.vulnlog.cli.result.ValidationResult
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

class ValidationRendererTest : FunSpec({

    test("no findings renders OK message") {
        val result = ValidationResult(emptyList())
        renderValidation(result) shouldBe "OK — no issues found."
    }

    test("single error renders error line and summary") {
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
        val output = renderValidation(result)
        output shouldContain "[ERROR] releases[v1.0]: Duplicate release ID 'v1.0'."
        output shouldContain "1 error(s)"
    }

    test("single warning renders warn line and summary") {
        val result =
            ValidationResult(
                listOf(
                    ValidationFinding(
                        severity = Severity.WARNING,
                        rule = Rule.ANALYSED_BEFORE_REPORTED,
                        path = "vulnerabilities[CVE-2021-1].analyzed_at",
                        message = "Analyzed date is before report date.",
                    ),
                ),
            )
        val output = renderValidation(result)
        output shouldContain "[WARN ] vulnerabilities[CVE-2021-1].analyzed_at: Analyzed date is before report date."
        output shouldContain "1 warning(s)"
    }

    test("single info renders info line and summary") {
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
        val output = renderValidation(result)
        output shouldContain "[INFO ] vulnerabilities[CVE-2021-1]: Some info."
        output shouldContain "1 info(s)"
    }

    test("errors appear before warnings in output") {
        val result =
            ValidationResult(
                listOf(
                    ValidationFinding(
                        severity = Severity.WARNING,
                        rule = Rule.ANALYSED_BEFORE_REPORTED,
                        path = "path.warning",
                        message = "A warning.",
                    ),
                    ValidationFinding(
                        severity = Severity.ERROR,
                        rule = Rule.DUPLICATE_RELEASE_ID,
                        path = "path.error",
                        message = "An error.",
                    ),
                ),
            )
        val output = renderValidation(result)
        val errorIndex = output.indexOf("[ERROR]")
        val warnIndex = output.indexOf("[WARN ]")
        assert(errorIndex < warnIndex) { "ERROR should appear before WARN in output" }
    }

    test("mixed findings summary lists all counts") {
        val result =
            ValidationResult(
                listOf(
                    ValidationFinding(Severity.ERROR, Rule.DUPLICATE_RELEASE_ID, "p1", "msg"),
                    ValidationFinding(Severity.WARNING, Rule.ANALYSED_BEFORE_REPORTED, "p2", "msg"),
                    ValidationFinding(Severity.INFO, Rule.DUPLICATE_VULNERABILITY_ID, "p3", "msg"),
                ),
            )
        val output = renderValidation(result)
        output shouldContain "1 error(s)"
        output shouldContain "1 warning(s)"
        output shouldContain "1 info(s)"
    }

    test("summary omits zero counts") {
        val result =
            ValidationResult(
                listOf(
                    ValidationFinding(Severity.ERROR, Rule.DUPLICATE_RELEASE_ID, "p1", "msg"),
                ),
            )
        val output = renderValidation(result)
        output shouldNotContain "warning(s)"
        output shouldNotContain "info(s)"
    }
})
