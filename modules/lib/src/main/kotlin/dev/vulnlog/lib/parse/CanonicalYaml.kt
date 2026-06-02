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
    /** Default indentation for all YAML blocks. */
    private const val INDENTATION: Int = 2

    /** Strings longer than this many characters are rendered as folded block scalars. */
    const val FOLD_THRESHOLD: Int = 80

    /** Folded/plain scalars are wrapped to fit within this column width. */
    private const val LINE_WIDTH: Int = 80

    /** Settings for a full document: emits the `---` start marker. */
    private val documentSettings: DumpSettings = settings(explicitStart = true)

    /** Settings for a fragment (single entry or section) spliced into existing content. */
    private val fragmentSettings: DumpSettings = settings(explicitStart = false)

    private fun settings(explicitStart: Boolean): DumpSettings =
        DumpSettings
            .builder()
            .setDefaultFlowStyle(FlowStyle.BLOCK)
            .setIndent(INDENTATION)
            .setIndicatorIndent(INDENTATION)
            .setIndentWithIndicator(true)
            .setWidth(LINE_WIDTH)
            .setBestLineBreak("\n")
            .setExplicitStart(explicitStart)
            .build()

    /** Renders a full Vulnlog document, including the `---` start marker. */
    fun renderDocument(
        dto: VulnlogFileV1Dto,
        mapper: ObjectMapper,
    ): String = dump(mapper.convertValue(dto, Map::class.java), documentSettings)

    /** Renders a single vulnerability entry as a top-level mapping (no `---`, no list indicator). */
    fun renderEntry(
        dto: VulnerabilityEntryDto,
        mapper: ObjectMapper,
    ): String = dump(mapper.convertValue(dto, Map::class.java), fragmentSettings)

    /** Renders a single top-level section (e.g. `project:`, `releases:`) as a fragment. */
    fun renderSection(
        key: String,
        value: Any?,
        mapper: ObjectMapper,
    ): String = dump(mapper.convertValue(mapOf(key to value), Map::class.java), fragmentSettings)

    private fun dump(
        tree: Any?,
        settings: DumpSettings,
    ): String = Dump(settings, VulnlogRepresenter(settings)).dumpToString(tree)
}

/**
 * Applies Vulnlog's per-node style rules on top of [StandardRepresenter]: single-element sequences
 * become flow arrays, long or multi-line strings become folded block scalars, and type-coercible
 * strings are double-quoted so they round-trip as strings.
 */
private class VulnlogRepresenter(
    settings: DumpSettings,
) : StandardRepresenter(settings) {
    init {
        representers[String::class.java] = RepresentToNode { data -> representString(data as String) }
        parentClassRepresenters[List::class.java] = RepresentToNode { data -> representList(data as List<*>) }
    }

    private fun representString(value: String): Node {
        val style =
            when {
                value.contains('\n') -> ScalarStyle.FOLDED
                value.length > FOLD_THRESHOLD && value.contains(' ') -> ScalarStyle.FOLDED
                settings.schema.scalarResolver.resolve(value, true) != Tag.STR -> ScalarStyle.DOUBLE_QUOTED
                value.contains(':') -> ScalarStyle.DOUBLE_QUOTED
                else -> ScalarStyle.PLAIN
            }
        return representScalar(Tag.STR, value, style)
    }

    private fun representList(value: List<*>): Node {
        val scalarOnly = value.all { it !is Map<*, *> && it !is List<*> }
        val flowStyle = if (value.size <= 1 && scalarOnly) FlowStyle.FLOW else FlowStyle.BLOCK
        return representSequence(Tag.SEQ, value, flowStyle)
    }
}
