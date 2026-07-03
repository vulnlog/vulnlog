// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.ParseValidationVersion
import dev.vulnlog.lib.parse.CanonicalYaml
import dev.vulnlog.lib.parse.FormatSource
import dev.vulnlog.lib.parse.LocatedNode
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.hasSchemaHeader
import dev.vulnlog.lib.parse.hasYamlComments
import dev.vulnlog.lib.parse.lineOf
import dev.vulnlog.lib.parse.mappingKeys
import dev.vulnlog.lib.parse.scalarValueOf
import dev.vulnlog.lib.parse.walkValues
import dev.vulnlog.lib.result.FormatFinding
import dev.vulnlog.lib.result.FormatRule
import dev.vulnlog.lib.result.ParseResult
import org.snakeyaml.engine.v2.common.FlowStyle
import org.snakeyaml.engine.v2.common.ScalarStyle
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.ScalarNode
import org.snakeyaml.engine.v2.nodes.SequenceNode
import tools.jackson.databind.ObjectMapper

/**
 * Explains why [parsed] is not in the canonical style as findings, one per deviation, with the
 * rule set versioned like [validate]. Each rule compares the observed presentation against the same
 * [CanonicalYaml] decision functions the writer uses, so checker and writer cannot disagree.
 * Deviations no rule names (blank lines, indentation, quoting, chomping) fall into a single
 * [FormatRule.NON_CANONICAL_LAYOUT] finding. An empty result means the content is byte-canonical.
 */
fun checkFormat(
    parsed: ParseResult.Ok,
    mapper: ObjectMapper,
): List<FormatFinding> {
    val source = FormatSource(parsed.rawContent, parsed.rootNode)
    val context = FormatCheckContext(source, walkValues(source.root), mapper)
    val findings =
        when (parsed.validationVersion) {
            ParseValidationVersion.V1 -> v1FormatRules
        }.flatMap { rule -> rule(context) }
    return findings.ifEmpty { layoutCatchAll(parsed, mapper) }
}

/** One line per finding, tagged with the kebab-case rule id, e.g. `[non-canonical-array-style]`. */
fun renderFormatFinding(finding: FormatFinding): String {
    val ruleName = finding.rule.name
    val id = ruleName.lowercase().replace('_', '-')
    return if (finding.path.isEmpty()) "[$id] ${finding.message}" else "[$id] ${finding.path}: ${finding.message}"
}

data class FormatCheckContext(
    val source: FormatSource,
    val nodes: List<LocatedNode>,
    val mapper: ObjectMapper,
)

private val v1FormatRules =
    listOf(
        ::checkSchemaHeader,
        ::checkSequenceStyles,
        ::checkEntryFieldOrder,
        ::checkBlockScalarStyles,
        ::checkComments,
    )

private fun checkSchemaHeader(context: FormatCheckContext): List<FormatFinding> {
    val lines =
        context.source.raw.content
            .lines()
    val schemaVersion = scalarValueOf(context.source.root, "schemaVersion") ?: "1"
    val header = YamlWriter.schemaHeader(schemaVersion)
    val hasHeader = hasSchemaHeader(context.source.root)
    val expectedStart = if (hasHeader) listOf(header, "---") else listOf("---")
    if (lines.take(expectedStart.size) == expectedStart) return emptyList()
    return listOf(
        FormatFinding(
            rule = FormatRule.NON_CANONICAL_DOCUMENT_START,
            path = "line 1",
            message =
                if (hasHeader) {
                    "File should start with '$header' followed by '---'."
                } else {
                    "File should start with '---'."
                },
        ),
    )
}

private fun checkSequenceStyles(context: FormatCheckContext): List<FormatFinding> =
    context.nodes.mapNotNull { located ->
        val node = located.node as? SequenceNode ?: return@mapNotNull null
        val scalarItemsOnly = node.value.all { it is ScalarNode }
        val canonical = CanonicalYaml.canonicalFlowStyle(node.value.size, scalarItemsOnly)
        if (node.flowStyle == canonical) return@mapNotNull null
        val style = if (canonical == FlowStyle.FLOW) "a flow array, e.g. key: [value]" else "a block list"
        FormatFinding(
            rule = FormatRule.NON_CANONICAL_ARRAY_STYLE,
            path = located.path,
            message = "Line ${lineOf(node)}: canonical style for this list is $style.",
        )
    }

private fun checkEntryFieldOrder(context: FormatCheckContext): List<FormatFinding> {
    val canonicalOrder = CanonicalYaml.canonicalEntryFieldOrder(context.mapper)
    return context.nodes
        .filter { it.path.startsWith("vulnerabilities[") && '.' !in it.path && it.node is MappingNode }
        .mapNotNull { located ->
            val known = mappingKeys(located.node as MappingNode).filter { it in canonicalOrder }
            val expected = canonicalOrder.filter { it in known.toSet() }
            if (known == expected) return@mapNotNull null
            val misplaced = known.zip(expected).firstOrNull { (got, want) -> got != want }?.first ?: known.first()
            FormatFinding(
                rule = FormatRule.NON_CANONICAL_FIELD_ORDER,
                path = located.path,
                message =
                    "Line ${lineOf(located.node)}: '$misplaced' is misplaced; " +
                        "canonical field order is ${expected.joinToString()}.",
            )
        }
}

private fun checkBlockScalarStyles(context: FormatCheckContext): List<FormatFinding> {
    val blockStyles = setOf(ScalarStyle.LITERAL, ScalarStyle.FOLDED)
    return context.nodes.mapNotNull { located ->
        val node = located.node as? ScalarNode ?: return@mapNotNull null
        val canonical = CanonicalYaml.canonicalScalarStyle(node.value)
        if (node.scalarStyle == canonical) return@mapNotNull null
        if (canonical !in blockStyles && node.scalarStyle !in blockStyles) return@mapNotNull null
        FormatFinding(
            rule = FormatRule.NON_CANONICAL_BLOCK_SCALAR,
            path = located.path,
            message =
                "Line ${lineOf(node)}: canonical style for this value is ${styleName(canonical)} " +
                    "(found ${styleName(node.scalarStyle)}).",
        )
    }
}

private fun styleName(style: ScalarStyle): String =
    when (style) {
        ScalarStyle.LITERAL -> "a literal block (|-)"
        ScalarStyle.FOLDED -> "a folded block (>-)"
        ScalarStyle.DOUBLE_QUOTED -> "double-quoted"
        ScalarStyle.SINGLE_QUOTED -> "single-quoted"
        else -> "plain"
    }

private fun checkComments(context: FormatCheckContext): List<FormatFinding> =
    if (hasYamlComments(context.source.root)) {
        listOf(
            FormatFinding(
                rule = FormatRule.COMMENTS_NOT_PRESERVED,
                path = "",
                message = "YAML comments are removed on write.",
            ),
        )
    } else {
        emptyList()
    }

private fun layoutCatchAll(
    parsed: ParseResult.Ok,
    mapper: ObjectMapper,
): List<FormatFinding> =
    if (formatYaml(parsed, mapper) == parsed.rawContent) {
        emptyList()
    } else {
        listOf(
            FormatFinding(
                rule = FormatRule.NON_CANONICAL_LAYOUT,
                path = "",
                message = "Layout differs from the canonical style (blank lines, indentation or quoting).",
            ),
        )
    }
