// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.yaml

import dev.vulnlog.lib.document.dto.ReportEntryDto
import dev.vulnlog.lib.document.dto.ResolutionDto
import dev.vulnlog.lib.document.dto.VulnerabilityEntryDto
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.RepresentToNode
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.Tag
import org.snakeyaml.engine.v2.representer.StandardRepresenter
import java.time.LocalDate

/**
 * Every YAML file Vulnlog writes (init, fmt, add, copy, the suppression files) is emitted here, so all share one
 * style. The style is a function of the value only; the presentation found in the source is not consulted.
 */
object CanonicalYaml {
    const val INDENTATION: Int = 2

    private const val LINE_WIDTH: Int = 120

    /** Room for the widest key prefix (`    description: `), so a plain value below [FOLD_THRESHOLD] fits its line. */
    private const val KEY_PREFIX_HEADROOM: Int = 17

    const val FOLD_THRESHOLD: Int = LINE_WIDTH - KEY_PREFIX_HEADROOM

    private val settings: DumpSettings =
        DumpSettings
            .builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(INDENTATION)
            .setIndicatorIndent(INDENTATION)
            // List items nest under their key.
            .setIndentWithIndicator(true)
            .setWidth(LINE_WIDTH)
            .setBestLineBreak("\n")
            .build()

    fun renderDocument(dto: Any): String = "---\n" + dump(dtoMapper.convertValue(dto, Map::class.java))

    fun renderEntry(dto: VulnerabilityEntryDto): String = dump(dtoMapper.convertValue(dto, Map::class.java))

    fun renderSection(
        key: String,
        value: Any?,
    ): String = dump(dtoMapper.convertValue(mapOf(key to value), Map::class.java))

    fun renderEntryListItem(dto: VulnerabilityEntryDto): String {
        val lines =
            renderEntry(dto)
                .lines()
                .dropWhile { it.isBlank() }
                .dropLastWhile { it.isBlank() }

        val itemPrefix = " ".repeat(INDENTATION) + "- "
        val continuationPrefix = " ".repeat(INDENTATION + 2)
        return lines
            .mapIndexed { index, line ->
                if (index == 0) "$itemPrefix$line" else "$continuationPrefix$line"
            }.joinToString("\n")
    }

    /** Shared by the emitter and the format checker, so `fmt --check` and a rewrite agree. */
    fun canonicalScalarStyle(rawValue: String): ScalarStyle {
        val value = rawValue.trim()
        return when {
            value.contains('\n') -> ScalarStyle.LITERAL
            value.length > FOLD_THRESHOLD && value.contains(' ') -> ScalarStyle.FOLDED
            settings.schema.scalarResolver.resolve(value, true) != Tag.STR -> ScalarStyle.DOUBLE_QUOTED
            value.contains(':') -> ScalarStyle.DOUBLE_QUOTED
            else -> ScalarStyle.PLAIN
        }
    }

    fun canonicalFlowStyle(
        itemCount: Int,
        scalarItemsOnly: Boolean,
    ): FlowStyle = if (itemCount <= 1 && scalarItemsOnly) FlowStyle.FLOW else FlowStyle.BLOCK

    /** Derived from the DTO's declaration order through a fully populated sample, so it cannot drift. */
    fun canonicalEntryFieldOrder(): List<String> =
        dtoMapper.convertValue(SAMPLE_FULL_ENTRY, Map::class.java).keys.map { it.toString() }

    private val SAMPLE_FULL_ENTRY =
        VulnerabilityEntryDto(
            id = "CVE-0000-0000",
            name = "n",
            description = "d",
            aliases = listOf("GHSA-0000-0000-0000"),
            releases = listOf("0"),
            packages = listOf("p"),
            reports = listOf(ReportEntryDto(reporter = "trivy")),
            tags = listOf("t"),
            analysis = "a",
            analyzedAt = LocalDate.EPOCH,
            verdict = "v",
            severity = "s",
            justification = "j",
            resolution = ResolutionDto(release = "0"),
            comment = "c",
        )

    private fun dump(tree: Any?): String = Dump(settings, VulnlogRepresenter(settings)).dumpToString(tree)
}

private class VulnlogRepresenter(
    settings: DumpSettings,
) : StandardRepresenter(settings) {
    init {
        representers[String::class.java] = RepresentToNode { data -> representString(data as String) }
        parentClassRepresenters[List::class.java] = RepresentToNode { data -> representList(data as List<*>) }
    }

    private fun representString(rawValue: String): Node {
        val value = rawValue.trim()
        return representScalar(Tag.STR, value, CanonicalYaml.canonicalScalarStyle(value))
    }

    private fun representList(value: List<*>): Node {
        val scalarOnly = value.all { it !is Map<*, *> && it !is List<*> }
        return representSequence(Tag.SEQ, value, CanonicalYaml.canonicalFlowStyle(value.size, scalarOnly))
    }
}
