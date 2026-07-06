// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ReporterType
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppress.SuppressedVulnerability
import dev.vulnlog.lib.model.suppress.SuppressionFormat
import dev.vulnlog.lib.model.suppress.SuppressionOutput
import dev.vulnlog.lib.result.SuppressionExclusion
import kotlin.reflect.KClass

/**
 * Renders one diagnostic line for a written suppression output, stating the target, the format,
 * and the number of entries. Shared by the CLI and the Gradle plugin.
 */
fun renderSuppressionWritten(
    target: String,
    output: SuppressionOutput,
): String = "wrote $target: ${formatName(output)} format, ${pluralizeEntries(output.entries.size)}"

/**
 * Renders one diagnostic line per entry included in the suppression outputs, the counterpart of
 * [renderSuppressionExclusion]. Shared by the CLI and the Gradle plugin.
 */
fun renderSuppressionInclusions(included: Map<ReporterType, List<SuppressedVulnerability>>): List<String> =
    included
        .flatMap { (reporter, suppressions) ->
            suppressions.map { suppression ->
                val expiry = suppression.expiresAt?.let { " (expires $it)" } ?: ""
                "included ${suppression.id.canonical()} for reporter ${reporter.canonical()}$expiry"
            }
        }.sorted()

/**
 * Renders one diagnostic line for an entry excluded from a suppression output, stating what was
 * skipped and why. Shared by the CLI and the Gradle plugin.
 */
fun renderSuppressionExclusion(exclusion: SuppressionExclusion): String =
    when (exclusion) {
        is SuppressionExclusion.UnsupportedIdType ->
            "skipped ${exclusion.id.canonical()} for ${exclusion.fileName}: " +
                "the ${formatName(exclusion.format)} format requires ${requiredIdTypes(exclusion.format)} ids"

        is SuppressionExclusion.UnsupportedReporter ->
            "skipped ${exclusion.id.canonical()} for reporter ${exclusion.reporter.canonical()}: " +
                "no suppression format available"

        is SuppressionExclusion.ResolvedVulnerability ->
            "skipped ${exclusion.id.canonical()}: resolved vulnerabilities are not suppressed"

        is SuppressionExclusion.ExpiredSuppression ->
            "skipped ${exclusion.id.canonical()} for reporter ${exclusion.reporter.canonical()}: " +
                "suppression expired on ${exclusion.expiredAt}"
    }

private fun formatName(format: SuppressionFormat): String =
    when (format) {
        is SuppressionFormat.GenericFormat.Generic -> "generic"
        SuppressionFormat.NativeFormat.Trivy -> "trivy"
        SuppressionFormat.NativeFormat.Snyk -> "snyk"
        SuppressionFormat.NativeFormat.CargoAudit -> "cargo-audit"
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

private fun formatName(output: SuppressionOutput): String =
    when (output) {
        is SuppressionOutput.GenericSuppression -> "generic"
        is SuppressionOutput.TrivySuppression -> "trivy"
        is SuppressionOutput.SnykSuppression -> "snyk"
        is SuppressionOutput.CargoAuditSuppression -> "cargo-audit"
    }

private fun pluralizeEntries(count: Int): String = if (count == 1) "1 entry" else "$count entries"
