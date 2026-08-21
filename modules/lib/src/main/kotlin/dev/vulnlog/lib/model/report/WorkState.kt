// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.report

/** Represent the state of a vulnerability entry. */
enum class WorkState {
    UNDER_INVESTIGATION,
    OPEN,
    RESOLVED,
    DISMISSED,
}
