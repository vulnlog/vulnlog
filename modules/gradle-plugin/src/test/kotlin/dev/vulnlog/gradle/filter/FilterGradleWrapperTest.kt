// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.filter

import dev.vulnlog.lib.core.filter.FilterRequest
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
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder

private fun wrapperTask(): DefaultTask =
    ProjectBuilder
        .builder()
        .build()
        .tasks
        .register("wrapper", DefaultTask::class.java)
        .get()

/** A file declaring two releases in order and one tag, for the wrapper to resolve against. */
private fun twoReleaseFile(): VulnlogFile =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0")),
        tags = listOf(tagEntry("internal")),
    )

class FilterGradleWrapperTest :
    FunSpec({

        context("resolveFilterOrFail") {

            test("hands back the expanded release window") {
                val task = wrapperTask()
                val request = FilterRequest(asOf = "2.0.0")

                val filter = task.resolveFilterOrFail(request, listOf(twoReleaseFile()))

                filter.releases shouldBe setOf(release("1.0.0"), release("2.0.0"))
            }

            test("hands back the tags and the reporter untouched") {
                val task = wrapperTask()
                val request = FilterRequest(reporter = "trivy", tags = setOf("internal"))

                val filter = task.resolveFilterOrFail(request, listOf(twoReleaseFile()))

                filter.tags shouldBe setOf(tag("internal"))
                filter.reporter shouldBe ReporterType.TRIVY
            }

            test("hands back the resolved states") {
                val task = wrapperTask()
                val request = FilterRequest(states = setOf("open", "not applicable"))

                val filter = task.resolveFilterOrFail(request, listOf(twoReleaseFile()))

                filter.states shouldBe setOf(WorkState.OPEN, WorkState.NOT_APPLICABLE)
            }

            test("fails on a state Vulnlog does not define") {
                val task = wrapperTask()
                val request = FilterRequest(states = setOf("bogus"))

                val failure =
                    shouldThrow<GradleException> { task.resolveFilterOrFail(request, listOf(twoReleaseFile())) }

                failure.message.orEmpty() shouldContain "Invalid state: bogus"
                failure.message.orEmpty() shouldContain "Supported states:"
            }

            test("hands back the resolved verdicts") {
                val task = wrapperTask()
                val request = FilterRequest(verdicts = setOf("affected", "under investigation"))

                val filter = task.resolveFilterOrFail(request, listOf(twoReleaseFile()))

                filter.verdicts shouldBe setOf(VerdictKind.AFFECTED, VerdictKind.UNDER_INVESTIGATION)
            }

            test("fails on a verdict Vulnlog does not define") {
                val task = wrapperTask()
                val request = FilterRequest(verdicts = setOf("bogus"))

                val failure =
                    shouldThrow<GradleException> { task.resolveFilterOrFail(request, listOf(twoReleaseFile())) }

                failure.message.orEmpty() shouldContain "Invalid verdict: bogus"
                failure.message.orEmpty() shouldContain "Supported verdicts:"
            }

            test("hands back the resolved dispositions") {
                val task = wrapperTask()
                val request = FilterRequest(dispositions = setOf("wont fix"))

                val filter = task.resolveFilterOrFail(request, listOf(twoReleaseFile()))

                filter.dispositions shouldBe setOf(Disposition.WONT_FIX)
            }

            test("fails on a disposition Vulnlog does not define") {
                val task = wrapperTask()
                val request = FilterRequest(dispositions = setOf("bogus"))

                val failure =
                    shouldThrow<GradleException> { task.resolveFilterOrFail(request, listOf(twoReleaseFile())) }

                failure.message.orEmpty() shouldContain "Invalid disposition: bogus"
                failure.message.orEmpty() shouldContain "Supported dispositions:"
            }

            test("fails on a reporter Vulnlog does not support") {
                val task = wrapperTask()
                val request = FilterRequest(reporter = "bogus")

                val failure =
                    shouldThrow<GradleException> { task.resolveFilterOrFail(request, listOf(twoReleaseFile())) }

                failure.message.orEmpty() shouldContain "Invalid reporter: bogus"
                failure.message.orEmpty() shouldContain "Supported reporters:"
            }

            test("fails on a release the files do not declare") {
                val task = wrapperTask()
                val request = FilterRequest(asOf = "9.9.9")

                val failure =
                    shouldThrow<GradleException> { task.resolveFilterOrFail(request, listOf(twoReleaseFile())) }

                failure.message.orEmpty() shouldContain "Release not found: 9.9.9"
                failure.message.orEmpty() shouldContain "Known releases: 1.0.0, 2.0.0"
            }

            test("names every failing dimension in one failure") {
                val task = wrapperTask()
                val request = FilterRequest(asOf = "9.9.9", tags = setOf("missing"))

                val failure =
                    shouldThrow<GradleException> { task.resolveFilterOrFail(request, listOf(twoReleaseFile())) }

                failure.message.orEmpty() shouldContain "Release not found: 9.9.9"
                failure.message.orEmpty() shouldContain "Tag not found: missing"
            }
        }
    })
