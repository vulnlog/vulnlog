// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.internal

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import dev.vulnlog.lib.render.formatFailureMessage
import dev.vulnlog.lib.render.renderFilterProblems
import dev.vulnlog.lib.render.renderInputFailure
import dev.vulnlog.lib.render.renderOpenVexBaselineFailure
import dev.vulnlog.lib.render.renderOpenVexFailure
import dev.vulnlog.lib.render.renderProjectsDiffer
import dev.vulnlog.lib.render.renderWriteFailure
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

/** A rejected filter is still a plain [GradleException] here, unlike in OpenVEX: the types are not settled yet. */
fun failure(failed: SuppressionOutcome.Failed): GradleException =
    when (failed) {
        is FilterRejected -> GradleException(formatFailureMessage(renderFilterProblems(failed.problems)))

        // The task writes one file per reporter, so it never asks for a single file.
        is SuppressionOutcome.SeveralReporters -> error("vulnlogSuppress never targets a single file")
    }

/** Plain [GradleException]s here, unlike in OpenVEX: the types are not settled yet. */
fun failure(failed: ImpactReportOutcome.Failed): GradleException =
    when (failed) {
        is ProjectsDiffer -> GradleException(formatFailureMessage(listOf(renderProjectsDiffer(failed))))
        is FilterRejected -> GradleException(formatFailureMessage(renderFilterProblems(failed.problems)))
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

fun failure(failed: OutputWrite.Failed): GradleException {
    val message = formatFailureMessage(listOf(renderWriteFailure(failed)))
    return when (failed) {
        is OutputWrite.MissingDirectory, is OutputWrite.Denied, is OutputWrite.Unwritable ->
            InvalidUserDataException(message)
    }
}
