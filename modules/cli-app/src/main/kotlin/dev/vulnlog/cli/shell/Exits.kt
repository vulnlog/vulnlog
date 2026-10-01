// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell

import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome


/** Mirrors [Outcomes] for the CLI */
fun exitCode(failed: OpenVexOutcome.Failed): ExitCode =
    when (failed) {
        is FilterRejected -> ExitCode.INVALID_FLAG_VALUE
        is OpenVexOutcome.BaselineRejected -> ExitCode.INVALID_FLAG_VALUE
        is OpenVexOutcome.NoStatementApplies -> ExitCode.VALIDATION_ERROR
    }
