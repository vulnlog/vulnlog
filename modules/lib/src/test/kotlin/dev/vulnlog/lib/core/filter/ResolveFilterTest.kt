// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.Disposition
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VerdictKind
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.reporting.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

private fun threeReleaseFile() =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0"), releaseEntry("3.0.0")),
        tags = listOf(tagEntry("internal"), tagEntry("public")),
    )

private val threeReleases = listOf(release("1.0.0"), release("2.0.0"), release("3.0.0"))

private val twoTags = listOf(tag("internal"), tag("public"))

private fun resolve(
    request: FilterRequest,
    vararg extraFiles: VulnlogFile,
): FilterOutcome = resolveFilter(request, listOf(threeReleaseFile()) + extraFiles)

private fun FilterOutcome.filter(): ResolvedFilter = shouldBeInstanceOf<FilterOutcome.Resolved>().filter

private fun FilterOutcome.problems(): List<FilterProblem> = shouldBeInstanceOf<FilterOutcome.Rejected>().problems

class ResolveFilterTest :
    FunSpec({

        test("an empty request resolves to an inactive filter") {
            val outcome = resolve(FilterRequest())

            outcome.filter() shouldBe ResolvedFilter()
        }

        test("reports every failing dimension in one outcome, in request order") {
            val request =
                FilterRequest(
                    reporter = "bogus",
                    asOf = "9.9.9",
                    tags = setOf("missing"),
                    states = setOf("nope"),
                    verdicts = setOf("bogus"),
                    dispositions = setOf("andere"),
                    fixedIn = "8.8.8",
                )

            val outcome = resolve(request)

            outcome.problems() shouldBe
                listOf(
                    FilterProblem.UnknownReporter("bogus"),
                    FilterProblem.UnknownRelease(release("9.9.9"), threeReleases),
                    FilterProblem.UnknownTags(listOf(tag("missing")), twoTags),
                    FilterProblem.UnknownStates(listOf("nope")),
                    FilterProblem.UnknownVerdicts(listOf("bogus")),
                    FilterProblem.UnknownDispositions(listOf("andere")),
                    FilterProblem.UnknownRelease(release("8.8.8"), threeReleases),
                )
        }

        context("release window") {

            test("expands a release to itself and everything declared before it") {
                val outcome = resolve(FilterRequest(asOf = "2.0.0"))

                outcome.filter().releases shouldBe setOf(release("1.0.0"), release("2.0.0"))
            }

            test("expands the first release to itself alone") {
                val outcome = resolve(FilterRequest(asOf = "1.0.0"))

                outcome.filter().releases shouldBe setOf(release("1.0.0"))
            }

            test("unions the window over several files") {
                val other = vulnlogFile(releases = listOf(releaseEntry("0.9.0"), releaseEntry("2.0.0")))

                val outcome = resolve(FilterRequest(asOf = "2.0.0"), other)

                outcome.filter().releases shouldBe setOf(release("1.0.0"), release("2.0.0"), release("0.9.0"))
            }

            test("rejects a release missing from one of several files and knows only those every file declares") {
                val other = vulnlogFile(releases = listOf(releaseEntry("1.0.0")))

                val outcome = resolve(FilterRequest(asOf = "2.0.0"), other)

                outcome.problems() shouldBe
                    listOf(FilterProblem.UnknownRelease(release("2.0.0"), listOf(release("1.0.0"))))
            }

            test("rejects a blank release") {
                val outcome = resolve(FilterRequest(asOf = "  "))

                outcome.problems() shouldBe listOf(FilterProblem.BlankRelease(threeReleases))
            }
        }

        context("fixed-in") {

            test("accepts a release only one of several files declares") {
                val other = vulnlogFile(releases = listOf(releaseEntry("0.9.0")))

                val outcome = resolve(FilterRequest(fixedIn = "3.0.0"), other)

                outcome.filter().fixedIn shouldBe release("3.0.0")
            }

            test("rejects a release no file declares and knows the releases of every file") {
                val other = vulnlogFile(releases = listOf(releaseEntry("0.9.0")))

                val outcome = resolve(FilterRequest(fixedIn = "9.9.9"), other)

                outcome.problems() shouldBe
                    listOf(FilterProblem.UnknownRelease(release("9.9.9"), threeReleases + release("0.9.0")))
            }

            test("rejects a blank release") {
                val outcome = resolve(FilterRequest(fixedIn = " "))

                outcome.problems() shouldBe listOf(FilterProblem.BlankRelease(threeReleases))
            }
        }

        context("tags") {

            test("accepts a tag only one of several files declares") {
                val other = vulnlogFile(tags = listOf(tagEntry("release-blocker")))

                val outcome = resolve(FilterRequest(tags = setOf("internal", "release-blocker")), other)

                outcome.filter().tags shouldBe setOf(tag("internal"), tag("release-blocker"))
            }

            test("names every unknown tag, sorted, and none of the known ones") {
                val outcome = resolve(FilterRequest(tags = setOf("zzz", "internal", "aaa")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownTags(listOf(tag("aaa"), tag("zzz")), twoTags))
            }

            test("rejects a blank tag") {
                val outcome = resolve(FilterRequest(tags = setOf("")))

                outcome.problems() shouldBe listOf(FilterProblem.BlankTag(twoTags))
            }
        }

        context("reporter") {

            test("resolves a canonical reporter name to its type") {
                val outcome = resolve(FilterRequest(reporter = "dependency-check"))

                outcome.filter().reporter shouldBe ReporterType.DEPENDENCY_CHECK
            }

            test("rejects a reporter Vulnlog does not support") {
                val outcome = resolve(FilterRequest(reporter = "bogus"))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownReporter("bogus"))
            }
        }

        context("states") {

            test("resolves every requested token, multi-word ones included") {
                val outcome = resolve(FilterRequest(states = setOf("open", "not applicable")))

                outcome.filter().states shouldBe setOf(WorkState.OPEN, WorkState.NOT_APPLICABLE)
            }

            test("names every unknown token, sorted, and none of the known ones") {
                val outcome = resolve(FilterRequest(states = setOf("bogus", "open", "andere")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownStates(listOf("andere", "bogus")))
            }

            test("rejects the enum spelling") {
                val outcome = resolve(FilterRequest(states = setOf("NOT_APPLICABLE")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownStates(listOf("NOT_APPLICABLE")))
            }
        }

        context("verdicts") {

            test("resolves every requested token, multi-word ones included") {
                val request = FilterRequest(verdicts = setOf("affected", "not affected", "under investigation"))

                val outcome = resolve(request)

                outcome.filter().verdicts shouldBe
                    setOf(VerdictKind.AFFECTED, VerdictKind.NOT_AFFECTED, VerdictKind.UNDER_INVESTIGATION)
            }

            test("names every unknown token, sorted, and none of the known ones") {
                val outcome = resolve(FilterRequest(verdicts = setOf("bogus", "affected", "andere")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownVerdicts(listOf("andere", "bogus")))
            }

            test("rejects the retired risk acceptable verdict") {
                val outcome = resolve(FilterRequest(verdicts = setOf("risk acceptable")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownVerdicts(listOf("risk acceptable")))
            }
        }

        context("dispositions") {

            test("resolves every requested token") {
                val outcome = resolve(FilterRequest(dispositions = setOf("will fix", "wont fix")))

                outcome.filter().dispositions shouldBe setOf(Disposition.WILL_FIX, Disposition.WONT_FIX)
            }

            test("names every unknown token, sorted, and none of the known ones") {
                val outcome = resolve(FilterRequest(dispositions = setOf("bogus", "wont fix", "andere")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownDispositions(listOf("andere", "bogus")))
            }

            test("rejects the hyphenated spelling") {
                val outcome = resolve(FilterRequest(dispositions = setOf("wont-fix")))

                outcome.problems() shouldBe listOf(FilterProblem.UnknownDispositions(listOf("wont-fix")))
            }
        }
    })
