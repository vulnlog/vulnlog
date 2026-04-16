// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.suppression.generic.dto

data class GenericSuppressionDto(
    val vulnerabilities: List<GenericVulnerabilityEntryDto>,
)
