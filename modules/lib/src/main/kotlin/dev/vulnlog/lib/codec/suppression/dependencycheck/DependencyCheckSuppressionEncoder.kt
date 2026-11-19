// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.dependencycheck

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionEncodingProblem
import dev.vulnlog.lib.model.suppression.SuppressionList
import java.time.LocalDate
import kotlin.reflect.KClass

internal object DependencyCheckSuppressionEncoder {
    internal val elementNames: Map<KClass<out VulnId>, String> =
        mapOf(VulnId.Cve::class to "cve", VulnId.Ghsa::class to "vulnerabilityName")

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
                appendLine("    <suppress$until>")
                entry.reason?.let { appendLine("        <notes>${escapeXml(it)}</notes>") }
                // validate() rejects unsupported IDs before encoding begins, even for direct callers.
                val element = requireNotNull(elementName(entry.id))
                appendLine("        <$element>${escapeXml(entry.id.id)}</$element>")
                appendLine("    </suppress>")
            }
            appendLine("</suppressions>")
        }
    }

    private fun elementName(id: VulnId): String? =
        when (id) {
            is VulnId.Cve, is VulnId.Ghsa -> elementNames.getValue(id::class)
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
