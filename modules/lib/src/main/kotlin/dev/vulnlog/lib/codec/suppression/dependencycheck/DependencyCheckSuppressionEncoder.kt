// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.dependencycheck

import dev.vulnlog.lib.model.Purl
import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionEncodingProblem
import dev.vulnlog.lib.model.suppression.SuppressionList
import java.time.LocalDate

private const val REGEX_METACHARACTERS = "\\^$.|?*+()[]{}"

internal object DependencyCheckSuppressionEncoder {
    /** Check all entries, including lists constructed directly rather than through format filtering. */
    fun validate(list: SuppressionList): List<SuppressionEncodingProblem> =
        list.entries.flatMap { entry ->
            buildList {
                if (elementName(entry.id) == null) {
                    add(SuppressionEncodingProblem.UnsupportedIdType(list.format, entry.id))
                }
                entry.reason
                    ?.codePoints()
                    ?.filter { !isXmlCharacter(it) }
                    ?.distinct()
                    ?.forEach { add(SuppressionEncodingProblem.UnencodableReason(list.format, entry.id, it)) }
                entry.expiresAt?.takeIf { it == LocalDate.MAX }?.let {
                    add(SuppressionEncodingProblem.UnencodableExpiry(list.format, entry.id, it))
                }
            }
        }

    fun encode(list: SuppressionList): String {
        require(validate(list).isEmpty()) { "Entries cannot be represented as Dependency-Check XML" }
        return buildString {
            appendLine(
                "<suppressions xmlns=\"https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd\">",
            )
            list.entries.forEach { entry ->
                // Vulnlog includes the expiry day; Dependency-Check stops at the start of `until`.
                val until =
                    entry.expiresAt?.let { " until=\"${it.plusDays(1).toString().removePrefix("+")}Z\"" }.orEmpty()
                // validate() rejects unsupported IDs before encoding begins, even for direct callers.
                val element = requireNotNull(elementName(entry.id))
                // The schema allows one packageUrl per suppress element.
                val packageUrls = entry.packages.map(::toPackageUrlElement).ifEmpty { listOf(null) }
                packageUrls.forEach { packageUrl ->
                    appendLine("    <suppress$until>")
                    entry.reason?.let { appendLine("        <notes>${escapeXml(it)}</notes>") }
                    packageUrl?.let { appendLine("        $it") }
                    appendLine("        <$element>${escapeXml(entry.id.id)}</$element>")
                    appendLine("    </suppress>")
                }
            }
            appendLine("</suppressions>")
        }
    }

    /**
     * Dependency-Check compares the whole PURL, so a PURL without a version becomes a pattern that matches every
     * version, as it does in Trivy.
     */
    private fun toPackageUrlElement(purl: Purl): String {
        val value = purl.value
        val coordinatesEnd = value.indexOfAny(charArrayOf('?', '#')).takeIf { it >= 0 } ?: value.length
        val coordinates = value.substring(0, coordinatesEnd)
        // Canonical PURLs encode the @ of an npm scope, so an @ in the last segment separates the version.
        if ('@' in coordinates.substringAfterLast('/')) return "<packageUrl>${escapeXml(value)}</packageUrl>"
        val pattern = escapeRegex(coordinates) + "@[^?#]+" + escapeRegex(value.substring(coordinatesEnd))
        return "<packageUrl regex=\"true\">${escapeXml(pattern)}</packageUrl>"
    }

    private fun escapeRegex(text: String): String =
        buildString {
            text.forEach { char ->
                if (char in REGEX_METACHARACTERS) append('\\')
                append(char)
            }
        }

    private fun elementName(id: VulnId): String? =
        when (id) {
            is VulnId.Cve -> "cve"
            is VulnId.Ghsa -> "vulnerabilityName"
            is VulnId.RustSec, is VulnId.Snyk -> null
        }

    private fun isXmlCharacter(codePoint: Int): Boolean =
        codePoint == 0x9 ||
            codePoint == 0xA ||
            codePoint == 0xD ||
            codePoint in 0x20..0xD7FF ||
            codePoint in 0xE000..0xFFFD ||
            codePoint in 0x10000..0x10FFFF

    private fun escapeXml(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("\r", "&#13;")
}
