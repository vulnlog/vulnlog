// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

enum class VexJustification {
    /**
     * The vulnerable component is not included in the deliverable.
     */
    COMPONENT_NOT_PRESENT,

    /**
     * The vulnerability is mitigated by existing controls (e.g., WAF, input validation).
     */
    INLINE_MITIGATIONS_ALREADY_EXIST,

    /**
     * The vulnerable code is reachable but cannot be triggered by an attacker.
     */
    VULNERABLE_CODE_CANNOT_BE_CONTROLLED_BY_ADVERSARY,

    /**
     * The vulnerable code is present but cannot be reached during execution.
     */
    VULNERABLE_CODE_NOT_IN_EXECUTE_PATH,

    /**
     * The vulnerable code is present but the specific vulnerable function or code path is not included.
     */
    VULNERABLE_CODE_NOT_PRESENT,
}
