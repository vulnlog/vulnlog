// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.report

import dev.vulnlog.lib.codec.report.dto.ProjectDto
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.reporting.severityOf
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.reporting.Impact
import java.time.Instant
import java.time.temporal.ChronoUnit

/*
 * The fields every report writes the same way, so a tool that reads one report also reads the other.
 */

internal fun toProjectDto(project: Project): ProjectDto =
    ProjectDto(organization = project.organization, name = project.name, author = project.author)

/** Seconds are precise enough to tell runs apart, and the table report shows no more either. */
internal fun formatGeneratedAt(generatedAt: Instant): String = generatedAt.truncatedTo(ChronoUnit.SECONDS).toString()

/**
 * The impact in one word or two, as the readable reports name it: the severity of an entry that is affected, or
 * `not affected`, or `not triaged`.
 */
internal fun labelImpact(impact: Impact): String =
    when (impact) {
        is Impact.Affected -> canonical(impact.severity)
        is Impact.NotAffected -> "not affected"
        Impact.Unknown -> "not triaged"
    }

/** `affected` or `not affected`, and null while untriaged, because the state already says so. */
internal fun labelVerdict(impact: Impact): String? =
    when (impact) {
        is Impact.Affected -> "affected"
        is Impact.NotAffected -> "not affected"
        Impact.Unknown -> null
    }

/** The severity of an entry that is affected. */
internal fun labelSeverity(impact: Impact): String? = severityOf(impact)?.let(::canonical)

/** The justification of an entry that is not affected, as the Vulnlog file writes it. */
internal fun labelJustification(impact: Impact): String? =
    when (impact) {
        is Impact.NotAffected -> canonical(impact.justification)
        is Impact.Affected, Impact.Unknown -> null
    }
