// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.filter

import dev.vulnlog.gradle.internal.log
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.render.formatFailureMessage
import dev.vulnlog.lib.render.renderFilterProblems
import dev.vulnlog.lib.render.renderFilterResolution
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException

/** Also reports what [request] resolved to, at verbose level. */
fun DefaultTask.resolveFilterOrFail(
    request: FilterRequest,
    files: List<VulnlogFile>,
): ResolvedFilter =
    when (val outcome = resolveFilter(request, files)) {
        is FilterOutcome.Resolved -> {
            renderFilterResolution(outcome.filter).forEach(logger::log)
            outcome.filter
        }

        is FilterOutcome.Rejected -> throw GradleException(formatFailureMessage(renderFilterProblems(outcome.problems)))
    }
