// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.codec.suppression.SuppressionEncoder
import dev.vulnlog.lib.core.filter.FilterOutcome
import dev.vulnlog.lib.core.filter.FilterRequest
import dev.vulnlog.lib.core.filter.ResolvedFilter
import dev.vulnlog.lib.core.filter.resolveFilter
import dev.vulnlog.lib.core.suppression.buildSuppressionLists
import dev.vulnlog.lib.core.suppression.collectSuppressedVulnerabilities
import dev.vulnlog.lib.core.suppression.genericSuppressionFormat
import dev.vulnlog.lib.core.suppression.nativeSuppressionFormat
import dev.vulnlog.lib.core.suppression.suppressionFileName
import dev.vulnlog.lib.document.validation.ValidVulnlogProject
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.suppression.SuppressionCollection
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import dev.vulnlog.lib.model.suppression.SuppressionList
import java.time.LocalDate

/** The driver reads the clock, so the run itself stays pure. */
data class SuppressionRequest(
    val filter: FilterRequest,
    val format: SuppressionFormatRequest,
    val today: LocalDate,
    /** The driver writes to one file, which holds one reporter's list. */
    val singleFile: Boolean,
)

sealed interface SuppressionOutcome {
    sealed interface Failed : SuppressionOutcome

    /** The run got past the filter, so the messages can tell what it collected, whether files follow or not. */
    sealed interface Collected : SuppressionOutcome {
        val filter: ResolvedFilter
        val collection: SuppressionCollection
    }

    data class SeveralReporters(
        override val filter: ResolvedFilter,
        override val collection: SuppressionCollection,
        val reporters: List<ReporterType>,
    ) : Failed,
        Collected

    /** No reporter in scope has a format, so there is no file to write. */
    data class NothingToSuppress(
        override val filter: ResolvedFilter,
        override val collection: SuppressionCollection,
    ) : Collected

    data class Generated(
        override val filter: ResolvedFilter,
        override val collection: SuppressionCollection,
        val files: List<SuppressionFile>,
    ) : Collected
}

/** The list for the messages, the content for the file. */
data class SuppressionFile(
    val list: SuppressionList,
    val content: String,
) {
    val fileName: String get() = suppressionFileName(list.format)
}

fun generateSuppressions(
    project: ValidVulnlogProject,
    request: SuppressionRequest,
): SuppressionOutcome {
    val vulnlogFile = project.vulnlogProjectFile
    val filter =
        when (val outcome = resolveFilter(request.filter, listOf(vulnlogFile))) {
            is FilterOutcome.Resolved -> outcome.filter
            is FilterOutcome.Rejected -> return FilterRejected(outcome.problems)
        }
    val formats = targetReporters(vulnlogFile, filter).associateWith { formatOf(it, request.format) }
    val collected = collectSuppressedVulnerabilities(vulnlogFile, filter, request.today)
    val built = buildSuppressionLists(collected.included, formats)
    val collection = SuppressionCollection(collected.included, collected.exclusions + built.exclusions)
    return when {
        built.lists.isEmpty() -> SuppressionOutcome.NothingToSuppress(filter, collection)

        request.singleFile && built.lists.size > 1 ->
            SuppressionOutcome.SeveralReporters(filter, collection, formats.filterValues { it != null }.keys.toList())

        else ->
            SuppressionOutcome.Generated(
                filter,
                collection,
                built.lists.map { list -> SuppressionFile(list, SuppressionEncoder.encode(list)) },
            )
    }
}

/** Every reporter the input names, so a reporter with nothing left to suppress gets an empty file. */
private fun targetReporters(
    vulnlogFile: VulnlogFile,
    filter: ResolvedFilter,
): Set<ReporterType> =
    vulnlogFile.vulnerabilities
        .flatMap { it.reports }
        .map { it.reporter }
        .filter { filter.reporter == null || it == filter.reporter }
        .toSet()

private fun formatOf(
    reporter: ReporterType,
    request: SuppressionFormatRequest,
): SuppressionFormat? =
    when (request) {
        SuppressionFormatRequest.Auto -> nativeSuppressionFormat(reporter) ?: genericSuppressionFormat(reporter)
        SuppressionFormatRequest.Generic -> genericSuppressionFormat(reporter)
    }
