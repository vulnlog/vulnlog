// Copyright the Vulnlog contributors
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

        project.tasks.register("vulnlogFormat", VulnlogFmtTask::class.java) { task ->
            task.description = "Format Vulnlog YAML files to the canonical style."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.check.convention(extension.fmt.check)
        }

        project.tasks.register("vulnlogSuppress", VulnlogSuppressTask::class.java) { task ->
            task.description = "Generate suppression files."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.reporter.convention(extension.suppress.reporter)
            task.asOf.convention(extension.suppress.asOf)
            task.tags.convention(extension.suppress.tags)
            task.outputDir.convention(
                extension.suppress.outputDir.orElse(project.layout.buildDirectory.dir("vulnlog/suppressions")),
            )
            task.format.convention(extension.suppress.format)
        }

        project.tasks.register("vulnlogImpactReport", VulnlogImpactReportTask::class.java) { task ->
            task.description = "Generate an HTML impact report."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.reporter.convention(extension.report.impact.reporter)
            task.asOf.convention(extension.report.impact.asOf)
            task.tags.convention(extension.report.impact.tags)
            task.states.convention(extension.report.impact.states)
            task.verdicts.convention(extension.report.impact.verdicts)
            task.outputFile.convention(
                extension.report.impact.outputFile.orElse(
                    project.layout.buildDirectory.file("vulnlog/vulnlog-impact-report.html"),
                ),
            )
        }
    }
}

private fun Project.propertyOrNull(name: String): String? = providers.gradleProperty(name).orNull
