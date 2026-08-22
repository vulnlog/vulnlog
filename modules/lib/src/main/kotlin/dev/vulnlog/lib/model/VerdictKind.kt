// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

/** Discriminates the [Verdict] variants without their payload, so a verdict can be filtered on. */
enum class VerdictKind {
    UNDER_INVESTIGATION,
    AFFECTED,
    NOT_AFFECTED,
}
