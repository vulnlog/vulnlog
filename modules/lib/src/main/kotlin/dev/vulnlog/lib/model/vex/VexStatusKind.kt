// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

/** The kind of a [VexStatus], without the text it carries. Declared in the order statements are sorted by. */
enum class VexStatusKind {
    AFFECTED,
    FIXED,
    NOT_AFFECTED,
    UNDER_INVESTIGATION,
}
