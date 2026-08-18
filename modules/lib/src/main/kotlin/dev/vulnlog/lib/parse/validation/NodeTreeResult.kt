// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.validation

import dev.vulnlog.lib.model.finding.ParseFailure
import org.snakeyaml.engine.v2.nodes.MappingNode

/** Whether the input text is well-formed YAML with a mapping at the root. */
sealed interface NodeTreeResult {
    data class Valid(
        val rootNode: MappingNode,
    ) : NodeTreeResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : NodeTreeResult
}
