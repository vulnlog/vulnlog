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

private fun lines(problem: FilterProblem): List<String> = renderFilterProblemLines(listOf(problem))

class FilterMessagesTest :
    FunSpec({

        context("renderFilterProblemLines") {

            test("follows every problem with its hint, in order") {
                val problems =
                    listOf(
                        FilterProblem.UnknownRelease(release("9.9.9"), threeReleases),
                        FilterProblem.BlankRelease(threeReleases),
                    )

                renderFilterProblemLines(problems) shouldContainExactly
                    listOf(
                        "error: Release not found: 9.9.9",
                        "  hint: Known releases: 1.0.0, 2.0.0, 3.0.0",
                        "error: Release must not be blank",
                        "  hint: Known releases: 1.0.0, 2.0.0, 3.0.0",
                    )
            }

            test("names the known tags, or says plainly that the input declares none") {
                val known = FilterProblem.UnknownTags(listOf(tag("aaa"), tag("zzz")), listOf(tag("internal")))
                val none = FilterProblem.BlankTag(emptyList())

                lines(known) shouldContainExactly
                    listOf("error: Tag not found: aaa, zzz", "  hint: Known tags: internal")
                lines(none) shouldContainExactly
                    listOf("error: Tag must not be blank", "  hint: The input declares no tags.")
            }

            test("names an unknown reporter and the supported ones") {
                val (message, hint) = lines(FilterProblem.UnknownReporter("bogus"))

                message shouldBe "error: Invalid reporter: bogus"
                hint shouldStartWith "  hint: Supported reporters: dependency-check, github-dependabot"
            }

            test("names every unknown state and the supported ones") {
                lines(FilterProblem.UnknownStates(listOf("andere", "bogus"))) shouldContainExactly
                    listOf(
                        "error: Invalid state: andere, bogus",
                        "  hint: Supported states: under investigation, open, accepted, resolved, not applicable",
                    )
            }

            test("names an unknown verdict and the supported ones") {
                lines(FilterProblem.UnknownVerdicts(listOf("bogus"))) shouldContainExactly
                    listOf(
                        "error: Invalid verdict: bogus",
                        "  hint: Supported verdicts: under investigation, affected, not affected",
                    )
            }

            test("names an unknown disposition and the supported ones") {
                lines(FilterProblem.UnknownDispositions(listOf("bogus"))) shouldContainExactly
                    listOf("error: Invalid disposition: bogus", "  hint: Supported dispositions: will fix, wont fix")
            }
        }

        context("renderFilterFailure") {

            test("puts every problem and its hint in one message") {
                val problems =
                    listOf(
                        FilterProblem.UnknownRelease(release("9.9.9"), threeReleases),
                        FilterProblem.UnknownTags(listOf(tag("missing")), listOf(tag("internal"))),
                    )

                renderFilterFailure(problems) shouldBe
                    "Release not found: 9.9.9. Known releases: 1.0.0, 2.0.0, 3.0.0 " +
                    "Tag not found: missing. Known tags: internal"
            }
        }
    })
