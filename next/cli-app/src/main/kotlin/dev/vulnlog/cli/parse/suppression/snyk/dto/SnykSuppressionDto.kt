// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.cli.parse.suppression.snyk.dto

data class SnykSuppressionDto(
    val ignore: Map<String, List<Map<String, SnykIgnoreEntryDto>>>,
)
