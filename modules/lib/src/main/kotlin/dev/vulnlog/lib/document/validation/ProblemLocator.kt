// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.yaml.scalarValueOf
import dev.vulnlog.lib.document.yaml.valueNodeOf
import dev.vulnlog.lib.document.yaml.walkValues
import dev.vulnlog.lib.finding.FailureLocation
import dev.vulnlog.lib.finding.ParseFailure
import org.snakeyaml.engine.v2.exceptions.MarkedYamlEngineException
import org.snakeyaml.engine.v2.nodes.MappingNode
import org.snakeyaml.engine.v2.nodes.Node
import org.snakeyaml.engine.v2.nodes.SequenceNode
import tools.jackson.core.JacksonException
import kotlin.jvm.optionals.getOrNull

fun locationOf(e: MarkedYamlEngineException): FailureLocation? =
    e.problemMark.getOrNull()?.let { FailureLocation(it.line + 1, it.column + 1) }

fun locationOf(node: Node): FailureLocation? =
    node.startMark.getOrNull()?.let { FailureLocation(it.line + 1, it.column + 1) }

/** The entry path uses the domain rules' spelling, so binding and rule findings read alike. */
internal fun failureAt(
    root: MappingNode,
    references: List<JacksonException.Reference>,
    message: String,
): ParseFailure {
    var node: Node = root
    val path = StringBuilder()
    for (reference in references) {
        val propertyName = reference.propertyName
        when {
            propertyName != null -> {
                if (path.isNotEmpty()) path.append('.')
                path.append(propertyName)
                node = (node as? MappingNode)?.let { valueNodeOf(it, propertyName) } ?: break
            }

            reference.index >= 0 -> {
                val item = (node as? SequenceNode)?.value?.getOrNull(reference.index)
                path.append('[').append((item as? MappingNode)?.let { scalarValueOf(it, "id") } ?: reference.index)
                path.append(']')
                node = item ?: break
            }

            else -> break
        }
    }
    return ParseFailure(message, path.toString().ifEmpty { null }, locationOf(node))
}

/** A failure whose entry path [root] lacks ends up without a location. */
fun locateFailures(
    root: MappingNode,
    failures: List<ParseFailure>,
): List<ParseFailure> {
    val nodesByPath = walkValues(root).associate { it.path to it.node }
    return failures.map { failure ->
        failure.copy(location = failure.path?.let { nodesByPath[it] }?.let(::locationOf))
    }
}
