// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.filter

import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
import dev.vulnlog.lib.model.ReporterType
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
                val request = FilterRequest(release = "2.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe setOf(release("1.0.0"), release("2.0.0"))
            }

            test("expands the first release to itself alone") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(release = "1.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe setOf(release("1.0.0"))
            }

            test("expands the last release to the full history") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(release = "3.0.0")

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
                val request = FilterRequest(release = "2.0.0")

                val outcome = resolveFilter(request, files)

                outcome.filter().releases shouldBe
                    setOf(release("1.0.0"), release("2.0.0"), release("0.9.0"))
            }

            test("rejects a release no file declares") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(release = "9.9.9")

                val outcome = resolveFilter(request, files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Release not found: 9.9.9", "Known releases: 1.0.0, 2.0.0, 3.0.0"))
            }

            test("rejects a blank release") {
                val files = listOf(threeReleaseFile())

                val outcome = resolveFilter(FilterRequest(release = "  "), files)

                outcome.problems() shouldBe
                    listOf(FilterProblem("Release must not be blank", "Known releases: 1.0.0, 2.0.0, 3.0.0"))
            }

            test("rejects a release missing from one of several files") {
                val files = listOf(threeReleaseFile(), vulnlogFile(releases = listOf(releaseEntry("1.0.0"))))
                val request = FilterRequest(release = "2.0.0")

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

        context("several failing dimensions") {

            test("reports every failing dimension in one outcome") {
                val files = listOf(threeReleaseFile())
                val request = FilterRequest(reporter = "bogus", release = "9.9.9", tags = setOf("missing"))

                val outcome = resolveFilter(request, files)

                outcome.problems().map { it.message } shouldBe
                    listOf("Invalid reporter: bogus", "Release not found: 9.9.9", "Tag not found: missing")
            }
        }

        context("renderFilterResolution") {

            test("lists the expanded releases on their own") {
                val filter = ResolvedFilter(releases = setOf(release("1.0.0"), release("1.1.0")))

                val lines = renderFilterResolution(filter)

                lines shouldBe listOf("release filter expanded to releases: 1.0.0, 1.1.0")
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

            test("renders one line per active dimension") {
                val filter =
                    ResolvedFilter(
                        reporter = ReporterType.TRIVY,
                        releases = setOf(release("1.0.0"), release("2.0.0")),
                        tags = setOf(tag("internal")),
                    )

                val lines = renderFilterResolution(filter)

                lines shouldBe
                    listOf(
                        "release filter expanded to releases: 1.0.0, 2.0.0",
                        "tag filter matched tags: internal",
                        "reporter filter: trivy",
                    )
            }

            test("renders nothing for an inactive filter") {
                val filter = ResolvedFilter()

                val lines = renderFilterResolution(filter)

                lines shouldBe emptyList()
            }
        }
    })
