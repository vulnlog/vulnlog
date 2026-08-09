// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.core.validation

data class ValidationConfig(
    /** Treat warnings as errors, so a warning stops the run. */
    val strict: Boolean = false,
)
