// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.vulnlogFileInputs
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.gradle.validation.validateInputOrFail
import dev.vulnlog.lib.app.CopiedFile
import dev.vulnlog.lib.app.CopyOutcome
import dev.vulnlog.lib.app.CopyRequest
import dev.vulnlog.lib.app.copyVulnerabilities
import dev.vulnlog.lib.core.parseVulnId
import dev.vulnlog.lib.io.FileInputOption
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.renderCopied
import dev.vulnlog.lib.render.renderCopyMessages
import dev.vulnlog.lib.render.renderWritten
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Rewrites Vulnlog files in place")
abstract class VulnlogCopyTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val destinationFiles: ConfigurableFileCollection

    @get:Input
    abstract val vulnIds: SetProperty<String>

    @TaskAction
    fun generate() {
        val request = CopyRequest(vulnIds.get().map { parseVulnId(it) }.toSet())
        val sourceProject = validateInputOrFail(FileInputOption.File(sourceFile.get().asFile.toPath())).project
        val destinationProjects =
            vulnlogFileInputs(destinationFiles.files).map { input -> validateInputOrFail(input).project }

        when (val outcome = copyVulnerabilities(sourceProject, destinationProjects, request)) {
            is CopyOutcome.Failed -> throw failure(outcome)
            is CopyOutcome.Copied -> outcome.files.forEach(::write)
        }
    }

    private fun write(file: CopiedFile) {
        renderCopyMessages(file).forEach(logger::log)
        val path = requireNotNull(file.document.path) { "Gradle inputs are always files" }
        writeOrFail(writeOutput(path, file.content))
        logger.log(renderWritten(file.document.source))
        renderCopied(file).forEach(logger::log)
    }
}
