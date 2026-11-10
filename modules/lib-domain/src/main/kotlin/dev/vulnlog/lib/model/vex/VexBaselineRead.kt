// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model.vex

/** What reading the baseline file found. Each caller decides what an unavailable baseline means. */
sealed interface VexBaselineRead {
    data class Present(
        val text: String,
    ) : VexBaselineRead

    sealed interface Unavailable : VexBaselineRead

    data object Absent : Unavailable

    /** [reason] is the file system's own wording, such as "Is a directory". */
    data class Unreadable(
        val reason: String,
    ) : Unavailable
}
