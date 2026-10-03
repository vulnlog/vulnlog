// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.document.AddOutcome
import java.nio.file.Path

fun formatAddOutcomeMessage(
    destinationPath: Path,
    outcome: AddOutcome,
): String =
    if (outcome.updated) {
        formatStatus(StatusVerb.UPDATED, "${outcome.vulnId.id} in $destinationPath")
    } else {
        formatStatus(StatusVerb.ADDED, "${outcome.vulnId.id} to $destinationPath")
    }
