// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.filter

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.renderFilterResolution
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.model.VulnlogFile
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

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
