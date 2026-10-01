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
import dev.vulnlog.lib.render.renderFilterFailure
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

/** Reports what [request] resolved to on the verbose sink. */
fun DefaultTask.resolveFilterOrFail(
    request: FilterRequest,
    files: List<VulnlogFile>,
): ResolvedFilter =
    when (val outcome = resolveFilter(request, files)) {
        is FilterOutcome.Resolved -> {
            renderFilterResolution(outcome.filter).forEach(diagnosticSink()::verbose)
            outcome.filter
        }

        is FilterOutcome.Rejected -> throw GradleException(renderFilterFailure(outcome.problems))
    }
