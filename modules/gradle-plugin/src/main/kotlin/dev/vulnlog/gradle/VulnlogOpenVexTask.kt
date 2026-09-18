// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.gradle.vex.buildOpenVexScopeOrFail
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatMessage
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.vex.openvex.generateOpenVex
import dev.vulnlog.lib.core.vex.openvex.openVexTooling
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexEmptyHint
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexProducts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexSkippedEntries
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexSkippedReleases
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexStatementCounts
import dev.vulnlog.lib.core.vex.openvex.renderOpenVexWritten
import dev.vulnlog.lib.model.Release
import dev.vulnlog.lib.model.Tag
import dev.vulnlog.lib.model.VulnlogFile
import dev.vulnlog.lib.model.finding.FindingSeverity
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaseline
import dev.vulnlog.lib.model.vex.openvex.OpenVexCollection
import dev.vulnlog.lib.model.vex.openvex.OpenVexOutcome
import dev.vulnlog.lib.model.vex.openvex.OpenVexScope
import dev.vulnlog.lib.parse.vex.openvex.OpenVexReader
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
        val scope =
            buildOpenVexScopeOrFail(vulnlogFile, release.orNull?.let(::Release), tags.get().map(::Tag).toSet(), sink)
        val out = outputFile.get().asFile
        val baselineDocument = readBaseline(out, sink)

        val tooling = openVexTooling("Gradle plugin", BuildInfo.VERSION)
        val outcome = generateOpenVex(vulnlogFile, scope, baselineDocument, Instant.now(), tooling)
        logCollection(outcome.collection, sink)
        val generated =
            when (outcome) {
                is OpenVexOutcome.Empty -> failOnEmptyDocument(vulnlogFile, scope)
                is OpenVexOutcome.Generated -> outcome
            }
        sink.verbose(renderOpenVexStatementCounts(generated.document))

        out.parentFile?.mkdirs()
        out.writeText(generated.content)
        sink.verbose(renderOpenVexWritten(out.path, generated.document))
        val verb = if (generated.unchanged) StatusVerb.UNCHANGED else StatusVerb.WROTE
        logger.lifecycle(formatStatus(verb, out.absolutePath))
    }

    /** The warning and the diagnostics that say what the collection holds and what it left out. */
    private fun logCollection(
        collection: OpenVexCollection,
        sink: DiagnosticSink,
    ) {
        renderOpenVexSkippedReleases(collection)?.let { logger.warn(formatMessage(FindingSeverity.WARNING, it)) }
        renderOpenVexProducts(collection)?.let(sink::verbose)
        renderOpenVexSkippedEntries(collection).forEach(sink::debug)
    }

    /**
     * Reads the configured baseline, or null when it is not configured or not created yet. A task that reads and
     * writes one file is never up to date and a build cache hit would overwrite the committed document, so the
     * output stays under the build directory and `vulnlogOpenVexUpdate` copies it over the baseline.
     */
    private fun readBaseline(
        out: File,
        sink: DiagnosticSink,
    ): OpenVexBaseline? {
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
        return OpenVexReader.readBaseline(file.readText()) ?: run {
            logger.warn(
                formatMessage(
                    FindingSeverity.WARNING,
                    "baseline '${file.path}' is not an OpenVEX document, issuing a new one",
                ),
            )
            null
        }
    }

    private fun failOnEmptyDocument(
        vulnlogFile: VulnlogFile,
        scope: OpenVexScope,
    ): Nothing {
        val hint = renderOpenVexEmptyHint(vulnlogFile, scope).replaceFirstChar(Char::uppercase)
        throw GradleException("No statement applies. $hint.")
    }
}
