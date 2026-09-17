// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.gradle.internal.diagnosticSink
import dev.vulnlog.lib.core.StatusVerb
import dev.vulnlog.lib.core.formatStatus
import dev.vulnlog.lib.core.init
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.parse.YamlWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
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
        if (!force.getOrElse(false) && file.exists()) {
            throw GradleException("The file ${file.path} already exists. Pass --force to replace it.")
        }
        val vulnlogFile = init(SchemaVersion.V1, organization.get(), projectName.get(), author.get())
        val content = YamlWriter.write(vulnlogFile)
        file.writeText(content)
        diagnosticSink().verbose("wrote ${file.path}")
        logger.lifecycle(formatStatus(StatusVerb.CREATED, file.absolutePath))
    }
}
