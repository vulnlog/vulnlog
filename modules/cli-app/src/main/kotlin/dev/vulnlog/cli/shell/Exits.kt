// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import dev.vulnlog.lib.app.ChangelogOutcome
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.ImpactReportOutcome
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.app.ProjectsDiffer
import dev.vulnlog.lib.app.SuppressionOutcome
import dev.vulnlog.lib.document.InputRead
import dev.vulnlog.lib.model.OutputWrite
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead

/** Mirrors [Outcomes] for the CLI */
fun exitCode(failed: OpenVexOutcome.Failed): ExitCode =
    when (failed) {
        is FilterRejected -> ExitCode.INVALID_FLAG_VALUE
        is OpenVexOutcome.BaselineRejected -> ExitCode.INVALID_FLAG_VALUE
        is OpenVexOutcome.NoStatementApplies -> ExitCode.VALIDATION_ERROR
    }

fun exitCode(failed: SuppressionOutcome.Failed): ExitCode =
    when (failed) {
        is FilterRejected -> ExitCode.INVALID_FLAG_VALUE
        is SuppressionOutcome.SeveralReporters -> ExitCode.GENERAL_ERROR
    }

fun exitCode(failed: ImpactReportOutcome.Failed): ExitCode =
    when (failed) {
        is ProjectsDiffer -> ExitCode.VALIDATION_ERROR
        is FilterRejected -> ExitCode.INVALID_FLAG_VALUE
    }

fun exitCode(failed: ChangelogOutcome.Failed): ExitCode =
    when (failed) {
        is ProjectsDiffer -> ExitCode.VALIDATION_ERROR
        is FilterRejected -> ExitCode.INVALID_FLAG_VALUE
    }

fun exitCode(unavailable: OpenVexBaselineRead.Unavailable): ExitCode =
    when (unavailable) {
        OpenVexBaselineRead.Absent -> ExitCode.INVALID_FLAG_VALUE
        is OpenVexBaselineRead.Unreadable -> ExitCode.GENERAL_ERROR
    }

fun exitCode(failed: InputRead.Failed): ExitCode =
    when (failed) {
        is InputRead.Missing, is InputRead.Denied, is InputRead.Unreadable -> ExitCode.GENERAL_ERROR
    }

fun exitCode(failed: OutputWrite.Failed): ExitCode =
    when (failed) {
        is OutputWrite.MissingDirectory, is OutputWrite.Denied, is OutputWrite.Unwritable -> ExitCode.GENERAL_ERROR
    }
