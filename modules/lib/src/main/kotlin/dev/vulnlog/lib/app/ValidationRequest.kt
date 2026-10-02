// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.app

import dev.vulnlog.lib.core.validation.ValidationConfig
import dev.vulnlog.lib.finding.ERRORS_ONLY
import dev.vulnlog.lib.finding.FindingSeverity

/** A command decides the policy and the severities it shows at once, but only [config] reaches the core. */
data class ValidationRequest(
    val config: ValidationConfig = ValidationConfig(),
    val reportedSeverities: Set<FindingSeverity> = ERRORS_ONLY,
)
