// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.model

/** In `model` like `OpenVexBaselineRead`: `io` produces it, and `render`, which may not import `io`, words it. */
sealed interface OutputWrite {
    data object Written : OutputWrite

    /** Only a write that compares first reports this; every other write replaces the file. */
    data object Unchanged : OutputWrite

    sealed interface Failed : OutputWrite {
        val target: String
    }

    data class MissingDirectory(
        override val target: String,
    ) : Failed

    data class Denied(
        override val target: String,
    ) : Failed

    /** [reason] is the file system's own wording, such as "Is a directory". */
    data class Unwritable(
        override val target: String,
        val reason: String,
    ) : Failed
}
