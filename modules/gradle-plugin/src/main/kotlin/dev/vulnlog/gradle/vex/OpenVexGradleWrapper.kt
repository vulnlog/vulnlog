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
import dev.vulnlog.lib.shell.resolveReleaseSelection
import dev.vulnlog.lib.shell.resolveTagsFilter
import org.gradle.api.GradleException

/**
 * Resolves the scope of an OpenVEX document, the Gradle counterpart of the CLI's resolveOpenVexScope.
 * [release] narrows it to one release; [tags] narrows the release purls.
 */
fun buildOpenVexScopeOrFail(
    vulnlogFile: VulnlogFile,
    release: Release?,
    tags: Set<Tag>,
    sink: DiagnosticSink = DiagnosticSink.NONE,
): OpenVexScope =
    try {
        val scope =
            OpenVexScope(
                releases = resolveReleaseSelection(release, vulnlogFile),
                tags = resolveTagsFilter(tags, vulnlogFile),
            )
        renderOpenVexScope(scope).forEach(sink::verbose)
        scope
    } catch (e: FilterValidationException) {
        throw GradleException("${e.message}. ${e.detail}")
    }
