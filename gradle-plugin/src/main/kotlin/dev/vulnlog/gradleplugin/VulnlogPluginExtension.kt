package dev.vulnlog.gradleplugin

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class VulnlogPluginExtension(objects: ObjectFactory) {
    /**
     * Specify the version of the Vulnlog DSL and CLI to use.
     * If not defined the Vulnlog Gradle plugins version is used.
     */
    val version: Property<String> = objects.property<String>().convention(readVersion())

    /**
     * Specify the Vulnlog definitions file.
     */
    val definitionsFile: RegularFileProperty = objects.fileProperty()

    /**
     * Specify the release branch to generate a report for.
     * If not specified, reports for all release branches are generated.
     */
    val releaseBranch: Property<String> = objects.property<String>()

    /**
     * Specify the Vulnlog report output directory location.
     */
    val reportOutput: DirectoryProperty = objects.directoryProperty()

    private fun readVersion() = javaClass.getResource("/version.txt")?.readText()?.lines()?.first().orEmpty()
}
