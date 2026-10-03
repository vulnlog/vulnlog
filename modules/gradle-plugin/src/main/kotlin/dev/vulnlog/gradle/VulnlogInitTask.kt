// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.failure
import dev.vulnlog.gradle.internal.log
import dev.vulnlog.gradle.internal.writeOrFail
import dev.vulnlog.lib.app.InitOutcome
import dev.vulnlog.lib.app.InitRequest
import dev.vulnlog.lib.app.initDocument
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.io.writeOutput
import dev.vulnlog.lib.render.Message
import dev.vulnlog.lib.render.renderWritten
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

@CacheableTask
abstract class VulnlogInitTask : DefaultTask() {
    @get:Input
    abstract val organization: Property<String>

    @get:Input
    abstract val projectName: Property<String>

    @get:Input
    abstract val author: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @get:Input
    @get:Optional
    @get:Option(option = "force", description = "Overwrite output file if it already exists.")
    abstract val force: Property<Boolean>

    @TaskAction
    fun generate() {
        val file = outputFile.get().asFile
        val request =
            InitRequest(
                organization = organization.get(),
                name = projectName.get(),
                author = author.get(),
                targetExists = file.exists(),
                force = force.getOrElse(false),
            )

        when (val outcome = initDocument(request)) {
            is InitOutcome.Failed -> throw failure(outcome, file.path)

            is InitOutcome.Created -> {
                writeOrFail(writeOutput(file.toPath(), outcome.content))
                logger.log(renderWritten(file.path))
                logger.log(Message.Status(formatStatus(StatusVerb.CREATED, file.absolutePath)))
            }
        }
    }
}
