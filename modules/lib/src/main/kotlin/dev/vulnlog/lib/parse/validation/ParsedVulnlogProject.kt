// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.parse.validation

import dev.vulnlog.lib.parse.dto.DtoVersion
import dev.vulnlog.lib.shell.InputDocument

data class ParsedVulnlogProject(
    val inputDocument: InputDocument,
    val nodeTree: NodeTreeResult.Valid,
    val validatedDto: DtoVersion,
)
