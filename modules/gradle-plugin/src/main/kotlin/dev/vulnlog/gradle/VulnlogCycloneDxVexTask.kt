// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.CycloneDxVexOutcome
import dev.vulnlog.lib.app.CycloneDxVexRequest
import dev.vulnlog.lib.app.generateCycloneDxVex
import dev.vulnlog.lib.codec.cyclonedx.cycloneDxSerialNumber
import dev.vulnlog.lib.io.readVexBaseline
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.vex.VexBaselineRead
import dev.vulnlog.lib.model.vex.VexTooling
import dev.vulnlog.lib.model.vex.cyclonedx.CycloneDxVexFormatVersion
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.StatusVerb
import dev.vulnlog.lib.render.formatStatus
import dev.vulnlog.lib.render.renderCycloneDxVexMessages
import dev.vulnlog.lib.render.renderCycloneDxVexWritten
import dev.vulnlog.lib.render.renderVexNewDocument
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
import java.io.File
import java.time.Instant
import java.util.UUID

/** Becomes a `formatVersion` property once a second CycloneDX version is supported. */
private val FORMAT_VERSION = CycloneDxVexFormatVersion.LATEST

@CacheableTask
abstract class VulnlogCycloneDxVexTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val files: ConfigurableFileCollection

    /** Required: CycloneDX holds one analysis per vulnerability, so a document covers one release. */
    @get:Input
    abstract val release: Property<String>

    @get:Input
    abstract val tags: SetProperty<String>

    /** [InputFiles] rather than `@InputFile`: the baseline does not exist before the first `vulnlogCycloneDxVexUpdate`. */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baseline: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val inputFile = singleVulnlogFileInput(name, files.files)
        val project = validateInputOrFail(inputFile).project
        val out = outputFile.get().asFile
        val request =
            CycloneDxVexRequest(
                release = release.get(),
                tags = tags.get(),
                baseline = readBaseline(out),
                serialNumber = cycloneDxSerialNumber(UUID.randomUUID()),
                timestamp = Instant.now(),
                tooling = VexTooling("Gradle plugin", BuildInfo.VERSION),
                formatVersion = FORMAT_VERSION,
            )

        val outcome = generateCycloneDxVex(project, request)
        renderCycloneDxVexMessages(outcome).forEach(logger::log)
        when (outcome) {
            is CycloneDxVexOutcome.Failed -> throw failure(outcome, baseline.orNull?.asFile?.path ?: "")
            is CycloneDxVexOutcome.Generated -> write(out, outcome)
        }
    }

    /** Always writes: [out] lives under the build directory, and `vulnlogCycloneDxVexUpdate` owns the baseline. */
    private fun write(
        out: File,
        outcome: CycloneDxVexOutcome.Generated,
    ) {
        writeOrFail(writeOutput(out.toPath(), outcome.content, createDirectories = true))
        logger.log(renderCycloneDxVexWritten(out.path, outcome))
        val verb = if (outcome is CycloneDxVexOutcome.Unchanged) StatusVerb.UNCHANGED else StatusVerb.WROTE
        logger.log(Message.Status(formatStatus(verb, out.absolutePath)))
    }

    /**
     * The baseline must not be the output: a task reading and writing one file is never up to date, and a build cache
     * hit would overwrite the committed document.
     */
    private fun readBaseline(out: File): String? {
        val file = baseline.orNull?.asFile ?: return null
        if (file.canonicalFile == out.canonicalFile) {
            throw InvalidUserDataException(
                "baseline and outputFile are the same file (${file.path}). " +
                    "Keep outputFile under the build directory and run 'vulnlogCycloneDxVexUpdate' " +
                    "to copy the document over the baseline.",
            )
        }
        return when (val read = readVexBaseline(file.toPath())) {
            is VexBaselineRead.Present -> read.text

            VexBaselineRead.Absent -> {
                logger.log(renderVexNewDocument(file.path))
                null
            }

            is VexBaselineRead.Unreadable -> throw failure(read, file.path)
        }
    }
}
