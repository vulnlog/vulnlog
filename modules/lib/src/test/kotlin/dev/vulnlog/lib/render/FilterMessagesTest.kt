// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.filter.FilterProblem
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.tag
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

private val threeReleases = listOf(release("1.0.0"), release("2.0.0"), release("3.0.0"))

class FilterMessagesTest :
    FunSpec({

        test("words every release problem, in order, with the releases to choose from") {
            val problems =
                listOf(
                    FilterProblem.UnknownRelease(release("9.9.9"), threeReleases),
                    FilterProblem.BlankRelease(threeReleases),
                )

            val failures = renderFilterProblems(problems)

            failures shouldContainExactly
                listOf(
                    Failure("Release not found: 9.9.9", "Known releases: 1.0.0, 2.0.0, 3.0.0"),
                    Failure("Release must not be blank", "Known releases: 1.0.0, 2.0.0, 3.0.0"),
                )
        }

        test("names the known tags, or says plainly that the input declares none") {
            val problems =
                listOf(
                    FilterProblem.UnknownTags(listOf(tag("aaa"), tag("zzz")), listOf(tag("internal"))),
                    FilterProblem.BlankTag(emptyList()),
                )

            val failures = renderFilterProblems(problems)

            failures shouldContainExactly
                listOf(
                    Failure("Tag not found: aaa, zzz", "Known tags: internal"),
                    Failure("Tag must not be blank", "The input declares no tags."),
                )
        }

        test("names an unknown reporter and the supported ones") {
            val problems = listOf(FilterProblem.UnknownReporter("bogus"))

            val failure = renderFilterProblems(problems).single()

            failure.message shouldBe "Invalid reporter: bogus"
            failure.hint shouldStartWith "Supported reporters: dependency-check, github-dependabot"
        }

        test("names every unknown state, verdict and disposition and the supported ones") {
            val problems =
                listOf(
                    FilterProblem.UnknownStates(listOf("andere", "bogus")),
                    FilterProblem.UnknownVerdicts(listOf("bogus")),
                    FilterProblem.UnknownDispositions(listOf("bogus")),
                )

            val failures = renderFilterProblems(problems)

            failures shouldContainExactly
                listOf(
                    Failure(
                        "Invalid state: andere, bogus",
                        "Supported states: under investigation, open, accepted, resolved, not applicable",
                    ),
                    Failure(
                        "Invalid verdict: bogus",
                        "Supported verdicts: under investigation, affected, not affected",
                    ),
                    Failure("Invalid disposition: bogus", "Supported dispositions: will fix, wont fix"),
                )
        }
    })
