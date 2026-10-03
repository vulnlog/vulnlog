// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.SuppressionFile
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.canonical
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.suppression.suppressionFileName
import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressedVulnerability
import dev.vulnlog.lib.model.suppression.SuppressionExclusion
import dev.vulnlog.lib.model.suppression.SuppressionFormat
import kotlin.reflect.KClass

fun renderSuppressionMessages(outcome: SuppressionOutcome): List<Message> =
    when (outcome) {
        is FilterRejected -> emptyList()

        is SuppressionOutcome.SeveralReporters, is SuppressionOutcome.Generated -> collectedMessages(outcome)

        is SuppressionOutcome.NothingToSuppress ->
            collectedMessages(outcome) +
                Message.Status(formatStatus(StatusVerb.UNCHANGED, "no suppression entries applicable"))
    }

/** The option names come from the driver; only [SuppressionOutcome.SeveralReporters] uses them. */
fun renderSuppressionFailure(
    failed: SuppressionOutcome.Failed,
    outputOption: String,
    reporterOption: String,
    outputDirOption: String,
): List<Failure> =
    when (failed) {
        is FilterRejected -> renderFilterProblems(failed.problems)

        is SuppressionOutcome.SeveralReporters -> {
            val names = failed.reporters.map { it.canonical() }.sorted()
            listOf(
                Failure(
                    "$outputOption requires a single reporter, found: ${names.joinToString(", ")}",
                    "use $reporterOption <name> to pick one, or $outputDirOption for one file per reporter",
                ),
            )
        }
    }

fun renderSuppressionWritten(
    target: String,
    file: SuppressionFile,
): Message =
    Message.Verbose("wrote $target: ${formatName(file.list.format)} format, ${entries(file.list.entries.size)}")

private fun collectedMessages(outcome: SuppressionOutcome.Collected): List<Message> =
    renderFilterResolution(outcome.filter) +
        outcome.collection.exclusions.map { Message.Verbose(exclusionLine(it)) } +
        inclusionLines(outcome.collection.included).map(Message::Debug)

private fun inclusionLines(included: Map<ReporterType, List<SuppressedVulnerability>>): List<String> =
    included
        .flatMap { (reporter, suppressions) ->
            suppressions.map { suppression ->
                val expiry = suppression.expiresAt?.let { " (expires $it)" } ?: ""
                "included ${suppression.id.canonical()} for reporter ${reporter.canonical()}$expiry"
            }
        }.sorted()

private fun exclusionLine(exclusion: SuppressionExclusion): String =
    when (exclusion) {
        is SuppressionExclusion.UnsupportedIdType ->
            "skipped ${exclusion.id.canonical()} for ${suppressionFileName(exclusion.format)}: " +
                "the ${formatName(exclusion.format)} format requires ${requiredIdTypes(exclusion.format)} ids"

        is SuppressionExclusion.UnsupportedReporter ->
            "skipped ${exclusion.id.canonical()} for reporter ${exclusion.reporter.canonical()}: " +
                "no suppression format available"

        is SuppressionExclusion.Resolved ->
            "skipped ${exclusion.id.canonical()}: resolved vulnerabilities are not suppressed"

        is SuppressionExclusion.Expired ->
            "skipped ${exclusion.id.canonical()} for reporter ${exclusion.reporter.canonical()}: " +
                "suppression expired on ${exclusion.expiredAt}"
    }

private fun formatName(format: SuppressionFormat): String =
    when (format) {
        is SuppressionFormat.Generic -> "generic"
        SuppressionFormat.Trivy -> "trivy"
        SuppressionFormat.Snyk -> "snyk"
        SuppressionFormat.CargoAudit -> "cargo-audit"
    }

private fun requiredIdTypes(format: SuppressionFormat): String =
    format.vulnIdTypes.joinToString(" or ") { idTypeName(it) }

private fun idTypeName(type: KClass<out VulnId>): String =
    when (type) {
        VulnId.Cve::class -> "CVE"
        VulnId.Ghsa::class -> "GHSA"
        VulnId.RustSec::class -> "RUSTSEC"
        VulnId.Snyk::class -> "SNYK"
        else -> type.simpleName ?: "unknown"
    }

private fun entries(count: Int): String = if (count == 1) "1 entry" else "$count entries"
