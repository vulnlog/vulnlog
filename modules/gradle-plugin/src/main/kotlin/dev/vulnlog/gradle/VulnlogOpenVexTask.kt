// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.FilterRejected
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.app.OpenVexRequest
import dev.vulnlog.lib.app.generateOpenVex
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.vex.openvex.openVexDocumentId
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.render.renderOpenVexBaselineProblem
import dev.vulnlog.lib.render.renderOpenVexEmptyHint
import dev.vulnlog.lib.render.renderOpenVexProducts
import dev.vulnlog.lib.render.renderOpenVexScope
import dev.vulnlog.lib.render.renderOpenVexSkippedEntries
import dev.vulnlog.lib.render.renderOpenVexSkippedReleases
import dev.vulnlog.lib.render.renderOpenVexStatementCounts
import dev.vulnlog.lib.render.renderOpenVexWritten
import dev.vulnlog.lib.shell.DiagnosticSink
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.time.Instant
import java.util.UUID

/** The OpenVEX version this task reads and writes. The single place a task property would feed one day. */
private val FORMAT_VERSION = OpenVexFormatVersion.LATEST

@CacheableTask
abstract class VulnlogOpenVexTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    @get:Input
    @get:Optional
    abstract val release: Property<String>

    @get:Input
    abstract val tags: SetProperty<String>

    /**
     * The committed document this run continues. Declared with [InputFiles] rather than `@InputFile`, because the
     * file does not exist before the first `vulnlogOpenVexUpdate` creates it.
     */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baseline: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val sink = diagnosticSink()
        val inputFile = singleVulnlogFileInput(name, files.files)
        val vulnlogFile = validateInputOrFail(inputFile).project.vulnlogProjectFile
        val out = outputFile.get().asFile
        val request =
            OpenVexRequest(
                release = release.orNull,
                tags = tags.get(),
                baseline = readBaseline(out, sink),
                documentId = openVexDocumentId(UUID.randomUUID()),
                timestamp = Instant.now(),
                tooling = OpenVexTooling("Gradle plugin", BuildInfo.VERSION),
                formatVersion = FORMAT_VERSION,
            )

        when (val outcome = generateOpenVex(vulnlogFile, request)) {
            is FilterRejected -> throw GradleException(
                outcome.problems.joinToString(" ") { "${it.message}. ${it.hint}" },
            )

            is OpenVexOutcome.BaselineRejected ->
                failOnBaseline(renderOpenVexBaselineProblem(baseline.get().asFile.path, outcome.problem))

            is OpenVexOutcome.NoStatementApplies -> {
                logCollection(outcome.collection, sink)
                failOnEmptyDocument(vulnlogFile, outcome.collection.scope)
            }

            is OpenVexOutcome.Revised ->
                write(out, outcome.collection, outcome.document, outcome.content, StatusVerb.WROTE, sink)

            is OpenVexOutcome.Unchanged ->
                write(out, outcome.collection, outcome.document, outcome.content, StatusVerb.UNCHANGED, sink)
        }
    }

    /** Writes [content] to [out] and reports it with [verb]. The output always lives under the build directory. */
    private fun write(
        out: File,
        collection: OpenVexCollection,
        document: OpenVexDocument,
        content: String,
        verb: StatusVerb,
        sink: DiagnosticSink,
    ) {
        logCollection(collection, sink)
        sink.verbose(renderOpenVexStatementCounts(document))
        out.parentFile?.mkdirs()
        out.writeText(content)
        sink.verbose(renderOpenVexWritten(out.path, document))
        logger.lifecycle(formatStatus(verb, out.absolutePath))
    }

    /** The warning and the diagnostics that say what the collection holds and what it left out. */
    private fun logCollection(
        collection: OpenVexCollection,
        sink: DiagnosticSink,
    ) {
        renderOpenVexScope(collection.scope).forEach(sink::verbose)
        renderOpenVexSkippedReleases(collection)?.let { logger.warn(formatMessage(FindingSeverity.WARNING, it)) }
        renderOpenVexProducts(collection)?.let(sink::verbose)
        renderOpenVexSkippedEntries(collection).forEach(sink::debug)
    }

    /**
     * Reads the text of the configured baseline, or null when it is not configured or not created yet. Whether the
     * text can be continued is the run's to decide. A task that reads and writes one file is never up to date and a
     * build cache hit would overwrite the committed document, so the output stays under the build directory and
     * `vulnlogOpenVexUpdate` copies it over the baseline.
     */
    private fun readBaseline(
        out: File,
        sink: DiagnosticSink,
    ): String? {
        val file = baseline.orNull?.asFile ?: return null
        if (file.canonicalFile == out.canonicalFile) {
            throw GradleException(
                "baseline and outputFile are the same file (${file.path}). " +
                    "Keep outputFile under the build directory and run 'vulnlogOpenVexUpdate' " +
                    "to copy the document over the baseline.",
            )
        }
        if (!file.exists()) {
            sink.verbose("baseline '${file.path}' does not exist yet, issuing a new document")
            return null
        }
        return file.readText()
    }

    private fun failOnBaseline(message: String): Nothing =
        throw GradleException(message.replaceFirstChar(Char::uppercase) + ". Unset 'baseline' to issue a new document.")

    private fun failOnEmptyDocument(
        vulnlogFile: VulnlogFile,
        scope: OpenVexScope,
    ): Nothing {
        val hint = renderOpenVexEmptyHint(vulnlogFile, scope).replaceFirstChar(Char::uppercase)
        throw GradleException("No statement applies. $hint.")
    }
}
