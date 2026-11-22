// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core

import dev.vulnlog.lib.model.VexJustification

/** The justification as the Vulnlog file writes it. */
fun canonical(justification: VexJustification): String =
    when (justification) {
        VexJustification.COMPONENT_NOT_PRESENT -> "component not present"
        VexJustification.INLINE_MITIGATIONS_ALREADY_EXIST -> "inline mitigations already exist"
        VexJustification.VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY ->
            "vulnerable code cannot be controlled by adversary"
        VexJustification.VULNERABLE_CODE_NOT_IN_EXECUTE_PATH -> "vulnerable code not in execute path"
        VexJustification.VULNERABLE_CODE_NOT_PRESENT -> "vulnerable code not present"
    }
