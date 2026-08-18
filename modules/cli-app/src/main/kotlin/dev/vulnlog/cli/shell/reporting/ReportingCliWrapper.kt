// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.reporting

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.echoMessage
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.reporting.validateSharedProject
import dev.vulnlog.lib.model.Project
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity

/** The project [files] share. Fails with [ExitCode.VALIDATION_ERROR] when their coordinates differ. */
fun CliktCommand.sharedProjectOrFail(files: List<VulnlogFile>): Project =
    validateSharedProject(files) ?: run {
        echoMessage(formatMessage(FindingSeverity.ERROR, "all input files must share the same project metadata"))
        throw ProgramResult(ExitCode.VALIDATION_ERROR.code)
    }
