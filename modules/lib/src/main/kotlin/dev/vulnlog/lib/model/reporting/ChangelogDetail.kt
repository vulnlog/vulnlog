// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.reporting

/** How much of a changelog entry is rendered. */
enum class ChangelogDetail {
    /** Identifiers, severity, description, and how the vulnerability was resolved. */
    FULL,

    /** Identifiers and severity only. */
    BRIEF,
}
