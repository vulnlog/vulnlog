// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.vex.openvex.buildOpenVexDocument
import dev.vulnlog.lib.core.vex.openvex.releasesWithoutPurls
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexProducts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexSkippedEntries
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexStatementCounts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexWritten
import dev.vulnlog.lib.model.ReleaseEntry
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.parse.vex.openvex.OpenVexWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val DOCUMENT_ID_PREFIX = "https://vulnlog.dev/vex/"

@CacheableTask
abstract class VulnlogOpenVexTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val sink = diagnosticSink()
        val inputFile = singleVulnlogFileInput(name, files.files)
        val vulnlogFile = validateInputOrFail(inputFile).project.vulnlogProjectFile

        warnAboutSkippedReleases(vulnlogFile)
        renderOpenVexProducts(vulnlogFile)?.let(sink::verbose)
        val document =
            buildOpenVexDocument(
                vulnlogFile = vulnlogFile,
                id = DOCUMENT_ID_PREFIX + UUID.randomUUID(),
                timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS),
            )
        renderOpenVexSkippedEntries(vulnlogFile).forEach(sink::debug)
        if (document.statements.isEmpty()) failOnEmptyDocument(vulnlogFile)
        sink.verbose(renderOpenVexStatementCounts(document))

        val out = outputFile.get().asFile
        out.parentFile?.mkdirs()
        out.writeText(OpenVexWriter.write(document))
        sink.verbose(renderOpenVexWritten(out.path, document))
        logger.lifecycle(formatStatus(StatusVerb.WROTE, out.absolutePath))
    }

    /** Names the releases a statement was meant for that carry no purl, because they silently drop out. */
    private fun warnAboutSkippedReleases(vulnlogFile: VulnlogFile) {
        val skipped =
            vulnlogFile.vulnerabilities
                .flatMap { vulnEntry -> releasesWithoutPurls(vulnlogFile, vulnEntry) }
                .toSet()
        if (skipped.isEmpty()) return
        // Named in the order the file declares them, not the order the entries happen to mention them.
        val names =
            vulnlogFile.releases
                .map(ReleaseEntry::id)
                .filter { it in skipped }
                .joinToString(", ") { "'${it.value}'" }
        logger.warn(
            formatMessage(FindingSeverity.WARNING, "releases without purls are not part of the document: $names"),
        )
    }

    private fun failOnEmptyDocument(vulnlogFile: VulnlogFile): Nothing {
        val hint =
            if (vulnlogFile.releases.none { it.purls.isNotEmpty() }) {
                "Declare 'purls' on the releases you want the document to cover."
            } else {
                "No vulnerability entry references a release that declares purls."
            }
        throw GradleException("No statement applies. $hint")
    }
}
