// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.reporting.ImpactReport
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Severity
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.Impact
import dev.vulnlog.lib.model.reporting.ImpactEntry
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Instant

private val ACME = Project("Acme Corp", "Acme Web App", "Acme Corp Security Team")

private fun entry(release: String) =
    ImpactEntry(
        state = WorkState.OPEN,
        primaryId = VulnId.Cve("CVE-2026-0001"),
        ids = emptySet(),
        shortDescription = null,
        impact = Impact.Affected(Severity.HIGH),
        disposition = null,
        analysis = null,
        reportFor = setOf(Release(release)),
        fixedIn = emptySet(),
    )

class ImpactReportMessagesTest :
    FunSpec({

        test("reports the filter verbosely and how many rows the merge kept at debug") {
            val collected = listOf(entry("1.0.0"), entry("2.0.0"))
            val report =
                ImpactReport(
                    project = ACME,
                    entries = listOf(entry("1.0.0")),
                    generatedAt = Instant.EPOCH,
                    vulnlogVersion = "1.2.3",
                    inputs = listOf("app.vl.yaml"),
                    filter = ResolvedFilter(tags = setOf(Tag("app"))),
                    asOf = null,
                )

            val messages = renderImpactReportMessages(ImpactReportOutcome.Generated(report, collected, ""))

            messages shouldContainExactly
                listOf(
                    Message.Verbose("tag filter matched tags: app"),
                    Message.Debug("collected 2 report entries, merged to 1"),
                )
        }

        test("a failed run reports nothing besides its failure") {
            val failed = listOf(ProjectsDiffer(emptyList()), FilterRejected(emptyList()))

            val messages = failed.flatMap(::renderImpactReportMessages)

            messages.shouldBeEmpty()
        }

        test("differing projects name how many there are and what to do") {
            val other = Project("Other Corp", "Other App", "Other Security Team")

            val failures = renderImpactReportFailure(ProjectsDiffer(listOf(ACME, other)))

            failures shouldContainExactly
                listOf(
                    Failure(
                        "all input files must share the same project metadata, found 2 different ones",
                        "give every input the same project block, or report each project on its own",
                    ),
                )
        }

        test("a rejected filter is worded like every filter problem") {
            val problems = listOf(FilterProblem.UnknownReporter("bogus"))

            val failures = renderImpactReportFailure(FilterRejected(problems))

            failures shouldBe renderFilterProblems(problems)
        }
    })
