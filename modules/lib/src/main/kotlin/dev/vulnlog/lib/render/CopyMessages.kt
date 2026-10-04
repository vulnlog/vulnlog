// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.CopiedFile
import dev.vulnlog.lib.app.CopyOutcome

/** Before the write; [renderCopied] follows it. */
fun renderCopyMessages(file: CopiedFile): List<Message> =
    listOfNotNull(renderCommentsDropped(file.document.source).takeIf { file.commentsDropped })

fun renderCopied(file: CopiedFile): List<Message> {
    val target = file.document.source
    return listOf(
        Message.Verbose("copied to $target: ${file.ids.joinToString(", ") { it.id }}"),
        Message.Status(formatStatus(StatusVerb.COPIED, "${pluralize(file.ids.size, "entry", "entries")} to $target")),
    )
}

fun renderCopyFailure(failed: CopyOutcome.Failed): List<Failure> =
    when (failed) {
        is CopyOutcome.IdsNotInSource -> {
            val ids = failed.ids.joinToString(", ") { it.id }
            listOf(
                Failure(
                    "vulnerability IDs not found in ${failed.source.source}: $ids",
                    "copy only IDs the source file records",
                ),
            )
        }
    }
