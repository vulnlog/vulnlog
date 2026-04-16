// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.lib.parse.suppression.trivy.dto

data class TrivySuppressionDto(
    val vulnerabilities: List<TrivyVulnerabilityEntryDto>,
)
