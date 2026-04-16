// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.model

import java.time.LocalDate

data class Resolution(
    /**
     * Release in which the resolution was applied. Must reference a release ID from the releases section.
     */
    val release: Release,
    /**
     * Date the resolution was applied.
     */
    val at: LocalDate? = null,
    /**
     * Reference to the issue or ticket tracking the resolution.
     */
    val ref: String? = null,
    /**
     * Brief description of how the vulnerability was resolved.
     */
    val note: String? = null,
)
