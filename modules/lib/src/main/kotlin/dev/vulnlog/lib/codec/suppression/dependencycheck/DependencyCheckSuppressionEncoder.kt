// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.dependencycheck

import dev.vulnlog.lib.model.suppression.SuppressionEncodingProblem
import dev.vulnlog.lib.model.suppression.SuppressionList
import java.time.LocalDate

internal object DependencyCheckSuppressionEncoder {
    /** XML 1.0 cannot represent these characters, even with numeric character references. */
    fun validate(list: SuppressionList): List<SuppressionEncodingProblem> =
        list.entries.flatMap { entry ->
            buildList {
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
                appendLine("        <cve>${escapeXml(entry.id.id)}</cve>")
                appendLine("    </suppress>")
            }
            appendLine("</suppressions>")
        }
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
