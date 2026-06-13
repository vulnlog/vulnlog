// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.parse.CanonicalYaml.FOLD_THRESHOLD
import dev.vulnlog.lib.parse.v1.dto.ReportEntryDto
import dev.vulnlog.lib.parse.v1.dto.ResolutionDto
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.RepresentToNode
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.Tag
import org.snakeyaml.engine.v2.representer.StandardRepresenter
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

/**
 * Renders Vulnlog DTOs to the canonical YAML style:
 *  - stable field order, taken from DTO declaration order
 *  - single-element lists as flow arrays, e.g. `releases: [0.11.0]`
 *  - multi-line strings as literal block scalars (`|-`)
 *  - single-line strings longer than [FOLD_THRESHOLD] as folded block scalars (`>-`); shorter
 *    strings stay plain on a single line, never wrapped
 *  - minimal, type-safe quoting: unquoted where the value round-trips as a string, double-quoted
 *    only to preserve type (e.g. `schemaVersion: "1"`, which would otherwise parse as a number)
 *
 * The style is a pure function of the value: presentation found in the source file is not consulted.
 *
 * Jackson converts the DTO to an ordered Map/List/scalar tree (reusing `@JsonProperty` renames,
 * `@JsonInclude` omission and `@JsonFormat` date formatting); snakeyaml-engine then dumps that tree
 * with a [VulnlogRepresenter] that applies the per-node flow and scalar-style rules.
 */
object CanonicalYaml {
    /** Indentation (spaces) for YAML blocks; entry list items derive their indent from this. */
    const val INDENTATION: Int = 2

    /** Lines are kept within this column width; folded block content wraps here. */
    private const val LINE_WIDTH: Int = 120

    /**
     * Headroom for the widest `key:` prefix a prose value sits behind (e.g. `    description: `).
     * Folding spaced strings beyond [LINE_WIDTH] minus this headroom guarantees a plain scalar
     * always fits its line, so plain values are never wrapped bare across lines.
     */
    private const val KEY_PREFIX_HEADROOM: Int = 17

    /**
     * Spaced strings longer than this become folded block scalars (`>-`); shorter ones stay plain
     * on a single line. Space-less tokens (purls, ids) never fold.
     */
    const val FOLD_THRESHOLD: Int = LINE_WIDTH - KEY_PREFIX_HEADROOM

    private val settings: DumpSettings =
        DumpSettings
            .builder()
            // Default style for collections without an explicit style: indented block, not inline flow ({ }/[ ]).
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            // Spaces added per nesting level for block mappings and sequences.
            .setIndent(INDENTATION)
            // Spaces the block-sequence "-" indicator is indented from its parent key.
            .setIndicatorIndent(INDENTATION)
            // Stack indicatorIndent on top of the block indent (vs. absorbing it), so list items nest under their key.
            .setIndentWithIndicator(true)
            // Preferred max column; folded block content wraps here. FOLD_THRESHOLD keeps plain
            // values short enough that they never hit this limit, so they stay on one line.
            .setWidth(LINE_WIDTH)
            // Line separator written between lines: force LF instead of the platform default.
            .setBestLineBreak("\n")
            .build()

    /** Renders a single vulnerability entry as a top-level mapping (no list indicator). */
    fun renderEntry(
        dto: VulnerabilityEntryDto,
        mapper: ObjectMapper,
    ): String = dump(mapper.convertValue(dto, Map::class.java))

    /** Renders a single top-level section (e.g. `project:`, `releases:`) as a fragment. */
    fun renderSection(
        key: String,
        value: Any?,
        mapper: ObjectMapper,
    ): String = dump(mapper.convertValue(mapOf(key to value), Map::class.java))

    /**
     * Renders a vulnerability entry as a `vulnerabilities:` list item: the first line gets a `-` list
     * indicator and the remaining lines are indented to sit under it.
     */
    fun renderEntryListItem(
        dto: VulnerabilityEntryDto,
        mapper: ObjectMapper,
    ): String {
        val lines =
            renderEntry(dto, mapper)
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

    /**
     * The canonical scalar style for [rawValue]: multi-line values are literal blocks, long spaced
     * single-line values are folded blocks, type-coercible or colon-bearing values are double-quoted,
     * everything else is plain. Single source of truth for the emitter and the format checker.
     */
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

    /** The canonical sequence style: at most one scalar element renders flow, everything else block. */
    fun canonicalFlowStyle(
        itemCount: Int,
        scalarItemsOnly: Boolean,
    ): FlowStyle = if (itemCount <= 1 && scalarItemsOnly) FlowStyle.FLOW else FlowStyle.BLOCK

    /** The canonical key order of a vulnerability entry, derived from [VulnerabilityEntryDto]. */
    fun canonicalEntryFieldOrder(mapper: ObjectMapper): List<String> =
        mapper.convertValue(SAMPLE_FULL_ENTRY, Map::class.java).keys.map { it.toString() }

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

/**
 * Applies Vulnlog's per-node style rules on top of [StandardRepresenter]. The style decisions live
 * in [CanonicalYaml.canonicalScalarStyle] and [CanonicalYaml.canonicalFlowStyle], shared with the
 * format checker; this class only maps them onto nodes.
 */
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
