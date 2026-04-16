// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.reporting.dto

data class ReportDataDto(
    val project: ProjectDataDto,
    val generatedAt: String,
    val entries: List<ReportEntryDataDto>,
)
