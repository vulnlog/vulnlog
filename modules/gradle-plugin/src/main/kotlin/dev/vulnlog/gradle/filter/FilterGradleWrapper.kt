// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.filter

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.renderFilterResolution
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.resolveReporterFilter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

/**
 * Turns the text a build script configured into a [FilterRequest].
 *
 * Gradle hands every filter dimension over as a string, so this is where they become domain types.
 * The CLI gets the same guarantee from its option declarations.
 */
fun DefaultTask.filterRequestOrFail(
    reporter: String?,
    release: String?,
    tags: Set<String>,
): FilterRequest =
    try {
        FilterRequest(
            reporter = resolveReporterFilter(reporter),
            release = release?.let(::Release),
            tags = tags.map(::Tag).toSet(),
        )
    } catch (e: FilterValidationException) {
        throw GradleException("${e.message}. ${e.detail}")
    }

/**
 * Checks [request] against [files] and reports what it resolved to on the verbose sink.
 * Every unknown filter value is named in the failure.
 */
fun DefaultTask.resolveFilterOrFail(
    request: FilterRequest,
    files: List<VulnlogFile>,
): ResolvedFilter =
    when (val outcome = resolveFilter(request, files)) {
        is FilterOutcome.Resolved -> {
            renderFilterResolution(outcome.filter).forEach(diagnosticSink()::verbose)
            outcome.filter
        }

        is FilterOutcome.Rejected ->
            throw GradleException(outcome.problems.joinToString(" ") { "${it.message}. ${it.hint}" })
    }
