// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.model.report

import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.VulnId

data class ReportingEntry(
    val state: WorkState,
    val primaryId: VulnId,
    val ids: Set<VulnId>,
    val shortDescription: String?,
    val impact: Impact,
    val analysis: String?,
    val reportFor: Set<Release>,
    val fixedIn: Set<Release>,
)
