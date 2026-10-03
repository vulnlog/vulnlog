// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.render

import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.pluralize
import dev.vulnlog.lib.finding.FindingSeverity
import dev.vulnlog.lib.model.VulnId
import java.nio.file.Path

fun formatVulnIdsNotInSourceMessage(missing: Set<VulnId>): String =
    formatMessage(
        FindingSeverity.ERROR,
        "vulnerability IDs not found in source file: ${missing.joinToString(", ") { it.id }}",
    )

fun formatCopiedMessage(
    destinationPath: Path,
    ids: List<VulnId>,
): String =
    if (ids.isEmpty()) {
        formatStatus(StatusVerb.UNCHANGED, "$destinationPath: no new vulnerabilities")
    } else {
        formatStatus(StatusVerb.COPIED, "${pluralize(ids.size, "entry", "entries")} to $destinationPath")
    }
