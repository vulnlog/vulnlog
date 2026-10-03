// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.ChangelogOutcome
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.reporting.Changelog
import dev.vulnlog.lib.model.reporting.ChangelogEntry
import dev.vulnlog.lib.model.reporting.ChangelogRelease
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

private val ACME = Project("Acme Corp", "Acme Web App", "Acme Corp Security Team")
private val FILTER = ResolvedFilter(fixedIn = Release("2.0.0"))

private fun released(
    version: String,
    vararg ids: String,
) = ChangelogRelease(fixedIn = Release(version), entries = ids.map { ChangelogEntry(VulnId.Cve(it)) })

class ChangelogMessagesTest :
    FunSpec({

        test("reports the filter verbosely and how many fixes it collected at debug") {
            val changelog = Changelog(ACME, listOf(released("2.0.0", "CVE-2026-0001", "CVE-2026-0002")))

            val messages = renderChangelogMessages(ChangelogOutcome.Fixed(FILTER, changelog, ""))

            messages shouldContainExactly
                listOf(
                    Message.Verbose("fixed-in filter: 2.0.0"),
                    Message.Debug("collected 2 fixes in 1 releases"),
                )
        }

        test("nothing fixed ends with an info status") {
            val outcome = ChangelogOutcome.NothingFixed(ResolvedFilter(), Changelog(ACME, emptyList()), "")

            val messages = renderChangelogMessages(outcome)

            messages shouldContainExactly
                listOf(
                    Message.Debug("collected 0 fixes in 0 releases"),
                    Message.Status("info: no fixed vulnerabilities to report"),
                )
        }

        test("a failed run reports nothing besides its failure") {
            val failed = listOf(ProjectsDiffer(emptyList()), FilterRejected(emptyList()))

            val messages = failed.flatMap(::renderChangelogMessages)

            messages.shouldBeEmpty()
        }

        test("words differing projects and a rejected filter like the impact report") {
            val problems = listOf(FilterProblem.UnknownReporter("bogus"))
            val differ = ProjectsDiffer(listOf(ACME, ACME.copy(name = "Other")))

            val failures = listOf(differ, FilterRejected(problems)).map(::renderChangelogFailure)

            failures shouldBe listOf(listOf(renderProjectsDiffer(differ)), renderFilterProblems(problems))
        }
    })
