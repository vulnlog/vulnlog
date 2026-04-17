// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import dev.vulnlog.lib.core.init
import dev.vulnlog.lib.model.SchemaVersion
import dev.vulnlog.lib.parse.YamlWriter
import dev.vulnlog.lib.parse.createYamlMapper
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

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

    @TaskAction
    fun generate() {
        val vulnlogFile = init(SchemaVersion(1, 0), organization.get(), projectName.get(), author.get())
        val content = YamlWriter.write(vulnlogFile, createYamlMapper())
        val file = outputFile.get().asFile
        file.writeText(content)
        logger.lifecycle("Vulnlog file created at: ${file.absolutePath}")
    }
}
