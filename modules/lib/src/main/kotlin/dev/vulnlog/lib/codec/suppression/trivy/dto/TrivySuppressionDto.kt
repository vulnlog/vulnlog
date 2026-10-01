// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.codec.suppression.trivy.dto

data class TrivySuppressionDto(
    val vulnerabilities: List<TrivyVulnerabilityEntryDto>,
)
