// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.validation

import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.shell.InputDocument

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
