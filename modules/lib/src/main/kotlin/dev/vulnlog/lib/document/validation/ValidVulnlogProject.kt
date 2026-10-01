// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.model.VulnlogFile

/**
 * A [ParsedVulnlogProject] whose domain model also passed the domain rules. Commands that read the vulnerabilities rather than the layout need this stage.
 */
data class ValidVulnlogProject(
    val parsedVulnlogProject: ParsedVulnlogProject,
    val vulnlogProjectFile: VulnlogFile,
) {
    val inputDocument: InputDocument get() = parsedVulnlogProject.inputDocument

    val nodeTree: NodeTreeResult.Valid get() = parsedVulnlogProject.nodeTree
}
