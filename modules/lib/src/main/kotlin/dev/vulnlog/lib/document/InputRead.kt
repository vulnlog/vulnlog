// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.document

/** [Failed.source] names the input as [InputDocument.source] would: the path as given, or `<stdin>`. */
sealed interface InputRead {
    data class Read(
        val document: InputDocument,
    ) : InputRead

    sealed interface Failed : InputRead {
        val source: String
    }

    data class Missing(
        override val source: String,
    ) : Failed

    data class Denied(
        override val source: String,
    ) : Failed

    /** [reason] is the file system's own wording, such as "Is a directory". */
    data class Unreadable(
        override val source: String,
        val reason: String,
    ) : Failed
}
