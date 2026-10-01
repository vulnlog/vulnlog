// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.filter

import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.fixtures.release
import dev.vulnlog.lib.fixtures.releaseEntry
import dev.vulnlog.lib.fixtures.tag
import dev.vulnlog.lib.fixtures.tagEntry
import dev.vulnlog.lib.fixtures.vulnlogFile
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

private val twoReleaseFile =
    vulnlogFile(
        releases = listOf(releaseEntry("1.0.0"), releaseEntry("2.0.0")),
        tags = listOf(tagEntry("internal")),
    )

class FilterGradleWrapperTest :
    FunSpec({

        test("hands back the resolved filter") {
            val request = FilterRequest(asOf = "2.0.0", tags = setOf("internal"))

            val filter = wrapperTask().resolveFilterOrFail(request, listOf(twoReleaseFile))

            filter shouldBe
                ResolvedFilter(releases = setOf(release("1.0.0"), release("2.0.0")), tags = setOf(tag("internal")))
        }

        test("names every problem with its hint in one failure") {
            val request = FilterRequest(asOf = "9.9.9", tags = setOf("missing"))

            val failure =
                shouldThrow<GradleException> { wrapperTask().resolveFilterOrFail(request, listOf(twoReleaseFile)) }

            failure.message.orEmpty() shouldContain "Release not found: 9.9.9. Known releases: 1.0.0, 2.0.0"
            failure.message.orEmpty() shouldContain "Tag not found: missing. Known tags: internal"
        }
    })
