// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle.vex

import dev.vulnlog.lib.core.vex.openvex.renderOpenVexScope
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.shell.DiagnosticSink
import dev.vulnlog.lib.shell.FilterValidationException
import dev.vulnlog.lib.shell.resolveReleaseFilter
import dev.vulnlog.lib.shell.resolveTagsFilter
import org.gradle.api.GradleException

/**
 * Resolves the scope of an OpenVEX document, the Gradle counterpart of the CLI's resolveOpenVexScope.
 * [asOf] expands to the release window up to and including that release; [tags] narrows the release purls.
 */
fun buildOpenVexScopeOrFail(
    vulnlogFile: VulnlogFile,
    asOf: Release?,
    tags: Set<Tag>,
    sink: DiagnosticSink = DiagnosticSink.NONE,
): OpenVexScope =
    try {
        val scope =
            OpenVexScope(
                releases = resolveReleaseFilter(asOf, vulnlogFile),
                tags = resolveTagsFilter(tags, vulnlogFile),
            )
        renderOpenVexScope(scope).forEach(sink::verbose)
        scope
    } catch (e: FilterValidationException) {
        throw GradleException("${e.message}. ${e.detail}")
    }
