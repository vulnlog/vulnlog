// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.reporting.dto

data class ReportDataDto(
    val project: ProjectDataDto,
    val generatedAt: String,
    val vulnlogVersion: String,
    val inputs: List<String>,
    val filter: FilterDataDto,
    val entries: List<ReportEntryDataDto>,
)
