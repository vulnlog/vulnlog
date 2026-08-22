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
import dev.vulnlog.lib.model.report.WorkState
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** A file declaring three releases in order and two tags, so every dimension has something to resolve against. */
private fun threeReleaseFile() =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0"), releaseEntry("3.0.0")),
        tags = listOf(tagEntry("internal"), tagEntry("public")),
    )

private fun FilterOutcome.filter(): ResolvedFilter = shouldBeInstanceOf<FilterOutcome.Resolved>().filter

private fun FilterOutcome.problems(): List<FilterProblem> = shouldBeInstanceOf<FilterOutcome.Rejected>().problems

class ResolveFilterTest :
    FunSpec({

        context("release window") {

            test("expands a release to itself and everything declared before it") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(asOf = "2.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe setOf(release("1.0.0"), release("2.0.0"))
            }

            test("expands the first release to itself alone") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(asOf = "1.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe setOf(release("1.0.0"))
            }

            test("expands the last release to the full history") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(asOf = "3.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe
                    setOf(release("1.0.0"), release("2.0.0"), release("3.0.0"))
            }

            test("leaves the window empty when no release is requested") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest()

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe emptySet()
            }

            test("unions the window over several files") {
                val files =
                    listOf(
                        threeReleaseFile(),
                        vulnlogFile(releases = listOf(releaseEntry("0.9.0"), releaseEntry("2.0.0"))),
                    )
                val request = FilterRequest(asOf = "2.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe
                    setOf(release("1.0.0"), release("2.0.0"), release("0.9.0"))
            }

            test("rejects a release no file declares") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(asOf = "9.9.9")

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Release not found: 9.9.9", "Known releases: 1.0.0, 2.0.0, 3.0.0"))
            }

            test("rejects a blank release") {
                val files = listOf(threeReleaseFile())

                val outcome = resolveFilter(FilterRequest(asOf = "  "), files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Release must not be blank", "Known releases: 1.0.0, 2.0.0, 3.0.0"))
            }

            test("rejects a release missing from one of several files") {
                val files = listOf(threeReleaseFile(), vulnlogFile(releases = listOf(releaseEntry("1.0.0"))))
                val request = FilterRequest(asOf = "2.0.0")

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Release not found: 2.0.0", "Known releases: 1.0.0, 2.0.0, 3.0.0"))
            }
        }

        context("tags") {

            test("passes through a tag the file declares") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(tags = setOf("internal"))

                val outcome = resolveFilter(request, files)

                outcome.filter().tags shouldBe setOf(tag("internal"))
            }

            test("accepts a tag declared in only one of several files") {
                val files = listOf(threeReleaseFile(), vulnlogFile(tags = listOf(tagEntry("release-blocker"))))
                val request = FilterRequest(tags = setOf("release-blocker"))

                val outcome = resolveFilter(request, files)

                outcome.filter().tags shouldBe setOf(tag("release-blocker"))
            }

            test("rejects unknown tags, naming all of them at once") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(tags = setOf("zzz", "aaa"))

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Tag not found: aaa, zzz", "Known tags: internal, public"))
            }

            test("rejects a blank tag") {
                val files = listOf(threeReleaseFile())

                val outcome = resolveFilter(FilterRequest(tags = setOf("")), files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Tag must not be blank", "Known tags: internal, public"))
            }

            test("says so plainly when the input declares no tags at all") {
                val files = listOf(vulnlogFile(releases = listOf(releaseEntry("1.0.0"))))
                val request = FilterRequest(tags = setOf("internal"))

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Tag not found: internal", "The input declares no tags."))
            }
        }

        context("reporter") {

            test("resolves a canonical reporter name to its type") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(reporter = "dependency-check")

                val outcome = resolveFilter(request, files)

                outcome.filter().reporter shouldBe ReporterType.DEPENDENCY_CHECK
            }

            test("leaves the reporter unset when none is requested") {
                val files = listOf(threeReleaseFile())

                val outcome = resolveFilter(FilterRequest(), files)

                outcome.filter().reporter shouldBe null
            }

            test("rejects a reporter Vulnlog does not support") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(reporter = "bogus")

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid reporter: bogus")
            }
        }

        context("states") {

            test("resolves a single state token") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("open"))

                val outcome = resolveFilter(request, files)

                outcome.filter().states shouldBe setOf(WorkState.OPEN)
            }

            test("resolves several state tokens cumulatively") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("open", "accepted"))

                val outcome = resolveFilter(request, files)

                outcome.filter().states shouldBe setOf(WorkState.OPEN, WorkState.ACCEPTED)
            }

            test("resolves the multi word tokens") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("under investigation", "not applicable"))

                val outcome = resolveFilter(request, files)

                outcome.filter().states shouldBe
                    setOf(WorkState.UNDER_INVESTIGATION, WorkState.NOT_APPLICABLE)
            }

            test("leaves the dimension inactive when no state is requested") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest()

                val outcome = resolveFilter(request, files)

                outcome.filter().states shouldBe emptySet()
            }

            test("accepts a state no entry currently has") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("resolved"))

                val outcome = resolveFilter(request, files)

                outcome.filter().states shouldBe setOf(WorkState.RESOLVED)
            }

            test("rejects a token Vulnlog does not define") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("bogus"))

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(
                        FilterProblem(
                            "Invalid state: bogus",
                            "Supported states: under investigation, open, accepted, resolved, not applicable",
                        ),
                    )
            }

            test("names every unknown token in one problem") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("bogus", "andere"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid state: andere, bogus")
            }

            test("rejects the enum spelling of a state") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("NOT_APPLICABLE"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid state: NOT_APPLICABLE")
            }
        }

        context("verdicts") {

            test("resolves a single verdict token") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(verdicts = setOf("affected"))

                val outcome = resolveFilter(request, files)

                outcome.filter().verdicts shouldBe setOf(VerdictKind.AFFECTED)
            }

            test("resolves several verdict tokens cumulatively") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(verdicts = setOf("affected", "not affected"))

                val outcome = resolveFilter(request, files)

                outcome.filter().verdicts shouldBe setOf(VerdictKind.AFFECTED, VerdictKind.NOT_AFFECTED)
            }

            test("resolves the untriaged token") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(verdicts = setOf("under investigation"))

                val outcome = resolveFilter(request, files)

                outcome.filter().verdicts shouldBe setOf(VerdictKind.UNDER_INVESTIGATION)
            }

            test("leaves the dimension inactive when no verdict is requested") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest()

                val outcome = resolveFilter(request, files)

                outcome.filter().verdicts shouldBe emptySet()
            }

            test("rejects a token Vulnlog does not define") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(verdicts = setOf("bogus"))

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(
                        FilterProblem(
                            "Invalid verdict: bogus",
                            "Supported verdicts: under investigation, affected, not affected",
                        ),
                    )
            }

            test("names every unknown token in one problem") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(verdicts = setOf("bogus", "andere"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid verdict: andere, bogus")
            }

            test("rejects the retired risk acceptable verdict") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(verdicts = setOf("risk acceptable"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid verdict: risk acceptable")
            }
        }

        context("dispositions") {

            test("resolves a single disposition token") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(dispositions = setOf("wont fix"))

                val outcome = resolveFilter(request, files)

                outcome.filter().dispositions shouldBe setOf(Disposition.WONT_FIX)
            }

            test("resolves both disposition tokens cumulatively") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(dispositions = setOf("will fix", "wont fix"))

                val outcome = resolveFilter(request, files)

                outcome.filter().dispositions shouldBe setOf(Disposition.WILL_FIX, Disposition.WONT_FIX)
            }

            test("leaves the dimension inactive when no disposition is requested") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest()

                val outcome = resolveFilter(request, files)

                outcome.filter().dispositions shouldBe emptySet()
            }

            test("rejects a token Vulnlog does not define") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(dispositions = setOf("bogus"))

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(
                        FilterProblem(
                            "Invalid disposition: bogus",
                            "Supported dispositions: will fix, wont fix",
                        ),
                    )
            }

            test("rejects the hyphenated spelling") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(dispositions = setOf("wont-fix"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid disposition: wont-fix")
            }

            test("names every unknown token in one problem") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(dispositions = setOf("bogus", "andere"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe listOf("Invalid disposition: andere, bogus")
            }
        }

        context("several failing dimensions") {

            test("reports every failing dimension in one outcome") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(reporter = "bogus", asOf = "9.9.9", tags = setOf("missing"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe
                    listOf("Invalid reporter: bogus", "Release not found: 9.9.9", "Tag not found: missing")
            }

            test("reports a bad state alongside the other failing dimensions") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(reporter = "bogus", asOf = "9.9.9", states = setOf("nope"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe
                    listOf("Invalid reporter: bogus", "Release not found: 9.9.9", "Invalid state: nope")
            }

            test("reports a bad verdict alongside a bad state") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(states = setOf("nope"), verdicts = setOf("bogus"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe
                    listOf("Invalid state: nope", "Invalid verdict: bogus")
            }

            test("reports every report-only dimension that failed") {
                val files = listOf(threeReleaseFile())
                val request =
                    FilterRequest(
                        states = setOf("nope"),
                        verdicts = setOf("bogus"),
                        dispositions = setOf("andere"),
                    )

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe
                    listOf("Invalid state: nope", "Invalid verdict: bogus", "Invalid disposition: andere")
            }
        }

        context("renderFilterResolution") {

            test("lists the expanded releases on their own") {
                val filter = ResolvedFilter(releases = setOf(release("1.0.0"), release("1.1.0")))

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("as-of filter expanded to releases: 1.0.0, 1.1.0")
            }

            test("lists the matched tags on their own") {
                val filter = ResolvedFilter(tags = setOf(tag("internal")))

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("tag filter matched tags: internal")
            }

            test("renders the reporter under its canonical name") {
                val filter = ResolvedFilter(reporter = ReporterType.CARGO_AUDIT)

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("reporter filter: cargo-audit")
            }

            test("renders the states under their canonical tokens") {
                val filter = ResolvedFilter(states = setOf(WorkState.NOT_APPLICABLE))

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("state filter: not applicable")
            }

            test("renders the verdicts under their canonical tokens") {
                val filter = ResolvedFilter(verdicts = setOf(VerdictKind.NOT_AFFECTED))

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("verdict filter: not affected")
            }

            test("renders the dispositions under their canonical tokens") {
                val filter = ResolvedFilter(dispositions = setOf(Disposition.WONT_FIX))

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("disposition filter: wont fix")
            }

            test("renders one line per active dimension") {
                val filter =
                    ResolvedFilter(
                        reporter = ReporterType.TRIVY,
                        releases = setOf(release("1.0.0"), release("2.0.0")),
                        tags = setOf(tag("internal")),
                        states = setOf(WorkState.OPEN),
                        verdicts = setOf(VerdictKind.AFFECTED),
                        dispositions = setOf(Disposition.WILL_FIX),
                    )

                val lines = renderFilterResolution(filter)

                lines shouldBe
                    listOf(
                        "as-of filter expanded to releases: 1.0.0, 2.0.0",
                        "tag filter matched tags: internal",
                        "reporter filter: trivy",
                        "state filter: open",
                        "verdict filter: affected",
                        "disposition filter: will fix",
                    )
            }

            test("renders nothing for an inactive filter") {
                val filter = ResolvedFilter()

                val lines = renderFilterResolution(filter)

                lines shouldBe emptyList()
            }
        }
    })
