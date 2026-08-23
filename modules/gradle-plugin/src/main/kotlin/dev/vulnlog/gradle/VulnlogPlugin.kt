// Copyright the Vulnlog contributors
// SPDX-License-Identifier: Apache-2.0

package dev.vulnlog.gradle

import dev.vulnlog.lib.shell.ChangelogFormatRequest
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFile

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
            task.dispositions.convention(extension.report.impact.dispositions)
            task.outputFile.convention(
                extension.report.impact.outputFile.orElse(
                    project.layout.buildDirectory.file("vulnlog/vulnlog-impact-report.html"),
                ),
            )
        }

        val changelog = extension.report.changelog
        project.tasks.register("vulnlogChangelogReport", VulnlogChangelogReportTask::class.java) { task ->
            task.description = "Report which vulnerabilities each release fixed."
            task.group = "vulnlog"
            task.files.from(extension.files)
            task.format.convention(changelog.format.orElse("text"))
            task.brief.convention(changelog.brief.orElse(false))
            task.fixedIn.convention(changelog.fixedIn)
            task.reporter.convention(changelog.reporter)
            task.asOf.convention(changelog.asOf)
            task.tags.convention(changelog.tags)
            task.outputFile.convention(
                changelog.outputFile.orElse(task.format.map { token -> project.changelogReportFile(token) }),
            )
        }
    }
}

/** Default report location, named after the format so the extension always matches the content. */
private fun Project.changelogReportFile(formatToken: String): RegularFile =
    layout.buildDirectory
        .file("vulnlog/vulnlog-changelog.${ChangelogFormatRequest.fromToken(formatToken).fileExtension}")
        .get()

private fun Project.propertyOrNull(name: String): String? = providers.gradleProperty(name).orNull
