// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse

import dev.vulnlog.lib.parse.CanonicalYaml.FOLD_THRESHOLD
import dev.vulnlog.lib.parse.v1.dto.VulnerabilityEntryDto
import dev.vulnlog.lib.parse.v1.dto.VulnlogFileV1Dto
import org.snakeyaml.engine.v2.api.Dump
import org.snakeyaml.engine.v2.api.DumpSettings
import org.snakeyaml.engine.v2.api.RepresentToNode
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.Tag
import org.snakeyaml.engine.v2.representer.StandardRepresenter
import tools.jackson.databind.ObjectMapper

/**
 * Renders Vulnlog DTOs to the canonical YAML style:
 *  - stable field order, taken from DTO declaration order
 *  - single-element lists as flow arrays, e.g. `releases: [ "0.11.0" ]`
 *  - strings longer than [FOLD_THRESHOLD] (or containing newlines) as folded block scalars (`>`)
 *  - minimal, type-safe quoting: unquoted where the value round-trips as a string, double-quoted
 *    only to preserve type (e.g. `schemaVersion: "1"`, which would otherwise parse as a number)
 *
 * Jackson converts the DTO to an ordered Map/List/scalar tree (reusing `@JsonProperty` renames,
 * `@JsonInclude` omission and `@JsonFormat` date formatting); snakeyaml-engine then dumps that tree
 * with a [VulnlogRepresenter] that applies the per-node flow and scalar-style rules.
 */
object CanonicalYaml {
    /** Indentation (spaces) for YAML blocks; entry splicing derives its list-item indent from this. */
    const val INDENTATION: Int = 2

    /** Folded/plain scalars are wrapped to fit within this column width. */
    private const val LINE_WIDTH: Int = 80

    /**
     * Headroom for the widest `key:` prefix a value sits behind (e.g. `    description: `). Folding spaced
     * strings beyond [LINE_WIDTH] minus this headroom stops the emitter from wrapping a plain scalar across
     * lines, so every break carries a `>`/`|` indicator instead of a bare wrapped plain scalar.
     */
    private const val KEY_PREFIX_HEADROOM: Int = 17

    /** Spaced strings longer than this become folded block scalars; space-less tokens (purls, ids) never do. */
    const val FOLD_THRESHOLD: Int = LINE_WIDTH - KEY_PREFIX_HEADROOM

    /** Settings for a full document: emits the `---` start marker. */
    private val documentSettings: DumpSettings = settings(explicitStart = true)

    /** Settings for a fragment (single entry or section) spliced into existing content. */
    private val fragmentSettings: DumpSettings = settings(explicitStart = false)

    private fun settings(explicitStart: Boolean): DumpSettings =
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
            // Preferred max column; plain and folded scalars are wrapped to stay within it (best-effort, not hard).
            .setWidth(LINE_WIDTH)
            // Line separator written between lines: force LF instead of the platform default.
            .setBestLineBreak("\n")
            // Emit the leading "---" document-start marker only when true.
            .setExplicitStart(explicitStart)
            .build()

    /** Renders a full Vulnlog document, including the `---` start marker. */
    fun renderDocument(
        dto: VulnlogFileV1Dto,
        mapper: ObjectMapper,
        preservedStyles: Map<String, BlockScalarStyle> = emptyMap(),
    ): String = dump(mapper.convertValue(dto, Map::class.java), documentSettings, preservedStyles)

    /** Renders a single vulnerability entry as a top-level mapping (no `---`, no list indicator). */
    fun renderEntry(
        dto: VulnerabilityEntryDto,
        mapper: ObjectMapper,
        preservedStyles: Map<String, BlockScalarStyle> = emptyMap(),
    ): String = dump(mapper.convertValue(dto, Map::class.java), fragmentSettings, preservedStyles)

    /** Renders a single top-level section (e.g. `project:`, `releases:`) as a fragment. */
    fun renderSection(
        key: String,
        value: Any?,
        mapper: ObjectMapper,
        preservedStyles: Map<String, BlockScalarStyle> = emptyMap(),
    ): String = dump(mapper.convertValue(mapOf(key to value), Map::class.java), fragmentSettings, preservedStyles)

    private fun dump(
        tree: Any?,
        settings: DumpSettings,
        preservedStyles: Map<String, BlockScalarStyle>,
    ): String = Dump(settings, VulnlogRepresenter(settings, preservedStyles)).dumpToString(tree)
}

/**
 * Applies Vulnlog's per-node style rules on top of [StandardRepresenter]: single-element sequences
 * become flow arrays, long or multi-line strings become folded block scalars, and type-coercible
 * strings are double-quoted so they round-trip as strings. A string whose value matches an entry in
 * [preservedStyles] keeps that source block style (`|`/`>`) instead of the default folded one.
 */
private class VulnlogRepresenter(
    settings: DumpSettings,
    private val preservedStyles: Map<String, BlockScalarStyle>,
) : StandardRepresenter(settings) {
    init {
        representers[String::class.java] = RepresentToNode { data -> representString(data as String) }
        parentClassRepresenters[List::class.java] = RepresentToNode { data -> representList(data as List<*>) }
    }

    private fun representString(rawValue: String): Node {
        val value = rawValue.trim()
        preservedStyles[value]?.let { return representBlock(value, it) }
        return when {
            value.contains('\n') -> representBlock(value, BlockScalarStyle.FOLDED_STRIP)
            value.length > FOLD_THRESHOLD && value.contains(' ') -> representBlock(value, BlockScalarStyle.FOLDED_STRIP)
            settings.schema.scalarResolver.resolve(
                value,
                true,
            ) != Tag.STR -> representScalar(Tag.STR, value, ScalarStyle.DOUBLE_QUOTED)
            value.contains(':') -> representScalar(Tag.STR, value, ScalarStyle.DOUBLE_QUOTED)
            else -> representScalar(Tag.STR, value, ScalarStyle.PLAIN)
        }
    }

    private fun representBlock(
        value: String,
        style: BlockScalarStyle,
    ): Node = representScalar(Tag.STR, value.trimEnd('\n') + if (style.clip) "\n" else "", style.scalarStyle)

    private fun representList(value: List<*>): Node {
        val scalarOnly = value.all { it !is Map<*, *> && it !is List<*> }
        val flowStyle = if (value.size <= 1 && scalarOnly) FlowStyle.FLOW else FlowStyle.BLOCK
        return representSequence(Tag.SEQ, value, flowStyle)
    }
}
