// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.render.formatFailureMessage
import dev.vulnlog.lib.render.renderOpenVexFailure
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.VerificationException

/** Mirrors [ExitsKt] for the Gradle plugin */
fun failure(
    failed: OpenVexOutcome.Failed,
    baseline: String,
): GradleException {
    val message = formatFailureMessage(renderOpenVexFailure(failed, baseline, "'baseline'"))
    return when (failed) {
        is FilterRejected -> InvalidUserDataException(message)
        is OpenVexOutcome.BaselineRejected -> InvalidUserDataException(message)
        is OpenVexOutcome.NoStatementApplies -> VerificationException(message)
    }
}
