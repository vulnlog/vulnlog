package dev.vulnlog.gradleplugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

interface VulnlogPluginExtension {
    /**
     * Specify the Vulnlog CLI version to use.
     * If not defined the Vulnlog Gradle plugins version is used.
     */
    val version: Property<String>
}

abstract class VulnlogPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension = project.extensions.create<VulnlogPluginExtension>("vulnlog")

        val dslVersion = javaClass.getResource("/version.txt")?.readText()?.lines()?.first() ?: ""
        val dslDependency = project.dependencies.create("dev.vulnlog:dsl:$dslVersion")
        project.configurations["compileOnly"].dependencies.add(dslDependency)

        val cliVersion: Provider<String> = extension.version.orElse(dslVersion)

        val downloadTask =
            project.tasks.register("downloadCli", DownloadTask::class.java) {
                description = "Download the Vulnlog CLI application."
                group = "Vulnlog"

                version.set(cliVersion)
            }

        val unpackTask =
            project.tasks.register("unpackFiles", Copy::class.java) {
                dependsOn(downloadTask)

                from(project.zipTree(project.layout.buildDirectory.file("vl.zip").get().asFile))
                into(project.layout.buildDirectory.dir("vl"))
            }

        project.tasks.register("showCliVersion", JavaExec::class.java) {
            description = "Print the Vulnlog CLI version."
            group = "Vulnlog"

            dependsOn(unpackTask)

            classpath = project.layout.buildDirectory.dir("vl/vl-${cliVersion.get()}/lib").get().asFileTree
            mainClass.set("dev.vulnlog.cli.AppKt")
            args = listOf("--version")
        }
    }
}
