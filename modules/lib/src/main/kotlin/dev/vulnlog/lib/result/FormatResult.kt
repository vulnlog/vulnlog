// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.result

enum class FormatRule {
    COMMENTS_NOT_PRESERVED,
    NON_CANONICAL_ARRAY_STYLE,
    NON_CANONICAL_BLOCK_SCALAR,
    NON_CANONICAL_DOCUMENT_START,
    NON_CANONICAL_FIELD_ORDER,
    NON_CANONICAL_LAYOUT,
}

/** A single deviation from the canonical style. [path] may be empty for file-level findings. */
data class FormatFinding(
    val rule: FormatRule,
    val path: String,
    val message: String,
)
