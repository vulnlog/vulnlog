// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.cli.shell.vex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import dev.vulnlog.cli.shell.ExitCode
import dev.vulnlog.cli.shell.diagnosticSink
import dev.vulnlog.cli.shell.echoMessage
import dev.vulnlog.lib.core.formatHint
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexScope
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.resolveReleaseFilter
import dev.vulnlog.lib.shell.resolveTagsFilter

/**
 * Resolves the scope of an OpenVEX document. [asOf] expands to the release window up to and including that release,
 * the same as everywhere else in the CLI; [tags] narrows the release purls that become products.
 */
fun CliktCommand.resolveOpenVexScope(
    asOf: String?,
    tags: Set<String>,
    vulnlogFile: VulnlogFile,
): OpenVexScope =
    try {
        val scope =
            OpenVexScope(
                releases = resolveReleaseFilter(asOf?.let(::Release), vulnlogFile),
                tags = resolveTagsFilter(tags.map(::Tag).toSet(), vulnlogFile),
            )
        renderOpenVexScope(scope).forEach { diagnosticSink().verbose(it) }
        scope
    } catch (e: FilterValidationException) {
        echoMessage(formatMessage(FindingSeverity.ERROR, e.message.orEmpty()))
        echoMessage(formatHint(e.detail))
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
    } catch (e: IllegalArgumentException) {
        echoMessage(formatMessage(FindingSeverity.ERROR, "Invalid scope value: ${e.message}"))
        throw ProgramResult(ExitCode.INVALID_FLAG_VALUE.code)
    }
