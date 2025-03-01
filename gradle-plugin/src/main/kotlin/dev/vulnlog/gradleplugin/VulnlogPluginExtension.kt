package dev.vulnlog.gradleplugin

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.property

open class VulnlogPluginExtension(objects: ObjectFactory) {
    /**
     * Specify the version of the Vulnlog DSL and CLI to use.
     * If not defined the Vulnlog Gradle plugins version is used.
     */
    val version: Property<String> = objects.property<String>().convention(readVersion())

    private fun readVersion() = javaClass.getResource("/version.txt")?.readText()?.lines()?.first().orEmpty()
}
