// Copyright 2024 the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0
package dev.vulnlog.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class VulnlogPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create("vulnlog", VulnlogExtension::class.java)

        project.tasks.register("vulnlogInit", VulnlogInitTask::class.java) { task ->
            task.description = "Initiate a new Vulnlog YAML file."
            task.group = "vulnlog"
            task.organization.convention(project.propertyOrNull("vulnlog.organization"))
            task.projectName.convention(project.propertyOrNull("vulnlog.name"))
            task.author.convention(project.propertyOrNull("vulnlog.author"))
            task.outputFile.convention(
                project.propertyOrNull("vulnlog.output")?.let { project.layout.projectDirectory.file(it) },
            )
        }

        project.tasks.register("vulnlogValidate", VulnlogValidateTask::class.java) { task ->
            task.description = "Validate Vulnlog YAML files."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.strict.convention(extension.validate.strict)
        }

        project.tasks.register("vulnlogSuppress", VulnlogSuppressTask::class.java) { task ->
            task.description = "Generate suppression files."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.reporter.convention(extension.suppress.reporter)
            task.release.convention(extension.suppress.release)
            task.tags.convention(extension.suppress.tags)
            task.outputDir.convention(
                extension.suppress.outputDir.orElse(project.layout.buildDirectory.dir("vulnlog/suppressions")),
            )
        }

        project.tasks.register("vulnlogReport", VulnlogReportTask::class.java) { task ->
            task.description = "Generate an HTML vulnerability report."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.reporter.convention(extension.report.reporter)
            task.release.convention(extension.report.release)
            task.tags.convention(extension.report.tags)
            task.outputFile.convention(
                extension.report.outputFile.orElse(project.layout.buildDirectory.file("vulnlog/vulnlog-report.html")),
            )
        }
    }
}

private fun Project.propertyOrNull(name: String): String? = providers.gradleProperty(name).orNull
