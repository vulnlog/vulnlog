// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.lib.shell

import dev.vulnlog.lib.core.validation.ValidationConfig
import dev.vulnlog.lib.model.finding.ERRORS_ONLY
import dev.vulnlog.lib.model.finding.FindingSeverity

/**
 * What a command asks of a validation run: the policy the core applies, and the severities the
 * shell puts in front of the user. The two travel together because a command decides both at once,
 * but only [config] reaches the core.
 */
data class ValidationRequest(
    val config: ValidationConfig = ValidationConfig(),
    val reportedSeverities: Set<FindingSeverity> = ERRORS_ONLY,
)
