// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import dev.vulnlog.lib.render.formatFailureMessage
import dev.vulnlog.lib.render.renderInputFailure
import dev.vulnlog.lib.render.renderOpenVexBaselineFailure
import dev.vulnlog.lib.render.renderOpenVexFailure
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.VerificationException

private const val BASELINE_OPTION = "'baseline'"

/** Mirrors `exitCode` in the CLI's `Exits.kt` for the Gradle plugin. */
fun failure(
    failed: OpenVexOutcome.Failed,
    baseline: String,
): GradleException {
    val message = formatFailureMessage(renderOpenVexFailure(failed, baseline, BASELINE_OPTION))
    return when (failed) {
        is FilterRejected -> InvalidUserDataException(message)
        is OpenVexOutcome.BaselineRejected -> InvalidUserDataException(message)
        is OpenVexOutcome.NoStatementApplies -> VerificationException(message)
    }
}

fun failure(
    unreadable: OpenVexBaselineRead.Unreadable,
    baseline: String,
): GradleException =
    InvalidUserDataException(
        formatFailureMessage(listOf(renderOpenVexBaselineFailure(unreadable, baseline, BASELINE_OPTION))),
    )

fun failure(failed: InputRead.Failed): GradleException {
    val message = formatFailureMessage(listOf(renderInputFailure(failed)))
    return when (failed) {
        is InputRead.Missing, is InputRead.Denied, is InputRead.Unreadable -> InvalidUserDataException(message)
    }
}
