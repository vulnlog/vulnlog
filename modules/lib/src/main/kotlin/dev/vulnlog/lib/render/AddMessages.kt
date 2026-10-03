// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.app.AddOutcome
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus

/** Before the write; [renderAddStatus] follows it. */
fun renderAddMessages(outcome: AddOutcome.Written): List<Message> =
    listOfNotNull(renderCommentsDropped(outcome.document.source).takeIf { outcome.commentsDropped })

fun renderAddStatus(outcome: AddOutcome.Written): Message {
    val id = outcome.vulnId.id
    val target = outcome.document.source
    return when (outcome) {
        is AddOutcome.Added -> Message.Status(formatStatus(StatusVerb.ADDED, "$id to $target"))
        is AddOutcome.Updated -> Message.Status(formatStatus(StatusVerb.UPDATED, "$id in $target"))
    }
}

fun renderAddFailure(failed: AddOutcome.Failed): List<Failure> =
    when (failed) {
        is AddOutcome.UnknownReferences -> {
            val target = failed.document.source
            listOfNotNull(
                failed.releases.takeIf { it.isNotEmpty() }?.let { releases ->
                    Failure(
                        "$target: releases not defined in the file: ${releases.joinToString(", ") { it.value }}",
                        "declare them under 'releases' in the file first",
                    )
                },
                failed.tags.takeIf { it.isNotEmpty() }?.let { tags ->
                    Failure(
                        "$target: tags not defined in the file: ${tags.joinToString(", ") { it.value }}",
                        "declare them under 'tags' in the file first",
                    )
                },
            )
        }
    }
