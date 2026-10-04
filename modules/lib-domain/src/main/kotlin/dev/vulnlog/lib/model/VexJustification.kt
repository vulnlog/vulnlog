// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

enum class VexJustification(
    val value: String,
) {
    /**
     * The vulnerable component is not included in the deliverable.
     */
    COMPONENT_NOT_PRESENT("Component not present"),

    /**
     * The vulnerability is mitigated by existing controls (e.g., WAF, input validation).
     */
    INLINE_MITIGATIONS_ALREADY_EXIST("Inline mitigations already exist"),

    /**
     * The vulnerable code is reachable but cannot be triggered by an attacker.
     */
    VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY("Vulnerable code cannot be controlled by adversary"),

    /**
     * The vulnerable code is present but cannot be reached during execution.
     */
    VULNERABLE_CODE_NOT_IN_EXECUTE_PATH("Vulnerable code not in execute path"),

    /**
     * The vulnerable code is present but the specific vulnerable function or code path is not included.
     */
    VULNERABLE_CODE_NOT_PRESENT("Vulnerable code not present"),
}
