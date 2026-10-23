// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.dependencycheck

import dev.vulnlog.lib.model.VulnId
import dev.vulnlog.lib.model.suppression.SuppressionList

internal object DependencyCheckSuppressionEncoder {
    data class InvalidNote(
        val id: VulnId,
        val codePoint: Int,
    )

    /** XML 1.0 cannot represent these characters, even with numeric character references. */
    fun invalidNote(list: SuppressionList): InvalidNote? =
        list.entries.firstNotNullOfOrNull { entry ->
            val invalid =
                entry.reason
                    ?.codePoints()
                    ?.filter { !isXmlCharacter(it) }
                    ?.findFirst()
                    ?.orElse(-1) ?: -1
            if (invalid == -1) null else InvalidNote(entry.id, invalid)
        }

    fun encode(list: SuppressionList): String {
        require(invalidNote(list) == null) { "Dependency-Check notes must contain only XML 1.0 characters" }
        return buildString {
            appendLine(
                "<suppressions xmlns=\"https://jeremylong.github.io/DependencyCheck/dependency-suppression.1.3.xsd\">",
            )
            list.entries.forEach { entry ->
                val until = entry.expiresAt?.let { " until=\"${it}Z\"" }.orEmpty()
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
