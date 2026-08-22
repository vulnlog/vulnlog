// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.report

/** Represent the state of a vulnerability entry. Declared in lifecycle order. */
enum class WorkState {
    UNDER_INVESTIGATION,
    OPEN,
    ACCEPTED,
    RESOLVED,
    NOT_APPLICABLE,
}
