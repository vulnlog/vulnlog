// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.singleVulnlogFileInput
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.OpenVexOutcome
import dev.vulnlog.lib.app.OpenVexRequest
import dev.vulnlog.lib.app.generateOpenVex
import dev.vulnlog.lib.codec.openvex.openVexDocumentId
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.io.readOpenVexBaseline
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.model.vex.openvex.OpenVexBaselineRead
import dev.vulnlog.lib.model.vex.openvex.OpenVexFormatVersion
import dev.vulnlog.lib.model.vex.openvex.OpenVexTooling
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.renderOpenVexMessages
import dev.vulnlog.lib.render.renderOpenVexNewDocument
import dev.vulnlog.lib.render.renderOpenVexWritten
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

/** Becomes a `formatVersion` property once a second OpenVEX version is supported. */
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

    /** [InputFiles] rather than `@InputFile`: the baseline does not exist before the first `vulnlogOpenVexUpdate`. */
    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baseline: RegularFileProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val inputFile = singleVulnlogFileInput(name, files.files)
        val vulnlogFile = validateInputOrFail(inputFile).project.vulnlogProjectFile
        val out = outputFile.get().asFile
        val request =
            OpenVexRequest(
                release = release.orNull,
                tags = tags.get(),
                baseline = readBaseline(out),
                documentId = openVexDocumentId(UUID.randomUUID()),
                timestamp = Instant.now(),
                tooling = OpenVexTooling("Gradle plugin", BuildInfo.VERSION),
                formatVersion = FORMAT_VERSION,
            )

        val outcome = generateOpenVex(vulnlogFile, request)
        renderOpenVexMessages(outcome).forEach(logger::log)
        when (outcome) {
            is OpenVexOutcome.Failed -> throw failure(outcome, baseline.orNull?.asFile?.path ?: "")
            is OpenVexOutcome.Generated -> write(out, outcome)
        }
    }

    /** Always writes: [out] lives under the build directory, and `vulnlogOpenVexUpdate` owns the baseline. */
    private fun write(
        out: File,
        outcome: OpenVexOutcome.Generated,
    ) {
        writeOrFail(writeOutput(out.toPath(), outcome.content, createDirectories = true))
        logger.log(renderOpenVexWritten(out.path, outcome))
        val verb = if (outcome is OpenVexOutcome.Unchanged) StatusVerb.UNCHANGED else StatusVerb.WROTE
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
                    "Keep outputFile under the build directory and run 'vulnlogOpenVexUpdate' " +
                    "to copy the document over the baseline.",
            )
        }
        return when (val read = readOpenVexBaseline(file.toPath())) {
            is OpenVexBaselineRead.Present -> read.text

            OpenVexBaselineRead.Absent -> {
                logger.log(renderOpenVexNewDocument(file.path))
                null
            }

            is OpenVexBaselineRead.Unreadable -> throw failure(read, file.path)
        }
    }
}
