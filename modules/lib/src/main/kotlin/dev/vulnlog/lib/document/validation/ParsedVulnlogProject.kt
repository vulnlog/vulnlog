// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document.validation

import dev.vulnlog.lib.document.InputDocument
import dev.vulnlog.lib.document.dto.DtoVersion

data class ParsedVulnlogProject(
    val inputDocument: InputDocument,
    val nodeTree: NodeTreeResult.Valid,
    val validatedDto: DtoVersion,
)
