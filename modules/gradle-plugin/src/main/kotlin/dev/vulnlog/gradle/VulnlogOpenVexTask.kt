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
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.vex.openvex.OpenVexDocument
import dev.vulnlog.lib.model.vex.openvex.OpenVexEmptyReason
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.render.OpenVexLine
import dev.vulnlog.lib.render.renderOpenVexBaselineProblem
import dev.vulnlog.lib.render.renderOpenVexEmptyHint
import dev.vulnlog.lib.render.renderOpenVexReport
import dev.vulnlog.lib.render.renderOpenVexWritten
import dev.vulnlog.lib.shell.DiagnosticSink
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
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
import org.gradle.api.tasks.VerificationException
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

        val outcome = generateOpenVex(vulnlogFile, request)
        renderOpenVexReport(outcome).forEach { line -> log(line, sink) }
        when (outcome) {
            is FilterRejected ->
                throw InvalidUserDataException(outcome.problems.joinToString(" ") { "${it.message}. ${it.hint}" })

            is OpenVexOutcome.BaselineRejected ->
                failOnBaseline(renderOpenVexBaselineProblem(baseline.get().asFile.path, outcome.problem))

            is OpenVexOutcome.NoStatementApplies -> failOnEmptyDocument(outcome.reason)
            is OpenVexOutcome.Revised -> write(out, outcome.document, outcome.content, StatusVerb.WROTE, sink)
            is OpenVexOutcome.Unchanged -> write(out, outcome.document, outcome.content, StatusVerb.UNCHANGED, sink)
        }
    }

    private fun log(
        line: OpenVexLine,
        sink: DiagnosticSink,
    ) = when (line) {
        is OpenVexLine.Warning -> logger.warn(formatMessage(FindingSeverity.WARNING, line.text))
        is OpenVexLine.Verbose -> sink.verbose(line.text)
        is OpenVexLine.Debug -> sink.debug(line.text)
    }

    /** Writes [content] to [out] and reports it with [verb]. The output always lives under the build directory. */
    private fun write(
        out: File,
        document: OpenVexDocument,
        content: String,
        verb: StatusVerb,
        sink: DiagnosticSink,
    ) {
        out.parentFile?.mkdirs()
        out.writeText(content)
        sink.verbose(renderOpenVexWritten(out.path, document))
        logger.lifecycle(formatStatus(verb, out.absolutePath))
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
            throw InvalidUserDataException(
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

    /** A baseline that cannot be continued is task configuration to fix. */
    private fun failOnBaseline(message: String): Nothing =
        throw InvalidUserDataException(
            message.replaceFirstChar(Char::uppercase) + ". Unset 'baseline' to issue a new document.",
        )

    /** An empty document is a verdict on the Vulnlog file, so it works with `--continue`. */
    private fun failOnEmptyDocument(reason: OpenVexEmptyReason): Nothing {
        val hint = renderOpenVexEmptyHint(reason).replaceFirstChar(Char::uppercase)
        throw VerificationException("No statement applies. $hint.")
    }
}
