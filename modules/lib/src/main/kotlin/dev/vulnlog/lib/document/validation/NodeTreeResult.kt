// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.model.finding.ParseFailure
import org.snakeyaml.engine.v2.nodes.MappingNode

/** Well-formed YAML with a mapping at the root. */
sealed interface NodeTreeResult {
    data class Valid(
        val rootNode: MappingNode,
    ) : NodeTreeResult

    data class Rejected(
        val problems: List<ParseFailure>,
    ) : NodeTreeResult
}
