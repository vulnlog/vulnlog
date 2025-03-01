package dev.vulnlog.gradleplugin

import de.undercouch.gradle.tasks.download.Download
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get

abstract class VulnlogPlugin : Plugin<Project> {
    override fun apply(project: Project): Unit =
        with(project) {
            val extension = extensions.create<VulnlogPluginExtension>("vulnlog")
            val version = extension.version

            version
                .map { dependencies.create("dev.vulnlog:dsl:$it") }
                .map { configurations["compileOnly"].dependencies.add(it) }

            val downloadUrl = version.map { "https://github.com/vulnlog/vulnlog/releases/download/v$it/vl-$it.zip" }
            val downloadDir = version.map { layout.buildDirectory.file("vulnlog/vl-$it.zip").get().asFile }

            val downloadTask =
                tasks.register("downloadVulnlog", Download::class.java) {
                    description = "Download the Vulnlog CLI application."
                    group = "Vulnlog"

                    src(downloadUrl)
                    dest(downloadDir)
                    overwrite(false)
                }

            val unzipTask =
                tasks.register("unzipVulnlog", Copy::class.java) {
                    dependsOn(downloadTask)

                    from(zipTree(downloadDir))
                    into(layout.buildDirectory.dir("vulnlog"))
                }

            tasks.register("showCliVersion", JavaExec::class.java) {
                description = "Print the Vulnlog CLI version."
                group = "Vulnlog"

                dependsOn(unzipTask)

                classpath = version.map { layout.buildDirectory.dir("vulnlog/vl-$it/lib").get().asFileTree }.get()
                mainClass.set("dev.vulnlog.cli.AppKt")
                args = listOf("--version")
            }
        }
}
